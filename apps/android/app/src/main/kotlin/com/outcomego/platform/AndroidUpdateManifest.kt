package com.outcomego.platform

import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.Locale
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class AndroidUpdateManifest(
    val version: String,
    val versionCode: Long,
    val applicationId: String,
    val apkUrl: String,
    val sizeBytes: Long,
    val sha256: String,
    val certificateSha256: String,
    val signatureBase64: String,
)

object AndroidUpdateManifestVerifier {
    private const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
    private const val PUBLIC_KEY_DER_HEX =
        "3059301306072a8648ce3d020106082a8648ce3d030107034200040bfa9b6a881b16bf2ac52482ef6a246588adfbabc17bb474d8f384a345bbbdeb39f51ae61ad3bd09592242e185e7c8b8d1e7c22e3df1631bcbfa2ca237ff25ca"
    private val json = Json { ignoreUnknownKeys = false }
    private val sha256Pattern = Regex("^[0-9a-fA-F]{64}$")
    private val versionPattern = Regex("^\\d+\\.\\d+\\.\\d+(?:-[A-Za-z]+\\.\\d+)?$")

    fun parseAndVerify(raw: String, expectedApplicationId: String): AndroidUpdateManifest {
        val manifest = json.decodeFromString<AndroidUpdateManifest>(raw)
        require(manifest.applicationId == expectedApplicationId) {
            "The update targets a different Android application."
        }
        require(versionPattern.matches(manifest.version)) {
            "The update manifest contains an invalid version."
        }
        require(manifest.versionCode > 0) {
            "The update manifest contains an invalid version code."
        }
        require(manifest.sizeBytes > 0) {
            "The update manifest contains an invalid APK size."
        }
        require(sha256Pattern.matches(manifest.sha256)) {
            "The update manifest contains an invalid APK checksum."
        }
        require(sha256Pattern.matches(manifest.certificateSha256)) {
            "The update manifest contains an invalid certificate checksum."
        }
        val url = java.net.URI(manifest.apkUrl)
        require(url.scheme.equals("https", ignoreCase = true)) {
            "The update APK must be downloaded over HTTPS."
        }
        require(manifest.signatureBase64.isNotBlank()) {
            "The update manifest is unsigned."
        }
        require(verifySignature(manifest)) {
            "The update manifest signature is invalid."
        }
        return manifest
    }

    fun canonicalPayload(manifest: AndroidUpdateManifest): String = buildString {
        append("version=").append(manifest.version).append('\n')
        append("versionCode=").append(manifest.versionCode).append('\n')
        append("applicationId=").append(manifest.applicationId).append('\n')
        append("apkUrl=").append(manifest.apkUrl).append('\n')
        append("sizeBytes=").append(manifest.sizeBytes).append('\n')
        append("sha256=").append(manifest.sha256.lowercase(Locale.US)).append('\n')
        append("certificateSha256=").append(manifest.certificateSha256.lowercase(Locale.US)).append('\n')
    }

    private fun verifySignature(manifest: AndroidUpdateManifest): Boolean = runCatching {
        val keyFactory = KeyFactory.getInstance("EC")
        val publicKey: PublicKey = keyFactory.generatePublic(
            X509EncodedKeySpec(PUBLIC_KEY_DER_HEX.chunked(2).map { it.toInt(16).toByte() }.toByteArray()),
        )
        Signature.getInstance(SIGNATURE_ALGORITHM).run {
            initVerify(publicKey)
            update(canonicalPayload(manifest).toByteArray(Charsets.UTF_8))
            verify(Base64.getDecoder().decode(manifest.signatureBase64))
        }
    }.getOrDefault(false)
}
