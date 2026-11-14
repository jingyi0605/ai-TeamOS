package com.outcomego.android.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.outcomego.mobile.network.ProviderConsentStore
import com.outcomego.mobile.network.ProviderCredentialStore
import java.security.MessageDigest
import java.util.UUID

class AndroidProviderStore(context: Context) : ProviderCredentialStore, ProviderConsentStore {
    private val preferences = EncryptedSharedPreferences.create(
        context,
        "mobile-network-provider",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    val machineId: String
        get() = preferences.getString(MACHINE_ID, null)
            ?: UUID.randomUUID().toString().also {
                preferences.edit().putString(MACHINE_ID, it).apply()
            }

    override suspend fun read(serverUrl: String, machineId: String): String? =
        preferences.getString(credentialKey(serverUrl, machineId), null)

    override suspend fun write(serverUrl: String, machineId: String, credential: String) {
        preferences.edit().putString(credentialKey(serverUrl, machineId), credential).apply()
    }

    override suspend fun clear(serverUrl: String, machineId: String) {
        preferences.edit().remove(credentialKey(serverUrl, machineId)).apply()
    }

    override suspend fun isAllowed(serverUrl: String, deploymentId: String): Boolean =
        preferences.getBoolean(consentKey(serverUrl, deploymentId), false)

    override suspend fun setAllowed(serverUrl: String, deploymentId: String, allowed: Boolean) {
        preferences.edit().putBoolean(consentKey(serverUrl, deploymentId), allowed).apply()
    }

    private fun credentialKey(serverUrl: String, machineId: String) =
        "credential:${digest("$serverUrl|$machineId")}"

    private fun consentKey(serverUrl: String, deploymentId: String) =
        "consent:${digest("$serverUrl|$deploymentId")}"

    private fun digest(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private companion object {
        const val MACHINE_ID = "machine-id"
    }
}
