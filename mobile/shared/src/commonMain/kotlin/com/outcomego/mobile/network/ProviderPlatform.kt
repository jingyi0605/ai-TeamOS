package com.outcomego.mobile.network

import kotlinx.coroutines.flow.Flow

interface ProviderClock {
    fun nowMs(): Long
}

interface ProviderWebSocket {
    val incoming: Flow<ByteArray>
    suspend fun send(bytes: ByteArray)
    suspend fun close(code: Int = 1000, reason: String = "")
}

interface ProviderWebSocketConnector {
    suspend fun connect(
        url: String,
        headers: Map<String, String>,
        protocol: String,
    ): ProviderWebSocket
}

interface ProviderTcpConnection {
    val incoming: Flow<ByteArray>
    suspend fun write(bytes: ByteArray)
    suspend fun shutdownOutput()
    suspend fun close()
}

interface ProviderTcpDialer {
    suspend fun connect(host: String, port: Int): ProviderTcpConnection
}

data class ProviderHttpResponse(
    val status: Int,
    val body: String,
)

interface ProviderHttpClient {
    suspend fun request(
        serverUrl: String,
        path: String,
        method: String,
        headers: Map<String, String> = emptyMap(),
        body: String? = null,
    ): ProviderHttpResponse
}

interface ProviderCredentialStore {
    suspend fun read(serverUrl: String, machineId: String): String?
    suspend fun write(serverUrl: String, machineId: String, credential: String)
    suspend fun clear(serverUrl: String, machineId: String)
}

interface ProviderConsentStore {
    suspend fun isAllowed(serverUrl: String, deploymentId: String): Boolean
    suspend fun setAllowed(serverUrl: String, deploymentId: String, allowed: Boolean)
}
