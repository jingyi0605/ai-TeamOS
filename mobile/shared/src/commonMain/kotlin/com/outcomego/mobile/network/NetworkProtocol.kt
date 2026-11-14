package com.outcomego.mobile.network

import com.outcomego.mobile.core.mobileJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

const val NETWORK_PROTOCOL = "outcomego-browser-network.v1"
const val LEASE_MS = 6_000L
const val RENEW_MS = 2_000L
const val MAX_STREAMS = 64
const val WINDOW = 128 * 1024
const val MAX_WINDOW = 256 * 1024
const val MAX_DATA = 64 * 1024
const val MAX_QUEUE = 16 * 1024 * 1024
const val MAX_CONTROL_PAYLOAD = 4 * 1024
const val MAX_RECONNECT_MS = 4_000L

fun validWindow(value: Int): Boolean = value == WINDOW || value == MAX_WINDOW

enum class FrameType(val wireValue: Int) {
    CONTROL(0),
    OPEN(1),
    READY(2),
    ERROR(3),
    DATA(4),
    WINDOW(5),
    FIN(6),
    RESET(7),
    ;

    companion object {
        fun fromWire(value: Int): FrameType =
            entries.firstOrNull { it.wireValue == value }
                ?: throw NetworkProtocolException("Invalid network frame type")
    }
}

data class NetworkFrame(
    val type: FrameType,
    val id: UInt,
    val payload: ByteArray,
) {
    override fun equals(other: Any?): Boolean =
        other is NetworkFrame && type == other.type && id == other.id && payload.contentEquals(other.payload)

    override fun hashCode(): Int = 31 * (31 * type.hashCode() + id.hashCode()) + payload.contentHashCode()
}

class NetworkProtocolException(message: String) : IllegalArgumentException(message)

fun encodeFrame(type: FrameType, id: UInt, payload: ByteArray = ByteArray(0)): ByteArray {
    val limit = if (type == FrameType.DATA) MAX_DATA else MAX_CONTROL_PAYLOAD
    require(payload.size <= limit) { "Network frame payload is too large" }
    require((type == FrameType.CONTROL) == (id == 0u)) { "Control frames must use stream zero" }
    require(type != FrameType.WINDOW || payload.size == 4) { "Window frames must contain four bytes" }
    require(type != FrameType.FIN || payload.isEmpty()) { "FIN frames cannot contain a payload" }
    val bytes = ByteArray(12 + payload.size)
    bytes[0] = 1
    bytes[1] = type.wireValue.toByte()
    putUShort(bytes, 2, 0)
    putUInt(bytes, 4, id)
    putUInt(bytes, 8, payload.size.toUInt())
    payload.copyInto(bytes, 12)
    return bytes
}

fun decodeFrame(bytes: ByteArray): NetworkFrame {
    if (bytes.size < 12) throw NetworkProtocolException("Invalid network frame")
    val version = bytes[0].toInt() and 0xff
    val type = FrameType.fromWire(bytes[1].toInt() and 0xff)
    val flags = readUShort(bytes, 2)
    val id = readUInt(bytes, 4)
    val size = readUInt(bytes, 8).toLong()
    val expected = bytes.size - 12
    val limit = if (type == FrameType.DATA) MAX_DATA else MAX_CONTROL_PAYLOAD
    if (
        version != 1 ||
        flags != 0 ||
        size != expected.toLong() ||
        size > limit ||
        ((type == FrameType.CONTROL) != (id == 0u)) ||
        (type == FrameType.WINDOW && size != 4L) ||
        (type == FrameType.FIN && size != 0L)
    ) {
        throw NetworkProtocolException("Invalid network frame")
    }
    return NetworkFrame(type, id, bytes.copyOfRange(12, bytes.size))
}

fun encodeJson(value: JsonElement): ByteArray =
    mobileJson.encodeToString(JsonElement.serializer(), value).encodeToByteArray()

fun decodeJsonObject(bytes: ByteArray): JsonObject =
    runCatching {
        mobileJson.parseToJsonElement(bytes.decodeToString()).jsonObject
    }.getOrElse { throw NetworkProtocolException("Invalid network message") }

@Serializable
data class OpenTarget(
    val host: String,
    val port: Int,
    val epoch: String,
)

@Serializable
data class NetworkRoute(
    val deploymentId: String,
    val mode: String,
    val machineId: String? = null,
    val revision: Int,
    val sourceOrder: List<String> = emptyList(),
    val epoch: String,
    val available: Boolean,
    val streamWindow: Int? = null,
)

fun JsonObject.toOpenTarget(): OpenTarget =
    runCatching { mobileJson.decodeFromJsonElement<OpenTarget>(this) }
        .getOrElse { throw NetworkProtocolException("Invalid network target") }
        .also { validateTarget(it) }

fun JsonObject.toNetworkRoute(): NetworkRoute =
    runCatching { mobileJson.decodeFromJsonElement<NetworkRoute>(this) }
        .getOrElse { throw NetworkProtocolException("Invalid network route") }
        .also {
            if (it.mode != "server" && it.mode != "device") throw NetworkProtocolException("Invalid route mode")
            if (
                it.revision < 0 ||
                it.epoch.isBlank() ||
                it.epoch.length > 100 ||
                it.deploymentId.isBlank() ||
                it.deploymentId.length > 100 ||
                (it.mode == "device" && it.machineId.isNullOrBlank())
            )
                throw NetworkProtocolException("Invalid network route")
            if (it.streamWindow != null && !validWindow(it.streamWindow))
                throw NetworkProtocolException("Invalid network window")
        }

fun validateTarget(target: OpenTarget) {
    if (
        target.host.isBlank() ||
        target.host.length > 253 ||
        target.host.any { it.isWhitespace() || it.code < 0x20 || it == '/' || it == '@' || it == '?' || it == '#' || it == '%' || it == '\\' } ||
        target.port !in 1..65_535 ||
        target.epoch.length > 100
    ) {
        throw NetworkProtocolException("Invalid network target")
    }
}

fun controlMessage(op: String, sequence: Long, fields: JsonObject = JsonObject(emptyMap())): JsonObject =
    buildJsonObject {
        put("op", op)
        put("sequence", sequence)
        fields.forEach { (key, value) -> put(key, value) }
    }

fun JsonObject.stringField(name: String): String =
    this[name]?.jsonPrimitive?.content
        ?: throw NetworkProtocolException("Missing network field: $name")

fun JsonObject.longField(name: String): Long =
    this[name]?.jsonPrimitive?.longOrNull
        ?: throw NetworkProtocolException("Invalid network field: $name")

fun JsonObject.intField(name: String): Int =
    longField(name).let {
        if (it !in Int.MIN_VALUE..Int.MAX_VALUE) throw NetworkProtocolException("Invalid network field: $name")
        it.toInt()
    }

private fun putUShort(target: ByteArray, offset: Int, value: Int) {
    target[offset] = (value ushr 8).toByte()
    target[offset + 1] = value.toByte()
}

private fun putUInt(target: ByteArray, offset: Int, value: UInt) {
    target[offset] = (value shr 24).toByte()
    target[offset + 1] = (value shr 16).toByte()
    target[offset + 2] = (value shr 8).toByte()
    target[offset + 3] = value.toByte()
}

private fun readUShort(bytes: ByteArray, offset: Int): Int =
    ((bytes[offset].toInt() and 0xff) shl 8) or (bytes[offset + 1].toInt() and 0xff)

private fun readUInt(bytes: ByteArray, offset: Int): UInt =
    ((bytes[offset].toUInt() and 0xffu) shl 24) or
        ((bytes[offset + 1].toUInt() and 0xffu) shl 16) or
        ((bytes[offset + 2].toUInt() and 0xffu) shl 8) or
        (bytes[offset + 3].toUInt() and 0xffu)
