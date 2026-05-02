package com.pauldavid74.ai_dnd.core.domain.factory

import com.pauldavid74.ai_dnd.core.data.repository.AiProviderRepository
import com.pauldavid74.ai_dnd.core.data.repository.GameRepository
import com.pauldavid74.ai_dnd.core.database.entity.MemoryEntity
import com.pauldavid74.ai_dnd.core.network.model.ChroniclerResponse
import com.pauldavid74.ai_dnd.feature.game.ChatMessage
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Chronicler @Inject constructor(
    private val gameRepository: GameRepository,
    private val aiRepository: AiProviderRepository,
    private val json: Json
) {
    private val providerId = "openai"
    private val modelId = "gpt-4"

    suspend fun chronicleSession(chatHistory: List<ChatMessage>) {
        val transcript = chatHistory.joinToString("\n") { "${it.sender}: ${it.content}" }
        
        val prompt = """
            SYSTEM: You are the Chronicler. Summarize the following D&D session transcript and update the memory database.
            
            TRANSCRIPT:
            $transcript
            
            TASK: 
            1. session_summary: A concise, 1-2 sentence overview of the latest events.
            2. memory_updates: Identify any new NPCs, locations, or major plot facts revealed.
            
            JSON SCHEMA:
            {
              "session_summary": "...",
              "memory_updates": [
                { "entityId": "name", "newFact": "..." }
              ]
            }
            
            IMPORTANT: Output ONLY valid JSON.
        """.trimIndent()

        var responseJson = ""
        aiRepository.streamChat(providerId, modelId, prompt).collect { chunk ->
            responseJson += chunk
        }

        try {
            val result = json.decodeFromString<ChroniclerResponse>(responseJson)
            
            // Persist summary
            val summaryKey = "summary_${System.currentTimeMillis()}"
            gameRepository.addMemory(MemoryEntity(
                type = "PLOT_SUMMARY",
                key = summaryKey,
                content = result.sessionSummary
            ))

            // Persist updates
            result.memoryUpdates.forEach { update ->
                gameRepository.addMemory(MemoryEntity(
                    type = "FACT",
                    key = update.entityId,
                    content = update.newFact ?: update.stateChange ?: ""
                ))
            }
        } catch (e: Exception) {
            // Handle error
        }
    }
}
