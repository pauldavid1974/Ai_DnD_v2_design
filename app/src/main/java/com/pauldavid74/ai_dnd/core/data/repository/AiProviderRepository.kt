package com.pauldavid74.ai_dnd.core.data.repository

import android.util.Log
import com.pauldavid74.ai_dnd.core.network.AiProvider
import com.pauldavid74.ai_dnd.core.network.GenericOpenAiProvider
import com.pauldavid74.ai_dnd.core.network.model.AiModel
import com.pauldavid74.ai_dnd.core.network.model.LLMProvider
import com.pauldavid74.ai_dnd.core.security.KeyManager
import io.ktor.client.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.serialization.json.Json
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
    private val keyManager: KeyManager,
    private val httpClient: HttpClient,
    private val json: Json
) : AiProviderRepository {

    override suspend fun getAvailableModels(providerId: String): List<AiModel> {
        val provider = getProvider(providerId)
        val apiKey = keyManager.getApiKey(providerId)
        return if (provider != null && apiKey != null) {
            provider.getAvailableModels(apiKey)
        } else {
            emptyList()
        }
    }

    override suspend fun streamChat(providerId: String, modelId: String, prompt: String): Flow<String> {
        val provider = getProvider(providerId)
        val apiKey = keyManager.getApiKey(providerId)
        
        Log.d("AiProviderRepository", "streaming chat for $providerId with model $modelId")
        
        return if (provider != null && !apiKey.isNullOrBlank()) {
            provider.streamChat(apiKey, modelId, prompt)
        } else {
            emptyFlow()
        }
    }

    private fun getProvider(providerId: String): AiProvider? {
        val registered = providers.find { it.id == providerId }
        if (registered != null) return registered

        val config = LLMProvider.ALL_PROVIDERS.find { it.id == providerId }
        val baseUrl = keyManager.getCustomBaseUrl(providerId) ?: config?.baseUrl
        
        return if (!baseUrl.isNullOrBlank()) {
            GenericOpenAiProvider(providerId, baseUrl, httpClient, json)
        } else null
    }

    override fun hasKey(providerId: String): Boolean {
        return keyManager.getApiKey(providerId) != null
    }

    override suspend fun validateKey(providerId: String): Boolean {
        return try {
            val models = getAvailableModels(providerId)
            models.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}
