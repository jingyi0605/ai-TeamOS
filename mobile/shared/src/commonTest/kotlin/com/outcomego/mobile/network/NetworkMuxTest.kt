package com.outcomego.mobile.network

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkMuxTest {
    @Test
    fun rejectsOpenWhenRouteEpochIsNoLongerAllowed() = runTest {
        val sent = mutableListOf<NetworkFrame>()
        val mux = ProviderMux(
            wire = recordingWire(sent),
            scope = this,
            dialer = immediateDialer(),
            onControl = {},
            targetAllowed = { false },
        )

        mux.receive(openFrame(1u, "old-epoch"))

        assertEquals(FrameType.ERROR, sent.single().type)
        assertEquals(1u, sent.single().id)
        assertEquals("device_unavailable", decodeJsonObject(sent.single().payload).stringField("code"))
    }

    @Test
    fun cancelsLateDialWhenRouteIsReset() = runTest {
        val sent = mutableListOf<NetworkFrame>()
        val started = CompletableDeferred<Unit>()
        var cancelled = false
        val mux = ProviderMux(
            wire = recordingWire(sent),
            scope = this,
            dialer = object : ProviderTcpDialer {
                override suspend fun connect(host: String, port: Int): ProviderTcpConnection {
                    started.complete(Unit)
                    return suspendCancellableCoroutine { continuation ->
                        continuation.invokeOnCancellation { cancelled = true }
                    }
                }
            },
            onControl = {},
            targetAllowed = { true },
        )

        mux.receive(openFrame(1u, "epoch"))
        started.await()
        mux.reset()
        runCurrent()

        assertTrue(sent.none { it.type == FrameType.READY })
        assertTrue(cancelled)
        assertEquals(0, mux.activeConnections())
    }

    @Test
    fun forwardsDataAndReturnsWindowCredit() = runTest {
        val sent = mutableListOf<NetworkFrame>()
        val connection = FakeTcpConnection()
        val mux = ProviderMux(
            wire = recordingWire(sent),
            scope = this,
            dialer = object : ProviderTcpDialer {
                override suspend fun connect(host: String, port: Int): ProviderTcpConnection = connection
            },
            onControl = {},
            targetAllowed = { true },
        )

        mux.receive(openFrame(1u, "epoch"))
        runCurrent()
        assertEquals(FrameType.READY, sent.single().type)

        mux.receive(encodeFrame(FrameType.DATA, 1u, byteArrayOf(1, 2, 3)))
        runCurrent()
        assertEquals(byteArrayOf(1, 2, 3).toList(), connection.writes.single().toList())
        connection.incomingChannel.close()
        runCurrent()

        assertTrue(sent.any { it.type == FrameType.FIN && it.id == 1u })
    }

    private fun openFrame(id: UInt, epoch: String): ByteArray =
        encodeFrame(
            FrameType.OPEN,
            id,
            encodeJson(
                buildJsonObject {
                    put("host", "example.com")
                    put("port", 443)
                    put("epoch", epoch)
                },
            ),
        )

    private fun recordingWire(sent: MutableList<NetworkFrame>) = object : ProviderWire {
        override suspend fun send(bytes: ByteArray) {
            sent += decodeFrame(bytes)
        }

        override suspend fun close() = Unit
    }

    private fun immediateDialer() = object : ProviderTcpDialer {
        override suspend fun connect(host: String, port: Int): ProviderTcpConnection = FakeTcpConnection()
    }
}

private class FakeTcpConnection : ProviderTcpConnection {
    val incomingChannel = Channel<ByteArray>(Channel.UNLIMITED)
    val writes = mutableListOf<ByteArray>()
    var closed = false

    override val incoming: Flow<ByteArray> = incomingChannel.receiveAsFlow()

    override suspend fun write(bytes: ByteArray) {
        writes += bytes
    }

    override suspend fun shutdownOutput() = Unit

    override suspend fun close() {
        closed = true
        incomingChannel.close()
    }
}
