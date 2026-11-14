package com.outcomego.mobile.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PublicTargetPolicyTest {
    @Test
    fun canonicalizesPublicHostnames() {
        assertEquals("example.com", canonicalHost("Example.COM."))
        assertEquals("93.184.216.34", canonicalHost("93.184.216.34"))
    }

    @Test
    fun blocksPrivateAndSpecialIpv4Ranges() {
        listOf(
            "10.0.0.1",
            "127.0.0.1",
            "169.254.1.1",
            "172.16.0.1",
            "192.168.1.1",
            "224.0.0.1",
        ).forEach { address ->
            assertFailsWith<NetworkTargetException> { canonicalHost(address) }
        }
    }

    @Test
    fun blocksLocalAndMalformedHostnames() {
        listOf("localhost", "printer.local", "https://example.com", "a/b", "bad host").forEach {
            assertFailsWith<NetworkTargetException> { canonicalHost(it) }
        }
    }

    @Test
    fun acceptsPublicIpv6AndRejectsPrivateIpv6() {
        assertTrue(isPublicAddress("2001:4860:4860::8888"))
        assertFailsWith<NetworkTargetException> { canonicalHost("::1") }
        assertFailsWith<NetworkTargetException> { canonicalHost("fc00::1") }
    }
}
