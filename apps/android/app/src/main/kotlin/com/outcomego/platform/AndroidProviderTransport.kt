package com.outcomego.platform

import com.outcomego.android.data.AndroidOutcomeGoTransport
import com.outcomego.mobile.core.HttpMethod
import com.outcomego.mobile.core.MobileRequest
import com.outcomego.mobile.network.ProviderHttpClient
import com.outcomego.mobile.network.ProviderHttpResponse
import com.outcomego.mobile.network.ProviderTcpConnection
import com.outcomego.mobile.network.ProviderTcpDialer
import com.outcomego.mobile.network.ProviderWebSocket
import com.outcomego.mobile.network.ProviderWebSocketConnector
import com.outcomego.mobile.network.canonicalHost
import com.outcomego.mobile.network.isPublicAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AndroidProviderHttpClient : ProviderHttpClient {
    private val transport = AndroidOutcomeGoTransport()

    override suspend fun request(
        serverUrl: String,
        path: String,
        method: String,
        headers: Map<String, String>,
        body: String?,
    ): ProviderHttpResponse {
        val result = transport.execute(
            serverUrl,
            token = null,
            request = MobileRequest(
                method = HttpMethod.valueOf(method),
                path = path,
                headers = headers,
                body = body,
            ),
        )
        return ProviderHttpResponse(result.status, result.body)
    }
}

class AndroidProviderWebSocketConnector : ProviderWebSocketConnector {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    override suspend fun connect(
        url: String,
        headers: Map<String, String>,
        protocol: String,
    ): ProviderWebSocket = suspendCancellableCoroutine { continuation ->
        val incoming = Channel<ByteArray>(capacity = 64)
        val request = Request.Builder()
            .url(url)
            .header("Sec-WebSocket-Protocol", protocol)
            .apply { headers.forEach { (key, value) -> header(key, value) } }
            .build()
        lateinit var webSocket: WebSocket
        var resumed = false
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(socket: WebSocket, response: Response) {
                if (resumed) return
                resumed = true
                continuation.resume(AndroidProviderWebSocket(socket, incoming))
            }

            override fun onMessage(socket: WebSocket, bytes: ByteString) {
                incoming.trySend(bytes.toByteArray())
            }

            override fun onMessage(socket: WebSocket, text: String) {
                socket.close(1002, "Binary messages required")
                incoming.close(IllegalStateException("Text WebSocket message"))
            }

            override fun onFailure(socket: WebSocket, t: Throwable, response: Response?) {
                incoming.close(t)
                if (!resumed) {
                    resumed = true
                    continuation.resumeWithException(t)
                }
            }

            override fun onClosed(socket: WebSocket, code: Int, reason: String) {
                incoming.close()
            }
        })
        continuation.invokeOnCancellation {
            webSocket.cancel()
            incoming.close()
        }
    }
}

private class AndroidProviderWebSocket(
    private val socket: WebSocket,
    private val incomingChannel: Channel<ByteArray>,
) : ProviderWebSocket {
    override val incoming: Flow<ByteArray> = incomingChannel.receiveAsFlow()

    override suspend fun send(bytes: ByteArray) {
        check(socket.send(ByteString.of(*bytes))) { "WebSocket send failed" }
    }

    override suspend fun close(code: Int, reason: String) {
        socket.close(code, reason)
        incomingChannel.close()
    }
}

class AndroidProviderTcpDialer : ProviderTcpDialer {
    override suspend fun connect(host: String, port: Int): ProviderTcpConnection {
        val canonical = canonicalHost(host)
        val addresses = withContext(Dispatchers.IO) {
            java.net.InetAddress.getAllByName(canonical).toList()
        }
        if (addresses.isEmpty() || addresses.any { !isPublicAddress(it.hostAddress.orEmpty()) }) {
            throw IllegalStateException("destination_blocked")
        }
        var lastError: Throwable? = null
        for (address in addresses.take(8)) {
            val socket = Socket()
            try {
                withContext(Dispatchers.IO) {
                    socket.connect(InetSocketAddress(address, port), 3_000)
                    socket.tcpNoDelay = true
                    socket.keepAlive = true
                }
                return AndroidProviderTcpConnection(socket)
            } catch (error: Throwable) {
                lastError = error
                runCatching { socket.close() }
            }
        }
        throw lastError ?: IOException("connect_failed")
    }
}

private class AndroidProviderTcpConnection(
    private val socket: Socket,
) : ProviderTcpConnection {
    override val incoming: Flow<ByteArray> = kotlinx.coroutines.flow.channelFlow {
        withContext(Dispatchers.IO) {
            val input = socket.getInputStream()
            val buffer = ByteArray(64 * 1024)
            try {
                while (!socket.isClosed) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (count == 0) continue
                    send(buffer.copyOf(count))
                }
            } finally {
                close()
            }
        }
    }

    override suspend fun write(bytes: ByteArray) {
        withContext(Dispatchers.IO) {
            socket.getOutputStream().write(bytes)
            socket.getOutputStream().flush()
        }
    }

    override suspend fun shutdownOutput() {
        withContext(Dispatchers.IO) { runCatching { socket.shutdownOutput() } }
    }

    override suspend fun close() {
        withContext(Dispatchers.IO) { runCatching { socket.close() } }
    }
}
