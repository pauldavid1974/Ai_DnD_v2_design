package com.pauldavid74.ai_dnd.core.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeyManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = try {
        createSharedPrefs(context)
    } catch (e: Exception) {
        // Decryption failed (e.g. AEADBadTagException). Corrupted Keystore or master key.
        // Wipe preferences and recreate to stop crash. User will need to re-enter API keys.
        context.getSharedPreferences("secure_keys", Context.MODE_PRIVATE).edit().clear().apply()
        createSharedPrefs(context)
    }

    private fun createSharedPrefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        "secure_keys",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /**
     * Saves an API key for a specific provider (e.g., "openai", "anthropic").
     */
    fun saveApiKey(provider: String, key: String) {
        sharedPreferences.edit().putString(provider, key).apply()
    }

    /**
     * Retrieves the API key for a specific provider.
     */
    fun getApiKey(provider: String): String? {
        return sharedPreferences.getString(provider, null)
    }

    /**
     * Saves the currently selected provider ID.
     */
    fun saveActiveProvider(providerId: String) {
        sharedPreferences.edit().putString(KEY_ACTIVE_PROVIDER, providerId).apply()
    }

    /**
     * Retrieves the currently selected provider ID.
     */
    fun getActiveProvider(): String {
        return sharedPreferences.getString(KEY_ACTIVE_PROVIDER, PROVIDER_OPENAI) ?: PROVIDER_OPENAI
    }

    /**
     * Saves the currently selected model ID for a specific provider.
     */
    fun saveActiveModel(providerId: String, modelId: String) {
        sharedPreferences.edit().putString("${KEY_ACTIVE_MODEL_PREFIX}_$providerId", modelId).apply()
    }

    /**
     * Retrieves the currently selected model ID for a specific provider.
     */
    fun getActiveModel(providerId: String): String? {
        return sharedPreferences.getString("${KEY_ACTIVE_MODEL_PREFIX}_$providerId", null)
    }

    /**
     * Deletes the API key for a specific provider.
     */
    fun deleteApiKey(provider: String) {
        sharedPreferences.edit().remove(provider).apply()
    }

    companion object {
        const val PROVIDER_OPENAI = "openai"
        const val PROVIDER_ANTHROPIC = "anthropic"
        const val PROVIDER_GROQ = "groq"

        private const val KEY_ACTIVE_PROVIDER = "active_provider"
        private const val KEY_ACTIVE_MODEL_PREFIX = "active_model"
    }
}
