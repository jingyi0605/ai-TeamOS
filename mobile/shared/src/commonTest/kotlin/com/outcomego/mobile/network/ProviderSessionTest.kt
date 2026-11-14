package com.outcomego.mobile.network

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProviderSessionTest {
    @Test
    fun appliesOnlyTheCurrentLeaseChallenge() = runTest {
        val socket = FakeProviderWebSocket()
        val clock = FakeProviderClock()
        val session = ProviderSession(
            configuration = configuration(),
            webSocket = object : ProviderWebSocketConnector {
                override suspend fun connect(
                    url: String,
                    headers: Map<String, String>,
                    protocol: String,
                ): ProviderWebSocket = socket
            },
            tcpDialer = immediateDialer(),
            clock = clock,
            scope = this,
        )

        session.start()
        runCurrent()
        val firstLease = sentControls(socket).single()
        val firstNonce = firstLease.stringField("nonce")
        assertEquals(43, firstNonce.length)

        socket.incomingChannel.send(
            encodeFrame(
                FrameType.CONTROL,
                0u,
                encodeJson(
                    buildJsonObject {
                        put("op", "lease")
                        put("nonce", "stale")
                        put("sequence", 1)
                        put("route", route("old"))
                    },
                ),
            ),
        )
        runCurrent()
        assertEquals(ProviderState.CONNECTING, session.snapshot.value.state)

        socket.incomingChannel.send(
            encodeFrame(
                FrameType.CONTROL,
                0u,
                encodeJson(
                    buildJsonObject {
                        put("op", "lease")
                        put("nonce", firstNonce)
                        put("sequence", 2)
                        put("route", route("current"))
                    },
                ),
            ),
        )
        runCurrent()
        assertEquals(ProviderState.READY, session.snapshot.value.state)
        assertEquals("current", session.snapshot.value.routeEpoch)
        session.stop()
    }

    @Test
    fun leaseExpiryMakesProviderUnavailableUntilAValidRenewal() = runTest {
        val socket = FakeProviderWebSocket()
        val clock = FakeProviderClock()
        val session = ProviderSession(
            configuration = configuration(),
            webSocket = object : ProviderWebSocketConnector {
                override suspend fun connect(
                    url: String,
                    headers: Map<String, String>,
                    protocol: String,
                ): ProviderWebSocket = socket
            },
            tcpDialer = immediateDialer(),
            clock = clock,
            scope = this,
        )

        session.start()
        runCurrent()
        val nonce = sentControls(socket).single().stringField("nonce")
        socket.incomingChannel.send(leaseFrame(1, nonce, "epoch-1"))
        runCurrent()
        assertEquals(ProviderState.READY, session.snapshot.value.state)

        clock.now = LEASE_MS + 1
        testScheduler.advanceTimeBy(250)
        runCurrent()
        assertEquals(ProviderState.UNAVAILABLE, session.snapshot.value.state)
        session.stop()
    }

    private fun configuration() = ProviderConfiguration(
        serverUrl = "https://server.example",
        machineId = "11111111-1111-1111-1111-111111111111",
        credential = "credential",
        connectionId = "connection",
        deploymentId = "22222222-2222-2222-2222-222222222222",
    )

    private fun route(epoch: String) = buildJsonObject {
        put("deploymentId", "22222222-2222-2222-2222-222222222222")
        put("mode", "device")
        put("machineId", "11111111-1111-1111-1111-111111111111")
        put("revision", 1)
        put("epoch", epoch)
        put("available", true)
        put("streamWindow", WINDOW)
    }

    private fun leaseFrame(sequence: Int, nonce: String, epoch: String): ByteArray =
        encodeFrame(
            FrameType.CONTROL,
            0u,
            encodeJson(
                buildJsonObject {
                    put("op", "lease")
                    put("nonce", nonce)
                    put("sequence", sequence)
                    put("route", route(epoch))
                },
            ),
        )

    private fun sentControls(socket: FakeProviderWebSocket): List<JsonObject> =
        socket.sent.map(::decodeFrame)
            .filter { it.type == FrameType.CONTROL }
            .map { decodeJsonObject(it.payload) }

    private fun immediateDialer() = object : ProviderTcpDialer {
        override suspend fun connect(host: String, port: Int): ProviderTcpConnection =
            object : ProviderTcpConnection {
                override val incoming: Flow<ByteArray> = Channel<ByteArray>(1).receiveAsFlow()
                override suspend fun write(bytes: ByteArray) = Unit
                override suspend fun shutdownOutput() = Unit
                override suspend fun close() = Unit
            }
    }
}

private class FakeProviderClock(var now: Long = 0) : ProviderClock {
    override fun nowMs(): Long = now
}

private class FakeProviderWebSocket : ProviderWebSocket {
    val incomingChannel = Channel<ByteArray>(Channel.UNLIMITED)
    val sent = mutableListOf<ByteArray>()

    override val incoming: Flow<ByteArray> = incomingChannel.receiveAsFlow()

    override suspend fun send(bytes: ByteArray) {
        sent += bytes
    }

    override suspend fun close(code: Int, reason: String) {
        incomingChannel.close()
    }
}
