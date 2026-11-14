package com.outcomego.mobile.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

interface ProviderWire {
    suspend fun send(bytes: ByteArray)
    suspend fun close()
}

class ProviderStream internal constructor(
    val id: UInt,
    private val mux: ProviderMux,
    private val window: Int,
) {
    private val inbound = Channel<ByteArray>(capacity = 64)
    private val state = Mutex()
    private var sendCredit = window
    private var receiveCredit = window
    private var consumed = 0
    private var accepted = false
    private var remoteEnded = false
    private var localEnded = false
    private var closed = false
    private var creditChanged = Channel<Unit>(capacity = Channel.CONFLATED)

    suspend fun awaitReady() {
        while (true) {
            state.withLock {
                if (accepted) return
                if (closed) throw NetworkProtocolException("Stream closed")
            }
            delay(5)
        }
    }

    suspend fun write(bytes: ByteArray) {
        var offset = 0
        while (offset < bytes.size) {
            val length = state.withLock {
                if (closed || localEnded) throw NetworkProtocolException("Stream is closed")
                minOf(MAX_DATA, bytes.size - offset, sendCredit)
            }
            if (length == 0) {
                creditChanged.receiveCatching()
                continue
            }
            val chunk = bytes.copyOfRange(offset, offset + length)
            state.withLock { sendCredit -= length }
            mux.sendFrame(FrameType.DATA, id, chunk)
            offset += length
        }
    }

    suspend fun receive(): ByteArray? {
        val result = inbound.receiveCatching().getOrNull() ?: return null
        mux.releaseInbound(result.size)
        val replenish = state.withLock {
            consumed += result.size
            val value = consumed
            consumed = 0
            receiveCredit += value
            value
        }
        if (replenish > 0) {
            val payload = ByteArray(4)
            putUInt(payload, 0, replenish.toUInt())
            mux.sendFrame(FrameType.WINDOW, id, payload)
        }
        return result
    }

    suspend fun finish() {
        state.withLock {
            if (closed || localEnded) return
            localEnded = true
        }
        mux.sendFrame(FrameType.FIN, id)
        maybeClose()
    }

    suspend fun reset(code: String = "stream_closed") {
        val shouldSend = state.withLock {
            if (closed) false else {
                closed = true
                true
            }
        }
        if (shouldSend) {
            inbound.close()
            mux.sendFrame(FrameType.RESET, id, jsonBytes(buildJsonObject { put("code", code) }))
            mux.remove(id)
        }
    }

    internal suspend fun markReady() {
        state.withLock {
            if (accepted) throw NetworkProtocolException("Duplicate stream acknowledgement")
            accepted = true
            sendCredit = window
        }
        creditChanged.trySend(Unit)
    }

    internal suspend fun receiveRemote(type: FrameType, payload: ByteArray) {
        when (type) {
            FrameType.READY -> {
                if (payload.isNotEmpty()) throw NetworkProtocolException("Invalid ready frame")
                markReady()
            }
            FrameType.ERROR, FrameType.RESET -> {
                resetWithoutFrame()
            }
            FrameType.DATA -> {
                state.withLock {
                    if (!accepted || remoteEnded || payload.size > receiveCredit)
                        throw NetworkProtocolException("Stream receive window exceeded")
                    receiveCredit -= payload.size
                }
                mux.reserveInbound(payload.size)
                try {
                    inbound.send(payload)
                } catch (error: Throwable) {
                    mux.releaseInbound(payload.size)
                    throw error
                }
            }
            FrameType.WINDOW -> {
                if (payload.size != 4) throw NetworkProtocolException("Invalid stream window")
                val count = readUInt(payload, 0).toLong()
                if (count <= 0 || count > Int.MAX_VALUE) throw NetworkProtocolException("Invalid stream credit")
                state.withLock {
                    if (sendCredit.toLong() + count > window) throw NetworkProtocolException("Invalid stream credit")
                    sendCredit += count.toInt()
                }
                creditChanged.trySend(Unit)
            }
            FrameType.FIN -> {
                if (payload.isNotEmpty()) throw NetworkProtocolException("Invalid FIN")
                state.withLock {
                    if (remoteEnded) throw NetworkProtocolException("Duplicate FIN")
                    remoteEnded = true
                }
                inbound.close()
                maybeClose()
            }
            else -> throw NetworkProtocolException("Invalid stream frame")
        }
    }

    internal suspend fun resetWithoutFrame() {
        state.withLock { closed = true }
        while (true) {
            val queued = inbound.tryReceive().getOrNull() ?: break
            mux.releaseInbound(queued.size)
        }
        inbound.close()
        mux.remove(id)
    }

    private suspend fun maybeClose() {
        val done = state.withLock {
            if (!closed && localEnded && remoteEnded) {
                closed = true
                true
            } else false
        }
        if (done) {
            inbound.close()
            mux.remove(id)
        }
    }
}

class ProviderMux(
    private val wire: ProviderWire,
    private val scope: CoroutineScope,
    private val dialer: ProviderTcpDialer,
    private val onControl: suspend (JsonObject) -> Unit,
    private val targetAllowed: (OpenTarget) -> Boolean = { true },
    private val onChanged: () -> Unit = {},
) {
    private val streams = linkedMapOf<UInt, ProviderStream>()
    private val pendingDials = mutableMapOf<UInt, Job>()
    private var nextOutgoingId = 1u
    private var highestIncomingId = 0u
    private var closed = false
    private var sentSequence = 0L
    private var receivedSequence = 0L
    private var window = WINDOW
    private var queuedInboundBytes = 0

    fun configureWindow(value: Int) {
        if (!validWindow(value)) throw NetworkProtocolException("Invalid stream window")
        if (streams.isNotEmpty() && value != window) throw NetworkProtocolException("Window changed on active route")
        window = value
    }

    suspend fun sendControl(fields: JsonObject) {
        val sequence = ++sentSequence
        sendFrame(FrameType.CONTROL, 0u, encodeJson(buildJsonObject {
            fields.forEach { (key, value) -> put(key, value) }
            put("sequence", sequence)
        }))
    }

    suspend fun receive(bytes: ByteArray) {
        if (queuedInboundBytes + bytes.size > MAX_QUEUE) {
            throw NetworkProtocolException("Network buffer budget exceeded")
        }
        val frame = decodeFrame(bytes)
        if (frame.type == FrameType.CONTROL) {
            val message = decodeJsonObject(frame.payload)
            val sequence = message.longField("sequence")
            if (sequence <= receivedSequence) throw NetworkProtocolException("Stale control message")
            receivedSequence = sequence
            onControl(message)
            return
        }
        if (frame.type == FrameType.OPEN) {
            if (frame.id <= highestIncomingId) throw NetworkProtocolException("Invalid stream open")
            highestIncomingId = frame.id
            if (streams.size >= MAX_STREAMS) {
                sendError(frame.id, "capacity_exceeded")
                return
            }
            val target = decodeJsonObject(frame.payload).toOpenTarget()
            if (!targetAllowed(target)) {
                sendError(frame.id, "device_unavailable")
                return
            }
            val stream = ProviderStream(frame.id, this, window)
            streams[frame.id] = stream
            onChanged()
            val dialJob = scope.launch {
                var connection: ProviderTcpConnection? = null
                try {
                    connection = dialer.connect(canonicalHost(target.host), target.port)
                    if (!targetAllowed(target) || streams[frame.id] !== stream) {
                        connection?.close()
                        stream.resetWithoutFrame()
                        return@launch
                    }
                    stream.markReady()
                    sendFrame(FrameType.READY, frame.id)
                    bridge(stream, connection!!)
                } catch (error: Throwable) {
                    if (streams[frame.id] === stream) {
                        sendError(frame.id, errorCode(error))
                        stream.resetWithoutFrame()
                    }
                    connection?.close()
                } finally {
                    pendingDials.remove(frame.id)
                }
            }
            pendingDials[frame.id] = dialJob
            if (!dialJob.isActive) pendingDials.remove(frame.id)
            return
        }
        val stream = streams[frame.id]
        if (stream != null) {
            stream.receiveRemote(frame.type, frame.payload)
        } else if (frame.id > highestIncomingId) {
            throw NetworkProtocolException("Unknown network stream")
        }
    }

    fun activeConnections(): Int = streams.size

    suspend fun reset() {
        pendingDials.values.toList().forEach { it.cancel() }
        pendingDials.clear()
        streams.values.toList().forEach { it.resetWithoutFrame() }
        streams.clear()
        queuedInboundBytes = 0
        highestIncomingId = 0u
        onChanged()
    }

    suspend fun close() {
        if (closed) return
        closed = true
        reset()
        wire.close()
    }

    internal suspend fun sendFrame(type: FrameType, id: UInt, payload: ByteArray = ByteArray(0)) {
        if (closed) return
        wire.send(encodeFrame(type, id, payload))
    }

    internal fun remove(id: UInt) {
        pendingDials.remove(id)?.cancel()
        if (streams.remove(id) != null) onChanged()
    }

    internal fun reserveInbound(bytes: Int) {
        if (bytes < 0 || queuedInboundBytes + bytes > MAX_QUEUE) {
            throw NetworkProtocolException("Network buffer budget exceeded")
        }
        queuedInboundBytes += bytes
    }

    internal fun releaseInbound(bytes: Int) {
        queuedInboundBytes = (queuedInboundBytes - bytes).coerceAtLeast(0)
    }

    private suspend fun sendError(id: UInt, code: String) {
        sendFrame(FrameType.ERROR, id, jsonBytes(buildJsonObject { put("code", code) }))
    }

    private suspend fun bridge(stream: ProviderStream, socket: ProviderTcpConnection) {
        try {
            kotlinx.coroutines.coroutineScope {
                val upload = launch {
                    socket.incoming.collect { stream.write(it) }
                    stream.finish()
                }
                val download = launch {
                    while (true) {
                        val data = stream.receive() ?: break
                        socket.write(data)
                    }
                    socket.shutdownOutput()
                }
                upload.join()
                download.cancel()
            }
        } finally {
            socket.close()
            stream.resetWithoutFrame()
        }
    }

    private fun errorCode(error: Throwable): String =
        when (error) {
            is NetworkTargetException -> error.code
            is MobileEnrollmentException -> "machine_unauthorized"
            is kotlinx.coroutines.CancellationException -> "request_cancelled"
            else -> "connect_failed"
        }
}

private fun putUInt(target: ByteArray, offset: Int, value: UInt) {
    target[offset] = (value shr 24).toByte()
    target[offset + 1] = (value shr 16).toByte()
    target[offset + 2] = (value shr 8).toByte()
    target[offset + 3] = value.toByte()
}

private fun readUInt(bytes: ByteArray, offset: Int): UInt =
    ((bytes[offset].toUInt() and 0xffu) shl 24) or
        ((bytes[offset + 1].toUInt() and 0xffu) shl 16) or
        ((bytes[offset + 2].toUInt() and 0xffu) shl 8) or
        (bytes[offset + 3].toUInt() and 0xffu)

private fun jsonBytes(value: JsonObject): ByteArray = encodeJson(value)
