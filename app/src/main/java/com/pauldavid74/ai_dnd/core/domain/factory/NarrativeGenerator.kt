package com.pauldavid74.ai_dnd.core.domain.factory

import com.pauldavid74.ai_dnd.core.data.repository.AiProviderRepository
import com.pauldavid74.ai_dnd.core.database.entity.CharacterEntity
import com.pauldavid74.ai_dnd.core.security.KeyManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NarrativeGenerator @Inject constructor(
    private val aiRepository: AiProviderRepository,
    private val keyManager: KeyManager
) {
    suspend fun generateBackstory(character: CharacterEntity): Flow<String> = flow {
        val providerId = keyManager.getActiveProvider() ?: "openai"
        val modelId = keyManager.getActiveModel(providerId) ?: "gpt-4"
        
        val prompt = """
            SYSTEM: You are a creative writer for a D&D 5.2.1 campaign. 
            Finalize the character's journey by providing a grounded, compelling backstory and visual description.
            
            CONTEXT (MATHEMATICAL FOUNDATION FINALIZED):
            - Name: ${character.name}
            - Class: ${character.characterClass}
            - Species: ${character.species}
            - Background: ${character.background}
            - Alignment: ${character.alignment}
            - Stats: STR ${character.strength}, DEX ${character.dexterity}, CON ${character.constitution}, INT ${character.intelligence}, WIS ${character.wisdom}, CHA ${character.charisma}
            - HP: ${character.maxHp}
            
            TASK: 
            1. Write a 2-paragraph backstory that explains how they became a ${character.characterClass}.
            2. Provide a detailed visual description (height, build, clothing, gear).
            
            RULES:
            - DO NOT suggest any mechanical changes.
            - Focus on narrative flavor only.
            - Maintain a tone consistent with the campaign setting.
        """.trimIndent()

        aiRepository.streamChat(providerId, modelId, prompt).collect { chunk ->
            emit(chunk)
        }
    }
}
