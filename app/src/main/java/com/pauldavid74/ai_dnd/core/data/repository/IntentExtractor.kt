package com.pauldavid74.ai_dnd.core.data.repository

import android.util.Log
import com.pauldavid74.ai_dnd.core.database.entity.CharacterEntity
import com.pauldavid74.ai_dnd.core.database.entity.MemoryEntity
import com.pauldavid74.ai_dnd.core.domain.factory.PromptFactory
import com.pauldavid74.ai_dnd.core.network.model.PlayerIntent
import com.pauldavid74.ai_dnd.core.security.KeyManager
import com.pauldavid74.ai_dnd.feature.game.ChatMessage
import com.pauldavid74.ai_dnd.feature.game.MessageSender
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

class IntentExtractionException(message: String) : Exception(message)

interface IntentExtractor {
    suspend fun extractIntent(
        playerText: String,
        character: CharacterEntity,
        chatHistory: List<ChatMessage>,
        memories: List<MemoryEntity> = emptyList()
    ): Flow<PlayerIntent>
}

@Singleton
class IntentExtractorImpl @Inject constructor(
    private val aiRepository: AiProviderRepository,
    private val keyManager: KeyManager,
    private val promptFactory: PromptFactory,
    private val json: Json
) : IntentExtractor {

    private val jsonConfig = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    override suspend fun extractIntent(
        playerText: String,
        character: CharacterEntity,
        chatHistory: List<ChatMessage>,
        memories: List<MemoryEntity>
    ): Flow<PlayerIntent> = flow {
        val providerId = keyManager.getActiveProvider() ?: "openai"
        val modelId = keyManager.getActiveModel(providerId) ?: "gpt-4"
        
        val historyPairs = chatHistory.filter { it.sender != MessageSender.SYSTEM }
            .map { (if (it.sender == MessageSender.USER) "User" else "AI") to it.content }

        val prompt = promptFactory.createIntentPrompt(
            character = character,
            lastSummary = null,
            chatHistory = historyPairs,
            relevantMemories = memories,
            activeFronts = emptyList(),
            userInput = playerText
        )

        val buffer = StringBuilder()
        try {
            aiRepository.streamChat(providerId, modelId, prompt).collect { chunk ->
                buffer.append(chunk)
                
                val currentRaw = buffer.toString().trim()
                val sanitized = sanitizeJson(currentRaw)
                
                if (sanitized.startsWith("{") && sanitized.endsWith("}")) {
                    try {
                        val intent = jsonConfig.decodeFromString<PlayerIntent>(sanitized)
                        
                        if (intent.impossibilityScore > 85) {
                            throw IntentExtractionException("Action is mechanically impossible (Score: ${intent.impossibilityScore})")
                        }
                        
                        emit(intent)
                        return@collect
                    } catch (e: Exception) {
                        if (e is IntentExtractionException) throw e
                        // Not a complete/valid JSON yet, continue buffering
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("IntentExtractor", "Phase A Intent extraction failed: ${e.message}", e)
            throw e
        }
    }

    private fun sanitizeJson(raw: String): String {
        val trimmed = raw.trim()
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        
        return if (start != -1 && end != -1 && end > start) {
            trimmed.substring(start, end + 1)
        } else {
            trimmed
        }
    }
}
