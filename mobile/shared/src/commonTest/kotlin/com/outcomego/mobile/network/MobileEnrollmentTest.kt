package com.outcomego.mobile.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MobileEnrollmentTest {
    @Test
    fun enrollmentUsesSessionThenMachineCredential() = kotlinx.coroutines.test.runTest {
        val requests = mutableListOf<Pair<String, Map<String, String>>>()
        val client = MobileEnrollmentClient(
            object : ProviderHttpClient {
                override suspend fun request(
                    serverUrl: String,
                    path: String,
                    method: String,
                    headers: Map<String, String>,
                    body: String?,
                ): ProviderHttpResponse {
                    requests += path to headers
                    return when (path) {
                        "/api/v0/machines/enroll" ->
                            ProviderHttpResponse(200, """{"machineId":"11111111-1111-1111-1111-111111111111","credential":"credential"}""")
                        "/api/v0/machines/channel/connect" ->
                            ProviderHttpResponse(200, """{"connectionId":"connection"}""")
                        else ->
                            ProviderHttpResponse(200, """{"deploymentId":"22222222-2222-2222-2222-222222222222"}""")
                    }
                }
            },
        )
        val enrollment = client.enroll(
            "https://server.example",
            "session",
            "11111111-1111-1111-1111-111111111111",
            "Phone",
        )
        assertEquals("credential", enrollment.credential)
        assertEquals("connection", enrollment.connectionId)
        assertEquals("22222222-2222-2222-2222-222222222222", enrollment.deploymentId)
        assertEquals("Bearer session", requests[0].second["Authorization"])
        assertEquals("Bearer credential", requests[1].second["Authorization"])
    }

    @Test
    fun enrollmentRejectsDifferentMachineIdentity() = kotlinx.coroutines.test.runTest {
        val client = MobileEnrollmentClient(
            object : ProviderHttpClient {
                override suspend fun request(
                    serverUrl: String,
                    path: String,
                    method: String,
                    headers: Map<String, String>,
                    body: String?,
                ) = ProviderHttpResponse(
                    200,
                    """{"machineId":"33333333-3333-3333-3333-333333333333","credential":"credential"}""",
                )
            },
        )
        assertFailsWith<NetworkProtocolException> {
            client.enroll(
                "https://server.example",
                "session",
                "11111111-1111-1111-1111-111111111111",
                "Phone",
            )
        }
    }
}
