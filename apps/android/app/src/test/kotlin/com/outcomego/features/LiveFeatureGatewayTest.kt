package com.outcomego.features

import com.outcomego.mobile.core.HttpMethod
import com.outcomego.android.data.MobileFeatureApi
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveFeatureGatewayTest {
    @Test
    fun richActionsUseTheServerWidgetContract() = runBlocking {
        val client = RecordingFeatureClient()
        val gateway = LiveFeatureGateway(client)

        gateway.submitRichAction("message-1", "approve", mapOf("name" to "Ada")).getOrThrow()

        assertEquals(HttpMethod.POST, client.lastMethod)
        assertEquals("/api/v0/channel-messages/message-1/widget-response", client.lastPath)
        assertTrue(client.lastBody?.get("value")?.jsonPrimitive?.content?.contains("Ada") == true)
        assertTrue(client.lastBody?.get("clientId")?.jsonPrimitive?.content?.isNotBlank() == true)
    }

    @Test
    fun routineListAndTogglePreserveRevisionAndUseScopedRoutes() = runBlocking {
        val client = RecordingFeatureClient(
            buildJsonObject {
                put("value", buildJsonArray {
                    add(buildJsonObject {
                        put("id", "routine-1")
                        put("name", "Morning")
                        put("prompt", "Plan the day")
                        put("schedule", "0 9 * * *")
                        put("timezone", "UTC")
                        put("enabled", true)
                        put("revision", 7)
                    })
                })
            },
        )
        val gateway = LiveFeatureGateway(client)
        val routine = gateway.listRoutines("bot-1").getOrThrow().single()
        assertEquals("routine-1", routine.id)
        assertEquals(7, routine.revision)

        gateway.setRoutineEnabled(routine.id, false, routine.revision).getOrThrow()

        assertEquals("/api/v0/routines/routine-1/pause", client.lastPath)
        assertEquals(7, client.lastBody?.get("expectedRevision")?.jsonPrimitive?.int)
    }

    @Test
    fun webProviderSavePassesSecretRemovalAsNull() = runBlocking {
        val client = RecordingFeatureClient(buildJsonObject {
            put("search", buildJsonObject {
                put("selected", "exa")
                put("providers", buildJsonObject {
                    put("exa", buildJsonObject {
                        put("secretSaved", false)
                        put("ready", false)
                    })
                })
            })
            put("fetch", buildJsonObject { put("selected", JsonNull); put("providers", buildJsonObject { }) })
        })
        val gateway = LiveFeatureGateway(client)

        gateway.saveWebProvider("search", "exa", "", selected = false).getOrThrow()

        assertEquals("/api/v0/server-settings/web-providers", client.lastPath)
        val fields = client.lastBody?.get("search")?.jsonObject?.get("providers")?.jsonObject?.get("exa")?.jsonObject
        assertTrue(fields?.containsKey("apiKey") == true)
        assertTrue(fields?.get("apiKey")?.toString() == "null")
    }

    @Test
    fun attachmentDownloadUsesAuthenticatedBinaryTransport() = runBlocking {
        val client = RecordingFeatureClient()
        val gateway = LiveFeatureGateway(client)

        assertEquals("/api/v0/assets/asset-1", gateway.downloadAttachment("asset-1").getOrThrow())
        assertEquals(HttpMethod.GET, client.lastMethod)
        assertEquals("/api/v0/assets/asset-1", client.lastPath)
        assertTrue(client.downloaded)
    }

    @Test
    fun oauthStartExposesBrowserUrlAndCancellationUsesTheReturnedState() = runBlocking {
        val client = RecordingFeatureClient(
            responsesByPath = mapOf(
                "/api/v0/plugin-connections/connection-1/authenticate" to buildJsonObject {
                    put("authorizationUrl", "https://provider.example/authorize?state=oauth-state")
                },
                "/api/v0/plugins" to buildJsonObject {
                    put("installs", buildJsonArray {
                        add(buildJsonObject {
                            put("pluginKey", "calendar")
                            put("name", "Calendar")
                            put("description", "Calendar provider")
                            put("version", "1.0.0")
                            put("connections", buildJsonArray {
                                add(buildJsonObject {
                                    put("id", "connection-1")
                                    put("name", "Calendar account")
                                    put("auth", "oauth")
                                    put("configured", false)
                                    put("setupPhase", "authorization_pending")
                                })
                            })
                        })
                    })
                },
            ),
        )
        val gateway = LiveFeatureGateway(client)

        val started = gateway.authorizePlugin("connection-1").getOrThrow()

        assertEquals("https://provider.example/authorize?state=oauth-state", started.authorizationUrl)
        assertEquals("oauth-state", started.authorizationState)
        assertEquals("connection-1", started.connectionId)

        gateway.cancelPluginAuthorization("connection-1", started.authorizationState!!).getOrThrow()

        assertEquals("/api/v0/plugin-connections/connection-1/authenticate/cancel", client.lastPath)
        assertEquals("oauth-state", client.lastBody?.get("state")?.jsonPrimitive?.content)
    }

    @Test
    fun assetUploadMapsTheServerAssetRefForMessageDelivery() = runBlocking {
        val client = RecordingFeatureClient(buildJsonObject {
            put("assetId", "a".repeat(64))
            put("fileName", "notes.txt")
            put("mimeType", "text/plain")
            put("byteSize", 42)
            put("kind", "text")
        })

        val attachment = LiveFeatureGateway(client).uploadAttachment("content://share/notes").getOrThrow()

        assertEquals("a".repeat(64), attachment.id)
        assertEquals("notes.txt", attachment.name)
        assertEquals("text", attachment.kind)
        assertEquals(42, attachment.sizeBytes)
    }

    @Test
    fun routineEditorSendsAStableCronSchedule() = runBlocking {
        val client = RecordingFeatureClient()
        val routine = Routine(
            id = "routine-1",
            name = "Weekdays",
            prompt = "Plan the day",
            schedule = RoutineSchedule(
                frequency = RoutineSchedule.Frequency.Weekdays,
                timeMinutes = 9 * 60 + 30,
                timeZone = "Asia/Singapore",
            ),
            revision = 4,
        )

        LiveFeatureGateway(client).saveRoutine("bot-1", routine).getOrThrow()

        assertEquals("/api/v0/routines/routine-1", client.lastPath)
        assertEquals("30 9 * * 1-5", client.lastBody?.get("schedule")?.jsonPrimitive?.content)
        assertEquals(4, client.lastBody?.get("expectedRevision")?.jsonPrimitive?.int)
    }

    @Test
    fun settingsLoadAndProviderDisconnectUseDedicatedServerOperations() = runBlocking {
        val client = RecordingFeatureClient(
            responsesByPath = mapOf(
                "/api/v0/time-zone" to buildJsonObject { put("timeZone", "Asia/Singapore") },
                "/api/v0/notification-preferences" to buildJsonObject { put("smartEnabled", false) },
            ),
        )
        val gateway = LiveFeatureGateway(client)

        val settings = gateway.loadSettings().getOrThrow()
        assertEquals("Asia/Singapore", settings.timeZone)
        assertTrue(!settings.notifications.messages)
        gateway.disconnectProvider("openai").getOrThrow()
        assertEquals("/api/v0/inference-providers/openai", client.lastPath)
        assertEquals(HttpMethod.DELETE, client.lastMethod)
    }

    @Test
    fun settingsLoadAndSaveUseServerTimeZoneOverrideAndDetectedContract() = runBlocking {
        val client = RecordingFeatureClient(
            responsesByPath = mapOf(
                "/api/v0/time-zone" to buildJsonObject {
                    put("detectedTimeZone", "America/Los_Angeles")
                    put("overrideTimeZone", JsonNull)
                    put("timeZone", "America/Los_Angeles")
                },
                "/api/v0/notification-preferences" to buildJsonObject { put("smartEnabled", true) },
            ),
        )
        val gateway = LiveFeatureGateway(client)

        val loaded = gateway.loadSettings().getOrThrow()
        assertEquals("America/Los_Angeles", loaded.timeZone)
        assertEquals("America/Los_Angeles", loaded.detectedTimeZone)
        assertEquals(null, loaded.overrideTimeZone)

        gateway.saveSettings(loaded.copy(timeZone = "Asia/Tokyo", overrideTimeZone = "Asia/Tokyo")).getOrThrow()
        val patchBody = client.bodiesByPath["/api/v0/time-zone"]
        assertEquals("Asia/Tokyo", patchBody?.get("overrideTimeZone")?.jsonPrimitive?.contentOrNull)
        assertEquals("America/Los_Angeles", patchBody?.get("detectedTimeZone")?.jsonPrimitive?.contentOrNull)

        gateway.saveSettings(loaded.copy(timeZone = "America/Los_Angeles", overrideTimeZone = null)).getOrThrow()
        val autoPatchBody = client.bodiesByPath["/api/v0/time-zone"]
        assertTrue(autoPatchBody?.get("overrideTimeZone") is JsonNull)
    }

    @Test
    fun loadRoutinesParsesCronTzPrefixFromServerSchedule() = runBlocking {
        val client = RecordingFeatureClient(
            response = buildJsonObject {
                put(
                    "routines",
                    buildJsonArray {
                        add(
                            buildJsonObject {
                                put("id", "r1")
                                put("title", "Standup")
                                put("prompt", "Summarize")
                                put("schedule", "CRON_TZ=Asia/Tokyo 30 9 * * 1-5")
                                put("timeZone", "UTC")
                            },
                        )
                    },
                )
            },
        )
        val gateway = LiveFeatureGateway(client)

        val routines = gateway.listRoutines("bot-1").getOrThrow()
        assertEquals(1, routines.size)
        assertEquals("Asia/Tokyo", routines.first().schedule.timeZone)
        assertEquals(RoutineSchedule.Frequency.Weekdays, routines.first().schedule.frequency)
        assertEquals(9 * 60 + 30, routines.first().schedule.timeMinutes)
    }

    @Test
    fun oauthCallbackUsesTheManualServerCompletionContract() = runBlocking {
        val client = RecordingFeatureClient()

        LiveFeatureGateway(client).completePluginAuthorization(
            "connection-1",
            "outcomego://oauth/callback?code=abc&state=oauth-state",
        ).getOrThrow()

        assertEquals("/api/v0/plugin-connections/connection-1/authenticate/manual", client.lastPath)
        assertEquals("outcomego://oauth/callback?code=abc&state=oauth-state", client.lastBody?.get("callbackUrl")?.jsonPrimitive?.content)
    }

    private fun providerSession(status: String, promptId: String? = null, error: String? = null) = buildJsonObject {
        put("id", "session-1")
        put("status", status)
        if (promptId != null) put("prompt", buildJsonObject { put("id", promptId); put("type", "secret") })
        if (error != null) put("error", error)
    }

    private val providerFlow = mapOf(
        "/api/v0/inference-providers" to buildJsonObject { put("providerId", "deepseek-api") },
        "/api/v0/inference-providers/deepseek-api/auth-sessions" to providerSession("waiting", "prompt-1"),
    )

    @Test
    fun providerPresetsAreReadFromTheWrappedArray() = runBlocking {
        val client = RecordingFeatureClient(
            responsesByPath = mapOf("/api/v0/inference-provider-presets" to buildJsonObject {
                put("value", buildJsonArray {
                    add(buildJsonObject {
                        put("id", "deepseek-api")
                        put("name", "DeepSeek")
                        put("group", "vendor")
                        put("endpoints", buildJsonObject {
                            put("openai-completions", "https://api.deepseek.com/v1")
                            put("anthropic-messages", "https://api.deepseek.com/anthropic")
                        })
                        put("keysUrl", "https://platform.deepseek.com/api_keys")
                    })
                    add(buildJsonObject {
                        put("id", "ollama-local"); put("name", "Ollama"); put("group", "local"); put("noKey", true); put("keyOptional", false)
                        put("endpoints", buildJsonObject { put("openai-completions", "http://localhost:11434/v1") })
                    })
                })
            }),
        )

        val presets = LiveFeatureGateway(client).loadProviderPresets().getOrThrow()

        assertEquals(listOf("deepseek-api", "ollama-local"), presets.map { it.id })
        assertEquals(listOf(ProviderProtocol.OpenAiChat, ProviderProtocol.AnthropicMessages), presets[0].protocols)
        assertEquals("https://api.deepseek.com/anthropic", presets[0].baseUrl(ProviderProtocol.AnthropicMessages))
        assertTrue(presets[1].noKey)
        assertEquals(false, presets[1].keyOptional)
    }

    @Test
    fun aRemoteMagpieIsAddedWithItsKeyAndALocalOneWithout() = runBlocking {
        val withKey = RecordingFeatureClient(responsesByPath = providerFlow + mapOf("/api/v0/inference-provider-auth-sessions/session-1/respond" to providerSession("connected")))
        val remote = ProviderPreset("deepseek-api", "magpie", "local", mapOf("openai-completions" to "http://127.0.0.1:3425/v1"), keyOptional = true)
        LiveFeatureGateway(withKey).addProvider(remote, "magpie", "http://10.0.0.8:3425/v1", ProviderProtocol.OpenAiChat, "sk-magpie-key-1").getOrThrow()
        assertEquals("sk-magpie-key-1", withKey.lastBody?.get("value")?.jsonPrimitive?.content)

        val keyless = RecordingFeatureClient(responsesByPath = providerFlow)
        LiveFeatureGateway(keyless).addProvider(remote, "magpie", "http://127.0.0.1:3425/v1", ProviderProtocol.OpenAiChat, null).getOrThrow()
        assertTrue(keyless.requests.none { it.second.contains("auth-sessions") })
    }

    @Test
    fun addingAProviderSendsTheEndpointThenTheKeyAndKeepsItWhenAccepted() = runBlocking {
        val client = RecordingFeatureClient(
            responsesByPath = providerFlow + mapOf(
                "/api/v0/inference-provider-auth-sessions/session-1/respond" to providerSession("connected"),
            ),
        )
        val preset = ProviderPreset("deepseek-api", "DeepSeek", "vendor", mapOf("openai-completions" to "https://api.deepseek.com/v1"))

        val id = LiveFeatureGateway(client).addProvider(preset, "DeepSeek", "https://api.deepseek.com/v1", ProviderProtocol.OpenAiChat, "sk-good").getOrThrow()

        assertEquals("deepseek-api", id)
        assertEquals(HttpMethod.POST to "/api/v0/inference-providers", client.requests.first())
        assertTrue(client.requests.none { it.first == HttpMethod.DELETE })
        assertEquals("sk-good", client.lastBody?.get("value")?.jsonPrimitive?.content)
    }

    @Test
    fun aRefusedKeyRemovesTheProviderAgain() = runBlocking {
        val client = RecordingFeatureClient(
            responsesByPath = providerFlow + mapOf(
                "/api/v0/inference-provider-auth-sessions/session-1/respond" to providerSession("failed", error = "That key was refused."),
            ),
        )

        val result = LiveFeatureGateway(client).addProvider(null, "Mine", "https://x.test/v1", ProviderProtocol.OpenAiChat, "sk-bad")

        assertTrue(result.isFailure)
        assertEquals("That key was refused.", result.exceptionOrNull()?.message)
        assertEquals(HttpMethod.DELETE to "/api/v0/inference-custom-providers/deepseek-api", client.requests.last())
    }

    private class RecordingFeatureClient(
        private val response: JsonObject = JsonObject(emptyMap()),
        private val responsesByPath: Map<String, JsonObject> = emptyMap(),
    ) : MobileFeatureApi {
        var lastMethod: HttpMethod? = null
        var lastPath: String? = null
        var lastBody: JsonObject? = null
        var downloaded = false
        val requests = mutableListOf<Pair<HttpMethod, String>>()
        val bodiesByPath = mutableMapOf<String, JsonObject?>()

        override suspend fun request(method: HttpMethod, path: String, body: JsonObject?): JsonObject {
            requests += method to path
            bodiesByPath[path] = body
            lastMethod = method
            lastPath = path
            lastBody = body
            return responsesByPath[path] ?: response
        }

        override suspend fun upload(path: String, localUri: String): JsonObject = response

        override suspend fun download(path: String): String {
            downloaded = true
            lastMethod = HttpMethod.GET
            lastPath = path
            return path
        }
    }
}
