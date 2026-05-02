package com.pauldavid74.ai_dnd.core.data.repository

import android.util.Log
import com.pauldavid74.ai_dnd.core.network.AiProvider
import com.pauldavid74.ai_dnd.core.network.model.AiModel
import com.pauldavid74.ai_dnd.core.security.KeyManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject
import javax.inject.Singleton

interface AiProviderRepository {
    suspend fun getAvailableModels(providerId: String): List<AiModel>
    suspend fun streamChat(providerId: String, modelId: String, prompt: String): Flow<String>
    fun hasKey(providerId: String): Boolean
    suspend fun validateKey(providerId: String): Boolean
}

@Singleton
class AiProviderRepositoryImpl @Inject constructor(
    private val providers: Set<@JvmSuppressWildcards AiProvider>,
    private val keyManager: KeyManager
) : AiProviderRepository {

    override suspend fun getAvailableModels(providerId: String): List<AiModel> {
        val provider = providers.find { it.id == providerId }
        val apiKey = keyManager.getApiKey(providerId)
        return if (provider != null && apiKey != null) {
            provider.getAvailableModels(apiKey)
        } else {
            emptyList()
        }
    }

    override suspend fun streamChat(providerId: String, modelId: String, prompt: String): Flow<String> {
        val provider = providers.find { it.id == providerId }
        val apiKey = keyManager.getApiKey(providerId)
        
        Log.d("AiProviderRepository", "streaming chat for $providerId with model $modelId")
        if (apiKey.isNullOrBlank()) {
            Log.e("AiProviderRepository", "No API key found for $providerId!")
        }
        if (provider == null) {
            Log.e("AiProviderRepository", "Provider $providerId not found in set!")
        }

        return if (provider != null && !apiKey.isNullOrBlank()) {
            provider.streamChat(apiKey, modelId, prompt)
        } else {
            emptyFlow()
        }
    }

    override fun hasKey(providerId: String): Boolean {
        return keyManager.getApiKey(providerId) != null
    }

    override suspend fun validateKey(providerId: String): Boolean {
        return try {
            getAvailableModels(providerId).isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}
