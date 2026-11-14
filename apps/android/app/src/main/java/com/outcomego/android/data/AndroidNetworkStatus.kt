package com.outcomego.android.data

import com.outcomego.mobile.core.HttpMethod
import com.outcomego.mobile.core.MobileRequest
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
    val connectedToThisPhone: Boolean = false,
    val activeMachineId: String? = null,
    val activeConnections: Int = 0,
    val bytesSent: Long = 0,
    val bytesReceived: Long = 0,
    val egressIp: String? = null,
    val egressLocation: String? = null,
    val latencyMs: Int? = null,
    val downloadMbps: Double? = null,
    val uploadMbps: Double? = null,
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
        val connected = state == "ready" && activeMachineId == localMachineId
        val egress = if (state == "ready" || state == "server") {
            runCatching {
                request(session, HttpMethod.POST, "/api/v0/server-settings/browser-network/egress")
            }.getOrNull()
        } else {
            null
        }
        val quality = if (includeQuality && (state == "ready" || state == "server")) {
            runCatching {
                request(session, HttpMethod.POST, "/api/v0/server-settings/browser-network/quality")
            }.getOrNull()
        } else {
            null
        }
        return AndroidNetworkDetails(
            providerState = local.state,
            computerState = state,
            connectedToThisPhone = connected,
            activeMachineId = activeMachineId,
            activeConnections = runtime.int("activeConnections"),
            bytesSent = runtime.long("bytesSent"),
            bytesReceived = runtime.long("bytesReceived"),
            egressIp = egress?.string("ip"),
            egressLocation = egress?.string("location"),
            latencyMs = quality?.int("latencyMs"),
            downloadMbps = quality?.double("downloadMbps"),
            uploadMbps = quality?.double("uploadMbps"),
        )
    }

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
}

private fun JsonObject.string(name: String): String =
    this[name]?.jsonPrimitive?.contentOrNull.orEmpty()

private fun JsonObject.int(name: String): Int =
    this[name]?.jsonPrimitive?.intOrNull ?: 0

private fun JsonObject.long(name: String): Long =
    this[name]?.jsonPrimitive?.longOrNull ?: 0L

private fun JsonObject.double(name: String): Double? =
    this[name]?.jsonPrimitive?.doubleOrNull
