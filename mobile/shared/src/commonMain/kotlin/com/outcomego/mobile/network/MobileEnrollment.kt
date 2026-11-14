package com.outcomego.mobile.network

import com.outcomego.mobile.core.mobileJson
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

data class MobileEnrollment(
    val serverUrl: String,
    val machineId: String,
    val credential: String,
    val connectionId: String,
    val deploymentId: String,
)

class MobileEnrollmentClient(
    private val http: ProviderHttpClient,
) {
    suspend fun enroll(
        serverUrl: String,
        sessionToken: String,
        machineId: String,
        label: String,
    ): MobileEnrollment {
        require(sessionToken.isNotBlank()) { "A signed-in session is required" }
        require(machineId.isUuid()) { "Invalid machine identity" }
        require(label.isNotBlank() && label.length <= 200) { "Invalid machine label" }

        val enrolled = request(
            serverUrl = serverUrl,
            path = "/api/v0/machines/enroll",
            method = "POST",
            headers = mapOf(
                "Authorization" to "Bearer $sessionToken",
                "Content-Type" to "application/json",
            ),
            body = mobileJson.encodeToString(
                buildJsonObject {
                    put("machineId", machineId)
                    put("label", label.trim())
                },
            ),
        )
        val credential = enrolled.stringField("credential").also {
            if (it.isBlank()) throw NetworkProtocolException("Invalid machine credential")
        }
        if (enrolled.stringField("machineId") != machineId) {
            throw NetworkProtocolException("Server returned a different machine identity")
        }

        val machineHeaders = mapOf(
            "Authorization" to "Bearer $credential",
            "x-outcomego-machine-id" to machineId,
        )
        val connected = request(
            serverUrl = serverUrl,
            path = "/api/v0/machines/channel/connect",
            method = "POST",
            headers = machineHeaders,
        )
        val connectionId = connected.stringField("connectionId").also {
            if (it.isBlank()) throw NetworkProtocolException("Invalid connection identity")
        }

        val identity = request(
            serverUrl = serverUrl,
            path = "/api/v0/machines/channel/browser-network-identity",
            method = "GET",
            headers = machineHeaders + ("x-outcomego-connection-id" to connectionId),
        )
        val deploymentId = identity.stringField("deploymentId")
        if (!deploymentId.isUuid()) throw NetworkProtocolException("Invalid deployment identity")
        return MobileEnrollment(serverUrl, machineId, credential, connectionId, deploymentId)
    }

    suspend fun resume(
        serverUrl: String,
        machineId: String,
        credential: String,
    ): MobileEnrollment {
        require(machineId.isUuid()) { "Invalid machine identity" }
        require(credential.isNotBlank()) { "Invalid machine credential" }
        val headers = mapOf(
            "Authorization" to "Bearer $credential",
            "x-outcomego-machine-id" to machineId,
        )
        val connected = request(
            serverUrl = serverUrl,
            path = "/api/v0/machines/channel/connect",
            method = "POST",
            headers = headers,
        )
        val connectionId = connected.stringField("connectionId").also {
            if (it.isBlank()) throw NetworkProtocolException("Invalid connection identity")
        }
        val identity = request(
            serverUrl = serverUrl,
            path = "/api/v0/machines/channel/browser-network-identity",
            method = "GET",
            headers = headers + ("x-outcomego-connection-id" to connectionId),
        )
        val deploymentId = identity.stringField("deploymentId")
        if (!deploymentId.isUuid()) throw NetworkProtocolException("Invalid deployment identity")
        return MobileEnrollment(serverUrl, machineId, credential, connectionId, deploymentId)
    }

    private suspend fun request(
        serverUrl: String,
        path: String,
        method: String,
        headers: Map<String, String>,
        body: String? = null,
    ): kotlinx.serialization.json.JsonObject {
        val response = http.request(serverUrl, path, method, headers, body)
        if (response.status !in 200..299) {
            throw MobileEnrollmentException(response.status, safeErrorCode(response.body))
        }
        return runCatching { mobileJson.parseToJsonElement(response.body).jsonObject }
            .getOrElse { throw NetworkProtocolException("Invalid enrollment response") }
    }

    private fun safeErrorCode(body: String): String =
        runCatching {
            mobileJson.parseToJsonElement(body).jsonObject["error"]
                ?.let { it.jsonObject["code"]?.toString() }
                ?: "http_error"
        }.getOrDefault("http_error")
}

class MobileEnrollmentException(
    val status: Int,
    val code: String,
) : IllegalStateException("Mobile enrollment failed: $code")

private fun String.isUuid(): Boolean =
    Regex("^[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}$", RegexOption.IGNORE_CASE).matches(this)
