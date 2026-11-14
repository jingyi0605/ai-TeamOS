package com.outcomego.mobile.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

data class ProviderConfiguration(
    val serverUrl: String,
    val machineId: String,
    val credential: String,
    val connectionId: String,
    val deploymentId: String,
)

enum class ProviderState {
    STOPPED,
    ENROLLING,
    CONNECTING,
    READY,
    RECONNECTING,
    UNAVAILABLE,
    REVOKED,
    FAILED,
}

data class ProviderSnapshot(
    val state: ProviderState = ProviderState.STOPPED,
    val machineId: String? = null,
    val deploymentId: String? = null,
    val routeEpoch: String? = null,
    val activeStreams: Int = 0,
    val lastErrorCode: String? = null,
)

class ProviderSession(
    private val configuration: ProviderConfiguration,
    private val webSocket: ProviderWebSocketConnector,
    private val tcpDialer: ProviderTcpDialer,
    private val clock: ProviderClock,
    private val scope: CoroutineScope,
    private val onChanged: (ProviderSnapshot) -> Unit = {},
) {
    private val mutableSnapshot = MutableStateFlow(
        ProviderSnapshot(
            state = ProviderState.STOPPED,
            machineId = configuration.machineId,
            deploymentId = configuration.deploymentId,
        ),
    )
    val snapshot: StateFlow<ProviderSnapshot> = mutableSnapshot.asStateFlow()
    private var job: Job? = null
    private var mux: ProviderMux? = null
    private var socket: ProviderWebSocket? = null
    private var route: NetworkRoute? = null
    private var expiresAt = 0L
    private var lastRenewal = 0L
    private var reconnectAttempt = 0
    private var stopped = true
    private val pendingLeases = linkedMapOf<String, Long>()

    fun start() {
        if (job?.isActive == true) return
        stopped = false
        job = scope.launch {
            runLoop()
        }
    }

    suspend fun stop() {
        stopped = true
        job?.cancel()
        job = null
        closeTransport()
        publish(ProviderState.STOPPED)
    }

    suspend fun revoke() {
        stopped = true
        job?.cancel()
        job = null
        closeTransport()
        publish(ProviderState.REVOKED, "machine_unauthorized")
    }

    private suspend fun runLoop() {
        while (scope.isActive && !stopped) {
            publish(if (reconnectAttempt == 0) ProviderState.CONNECTING else ProviderState.RECONNECTING)
            try {
                connectOnce()
                reconnectAttempt = 0
            } catch (error: Throwable) {
                if (!scope.isActive || stopped) break
                val code = when (error) {
                    is NetworkProtocolException -> "network_protocol_invalid"
                    is MobileEnrollmentException -> if (error.status == 401 || error.status == 403) "machine_unauthorized" else "enrollment_failed"
                    is NetworkTargetException -> error.code
                    else -> "network_unavailable"
                }
                if (code == "machine_unauthorized") {
                    publish(ProviderState.REVOKED, code)
                    break
                }
                publish(ProviderState.UNAVAILABLE, code)
                closeTransport()
                val delayMs = minOf(MAX_RECONNECT_MS, 500L * (1L shl reconnectAttempt.coerceAtMost(3)))
                reconnectAttempt++
                delay(delayMs)
            }
        }
    }

    private suspend fun connectOnce() {
        val url = webSocketUrl(configuration.serverUrl)
        val headers = mapOf(
            "Authorization" to "Bearer ${configuration.credential}",
            "x-outcomego-machine-id" to configuration.machineId,
            "x-outcomego-connection-id" to configuration.connectionId,
        )
        val ws = webSocket.connect(url, headers, NETWORK_PROTOCOL)
        socket = ws
        val wire = object : ProviderWire {
            override suspend fun send(bytes: ByteArray) = ws.send(bytes)
            override suspend fun close() = ws.close(1000, "Provider stopped")
        }
        val currentMux = ProviderMux(
            wire = wire,
            scope = scope,
            dialer = tcpDialer,
            onControl = ::handleControl,
            targetAllowed = { target ->
                target.epoch == route?.epoch && mutableSnapshot.value.state == ProviderState.READY
            },
            onChanged = { publish(mutableSnapshot.value.state) },
        )
        mux = currentMux
        challenge()
        coroutineScope {
            val receiver = launch {
                ws.incoming.collect { currentMux.receive(it) }
            }
            val timer = launch {
                while (isActive) {
                    delay(250)
                    val now = clock.nowMs()
                    if (expiresAt != 0L && now >= expiresAt) {
                        currentMux.reset()
                        route = null
                        publish(ProviderState.UNAVAILABLE, "lease_expired")
                    }
                    if (now - lastRenewal >= RENEW_MS) {
                        lastRenewal = now
                        challenge()
                    }
                }
            }
            receiver.join()
            timer.cancel()
        }
        throw IllegalStateException("Provider WebSocket closed")
    }

    private suspend fun challenge() {
        val current = mux ?: return
        val nonce = randomNonce()
        pendingLeases[nonce] = clock.nowMs()
        while (pendingLeases.size > 4) {
            pendingLeases.remove(pendingLeases.keys.first())
        }
        current.sendControl(
            buildJsonObject {
                put("op", "lease")
                put("nonce", nonce)
                put("deploymentId", configuration.deploymentId)
                put("maxWindow", MAX_WINDOW)
            },
        )
    }

    private suspend fun handleControl(message: JsonObject) {
        when (message.stringField("op")) {
            "invalidate" -> {
                mux?.reset()
                route = null
                expiresAt = 0
                publish(ProviderState.CONNECTING)
                challenge()
            }
            "lease" -> {
                val nonce = message.stringField("nonce")
                val startedAt = pendingLeases.remove(nonce)
                if (startedAt == null || clock.nowMs() >= startedAt + LEASE_MS) return
                val routeValue = message["route"]?.jsonObject?.toNetworkRoute()
                    ?: throw NetworkProtocolException("Missing lease route")
                if (routeValue.deploymentId != configuration.deploymentId) {
                    throw NetworkProtocolException("Deployment identity changed")
                }
                if (route != null && route?.epoch != routeValue.epoch) mux?.reset()
                route = routeValue
                routeValue.streamWindow?.let { mux?.configureWindow(it) }
                val now = clock.nowMs()
                expiresAt = now + LEASE_MS
                lastRenewal = now
                mux?.sendControl(
                    buildJsonObject {
                        put("op", "applied")
                        put("epoch", routeValue.epoch)
                        put("revision", routeValue.revision)
                    },
                )
                publish(if (routeValue.available) ProviderState.READY else ProviderState.UNAVAILABLE)
            }
            else -> throw NetworkProtocolException("Unsupported network control")
        }
    }

    private suspend fun closeTransport() {
        mux?.close()
        mux = null
        socket?.close(1000, "Provider stopped")
        socket = null
        route = null
        expiresAt = 0
        publish(ProviderState.STOPPED)
    }

    private fun publish(state: ProviderState, error: String? = null) {
        val next = ProviderSnapshot(
            state = state,
            machineId = configuration.machineId,
            deploymentId = configuration.deploymentId,
            routeEpoch = route?.epoch,
            activeStreams = mux?.activeConnections() ?: 0,
            lastErrorCode = error,
        )
        mutableSnapshot.value = next
        onChanged(next)
    }
}

private fun webSocketUrl(serverUrl: String): String {
    val trimmed = serverUrl.trimEnd('/')
    return when {
        trimmed.startsWith("https://", ignoreCase = true) -> "wss://" + trimmed.removePrefixIgnoreCase("https://") + "/api/v0/machines/channel/browser-network"
        trimmed.startsWith("http://", ignoreCase = true) -> "ws://" + trimmed.removePrefixIgnoreCase("http://") + "/api/v0/machines/channel/browser-network"
        else -> throw IllegalArgumentException("Invalid provider server URL")
    }
}

private fun String.removePrefixIgnoreCase(prefix: String): String =
    if (startsWith(prefix, ignoreCase = true)) substring(prefix.length) else this

private fun randomNonce(): String =
    (1..43).map { "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_".random() }.joinToString("")
