package com.outcomego.android.data

import com.outcomego.mobile.core.HttpMethod
import com.outcomego.mobile.core.MobileRequest
import com.outcomego.mobile.core.OutcomeGoRequestException
import com.outcomego.mobile.core.mobileJson
import com.outcomego.mobile.core.requireSuccess
import com.outcomego.mobile.network.ProviderSnapshot
import com.outcomego.mobile.network.ProviderState
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.outcomego.mobile.core.Session

data class AndroidNetworkDetails(
    val providerState: ProviderState,
    val computerState: String = "unavailable",
    val routeSelectedToThisPhone: Boolean = false,
    val connectedToThisPhone: Boolean = false,
    val egressAvailable: Boolean = false,
    val activeMachineId: String? = null,
    val activeConnections: Int = 0,
    val bytesSent: Long = 0,
    val bytesReceived: Long = 0,
    val egressIp: String? = null,
    val egressLocation: String? = null,
    val latencyMs: Int? = null,
    val downloadMbps: Double? = null,
    val uploadMbps: Double? = null,
    val egressErrorCode: String? = null,
    val qualityErrorCode: String? = null,
    val error: String? = null,
)

class AndroidNetworkStatusLoader(
    private val context: android.content.Context,
) {
    private val sessions = AndroidSecureSessionStore(context)
    private val transport = AndroidOutcomeGoTransport()

    suspend fun load(includeQuality: Boolean = true): AndroidNetworkDetails = withContext(Dispatchers.IO) {
        val local = com.outcomego.platform.AndroidNetworkProvider.snapshot.value
        val session = sessions.readActive()
            ?: return@withContext AndroidNetworkDetails(
                providerState = local.state,
                error = "Sign in to view cloud computer networking.",
            )
        runCatching { loadFromServer(session, local, includeQuality) }
            .getOrElse { error ->
                AndroidNetworkDetails(
                    providerState = local.state,
                    error = error.message ?: "Cloud computer networking is unavailable.",
                )
            }
    }

    private suspend fun loadFromServer(
        session: Session,
        local: ProviderSnapshot,
        includeQuality: Boolean,
    ): AndroidNetworkDetails {
        val view = request(session, HttpMethod.GET, "/api/v0/server-settings/browser-network")
        val runtime = view["runtime"]?.jsonObject ?: error("The server returned no network runtime.")
        val state = runtime.string("state").ifBlank { "unavailable" }
        val activeMachineId = runtime.string("activeMachineId").ifBlank { null }
        val localMachineId = local.machineId ?: AndroidProviderStore(context).machineId
        val routeSelectedToThisPhone = state == "ready" && activeMachineId == localMachineId
        val egressProbe = if (state == "ready" || state == "server") {
            probe(session, "/api/v0/server-settings/browser-network/egress")
        } else {
            ProbeResult(null, null)
        }
        val qualityProbe = if (includeQuality && (state == "ready" || state == "server")) {
            probe(session, "/api/v0/server-settings/browser-network/quality")
        } else {
            ProbeResult(null, null)
        }
        val egress = egressProbe.value
        val egressAvailable = egress != null &&
            egress.string("sourceId") == (activeMachineId ?: "server") &&
            egress.string("epoch") == runtime.string("routeEpoch")
        val connected = routeSelectedToThisPhone && egressAvailable
        val egressErrorCode = egressProbe.errorCode
            ?: if (egress != null && !egressAvailable) "network_egress_changed" else null
        return AndroidNetworkDetails(
            providerState = local.state,
            computerState = state,
            routeSelectedToThisPhone = routeSelectedToThisPhone,
            connectedToThisPhone = connected,
            egressAvailable = egressAvailable,
            activeMachineId = activeMachineId,
            activeConnections = runtime.int("activeConnections"),
            bytesSent = runtime.long("bytesSent"),
            bytesReceived = runtime.long("bytesReceived"),
            egressIp = egress?.string("ip")?.takeIf { egressAvailable },
            egressLocation = egress?.string("location")?.takeIf { egressAvailable },
            latencyMs = qualityProbe.value?.int("latencyMs")?.takeIf { egressAvailable },
            downloadMbps = qualityProbe.value?.double("downloadMbps")?.takeIf { egressAvailable },
            uploadMbps = qualityProbe.value?.double("uploadMbps")?.takeIf { egressAvailable },
            egressErrorCode = egressErrorCode,
            qualityErrorCode = qualityProbe.errorCode,
            error = when {
                egressErrorCode != null -> userMessage(egressErrorCode)
                qualityProbe.errorCode != null ->
                    "Egress is connected, but the speed test is unavailable (${qualityProbe.errorCode})."
                else -> null
            },
        )
    }

    private suspend fun probe(
        session: Session,
        path: String,
    ): ProbeResult = runCatching {
        request(session, HttpMethod.POST, path)
    }.fold(
        onSuccess = { ProbeResult(it, null) },
        onFailure = { ProbeResult(null, it.errorCode()) },
    )

    private suspend fun request(
        session: Session,
        method: HttpMethod,
        path: String,
    ): JsonObject {
        val response = transport.execute(
            session.server,
            session.token,
            MobileRequest(method = method, path = path, body = if (method == HttpMethod.POST) "{}" else null),
        ).requireSuccess()
        return mobileJson.parseToJsonElement(response.body).jsonObject
    }

    private fun Throwable.errorCode(): String =
        (this as? OutcomeGoRequestException)?.error?.code
            ?: message?.takeIf { it.isNotBlank() }
            ?: "network_probe_failed"

    private fun userMessage(code: String): String = when (code) {
        "network_egress_unavailable" ->
            "The phone is selected, but it cannot reach the public internet."
        "network_egress_changed" ->
            "The network route changed while it was being checked. Refresh and try again."
        "computer_unavailable" ->
            "The cloud computer network worker is unavailable."
        else -> "The selected network egress is unavailable ($code)."
    }

    private data class ProbeResult(
        val value: JsonObject?,
        val errorCode: String?,
    )
}

private fun JsonObject.string(name: String): String =
    this[name]?.jsonPrimitive?.contentOrNull.orEmpty()

private fun JsonObject.int(name: String): Int =
    this[name]?.jsonPrimitive?.intOrNull ?: 0

private fun JsonObject.long(name: String): Long =
    this[name]?.jsonPrimitive?.longOrNull ?: 0L

private fun JsonObject.double(name: String): Double? =
    this[name]?.jsonPrimitive?.doubleOrNull
