package com.outcomego.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AndroidUpdateManifestTest {
    private val validManifest = AndroidUpdateManifest(
        version = "0.1.0-beta.14",
        versionCode = 10014,
        applicationId = "com.outcomego.android",
        apkUrl = "https://github.com/jingyi0605/OutcomeGo/releases/download/v0.1.0-beta.14/OutcomeGo-0.1.0-beta.14-android.apk",
        sizeBytes = 24943411,
        sha256 = "ce9870dc18b67e6788456a57441ca97681db133f72901c66f9894c9e82a4dcf7",
        certificateSha256 = "6973a9c2170428745f295fb7304cbfe03878e93fd9f58907f0540d96086ed629",
        signatureBase64 = "MEUCICZdss6odTMF7SbcR8qzkpQGrEOwAGlp7c4B5ODbSn20AiEAxsYkonBKnbil2l9hI2tRRVNYIIZ5SASBWGVkWYH3VTM=",
    )

    @Test
    fun canonicalPayloadIsStableAndUsesLowercaseDigests() {
        assertEquals(
            "version=0.1.0-beta.14\n" +
                "versionCode=10014\n" +
                "applicationId=com.outcomego.android\n" +
                "apkUrl=https://github.com/jingyi0605/OutcomeGo/releases/download/v0.1.0-beta.14/OutcomeGo-0.1.0-beta.14-android.apk\n" +
                "sizeBytes=24943411\n" +
                "sha256=ce9870dc18b67e6788456a57441ca97681db133f72901c66f9894c9e82a4dcf7\n" +
                "certificateSha256=6973a9c2170428745f295fb7304cbfe03878e93fd9f58907f0540d96086ed629\n",
            AndroidUpdateManifestVerifier.canonicalPayload(validManifest),
        )
    }

    @Test
    fun validSignedManifestIsAccepted() {
        val parsed = AndroidUpdateManifestVerifier.parseAndVerify(
            Json.encodeToString(validManifest),
            "com.outcomego.android",
        )
        assertEquals(validManifest, parsed)
    }

    @Test
    fun tamperedPayloadIsRejected() {
        val tampered = validManifest.copy(version = "1.2.4")
        assertThrows(IllegalArgumentException::class.java) {
            AndroidUpdateManifestVerifier.parseAndVerify(
                Json.encodeToString(tampered),
                "com.outcomego.android",
            )
        }
    }

    @Test
    fun invalidTransportVersionAndChecksumAreRejected() {
        val invalidUrl = validManifest.copy(apkUrl = "http://example.com/update.apk")
        assertThrows(IllegalArgumentException::class.java) {
            AndroidUpdateManifestVerifier.parseAndVerify(
                Json.encodeToString(invalidUrl),
                "com.outcomego.android",
            )
        }
        val invalidVersion = validManifest.copy(version = "1.2")
        assertThrows(IllegalArgumentException::class.java) {
            AndroidUpdateManifestVerifier.parseAndVerify(
                Json.encodeToString(invalidVersion),
                "com.outcomego.android",
            )
        }
        val invalidChecksum = validManifest.copy(sha256 = "not-a-checksum")
        assertThrows(IllegalArgumentException::class.java) {
            AndroidUpdateManifestVerifier.parseAndVerify(
                Json.encodeToString(invalidChecksum),
                "com.outcomego.android",
            )
        }
    }
}
