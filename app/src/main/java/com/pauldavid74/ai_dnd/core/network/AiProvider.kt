package com.pauldavid74.ai_dnd.core.network

import com.pauldavid74.ai_dnd.core.network.model.AiModel
import kotlinx.coroutines.flow.Flow

interface AiProvider {
    val id: String
    val baseUrl: String
    suspend fun getAvailableModels(apiKey: String): List<AiModel>
    suspend fun streamChat(apiKey: String, modelId: String, prompt: String): Flow<String>
}
