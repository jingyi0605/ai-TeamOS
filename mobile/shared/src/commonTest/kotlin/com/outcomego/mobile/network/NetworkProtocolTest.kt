package com.outcomego.mobile.network

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class NetworkProtocolTest {
    @Test
    fun binaryFrameRoundTripsWithBigEndianHeader() {
        val payload = byteArrayOf(1, 2, 3, 4)
        val decoded = decodeFrame(encodeFrame(FrameType.DATA, 7u, payload))
        assertEquals(FrameType.DATA, decoded.type)
        assertEquals(7u, decoded.id)
        assertContentEquals(payload, decoded.payload)
    }

    @Test
    fun controlFramesUseStreamZero() {
        assertFailsWith<IllegalArgumentException> {
            encodeFrame(FrameType.CONTROL, 1u)
        }
        assertFailsWith<IllegalArgumentException> {
            decodeFrame(byteArrayOf(1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0))
        }
    }

    @Test
    fun malformedWindowAndFinFramesAreRejected() {
        assertFailsWith<IllegalArgumentException> {
            encodeFrame(FrameType.WINDOW, 1u, byteArrayOf(1))
        }
        assertFailsWith<IllegalArgumentException> {
            encodeFrame(FrameType.FIN, 1u, byteArrayOf(1))
        }
    }

    @Test
    fun routeAndTargetModelsValidateWireFields() {
        val target = buildJsonObject {
            put("host", "example.com")
            put("port", 443)
            put("epoch", "epoch-1")
        }.toOpenTarget()
        assertEquals("example.com", target.host)
        assertEquals(443, target.port)

        val route = buildJsonObject {
            put("deploymentId", "deployment")
            put("mode", "device")
            put("machineId", "machine")
            put("revision", 4)
            put("epoch", "epoch-1")
            put("available", true)
            put("streamWindow", WINDOW)
        }.toNetworkRoute()
        assertEquals(WINDOW, route.streamWindow)
        assertEquals("device", route.mode)
    }
}
