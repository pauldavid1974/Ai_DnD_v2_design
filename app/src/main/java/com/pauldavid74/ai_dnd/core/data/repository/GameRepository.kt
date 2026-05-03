package com.pauldavid74.ai_dnd.core.data.repository

import com.pauldavid74.ai_dnd.core.database.entity.CampaignEntity
import com.pauldavid74.ai_dnd.core.database.entity.CharacterEntity
import com.pauldavid74.ai_dnd.core.database.entity.ChatMessageEntity
import com.pauldavid74.ai_dnd.core.database.entity.FrontEntity
import com.pauldavid74.ai_dnd.core.database.entity.MemoryEntity
import com.pauldavid74.ai_dnd.core.database.entity.ScenarioEdgeEntity
import com.pauldavid74.ai_dnd.core.database.entity.ScenarioNodeEntity
import com.pauldavid74.ai_dnd.core.database.entity.SrdReferenceEntity
import com.pauldavid74.ai_dnd.core.domain.model.CampaignImportPayload
import kotlinx.coroutines.flow.Flow

interface GameRepository {
    // Character
    fun getAllCharacters(): Flow<List<CharacterEntity>>
    suspend fun getCharacter(id: Long): CharacterEntity?
    suspend fun saveCharacter(character: CharacterEntity): Long
    suspend fun updateCharacter(character: CharacterEntity)
    suspend fun deleteCharacter(character: CharacterEntity)

    // Chat
    fun getChatMessages(characterId: Long): Flow<List<ChatMessageEntity>>
    suspend fun saveChatMessage(message: ChatMessageEntity)
    suspend fun deleteChatHistory(characterId: Long)
    suspend fun deleteLastMessage(characterId: Long)

    // Memory
    fun getAllMemories(): Flow<List<MemoryEntity>>
    suspend fun addMemory(memory: MemoryEntity)
    suspend fun getMemory(key: String): MemoryEntity?

    // SRD
    suspend fun getSrdReference(id: String): SrdReferenceEntity?
    suspend fun getSrdReferencesByCategory(category: String): List<SrdReferenceEntity>
    suspend fun seedSrdData(references: List<SrdReferenceEntity>)

    // Scenario / Campaign
    fun getAllCampaigns(): Flow<List<CampaignEntity>>
    suspend fun saveCampaign(campaign: CampaignEntity)
    fun getNodesForCampaign(campaignId: String): Flow<List<ScenarioNodeEntity>>
    fun getEdgesForCampaign(campaignId: String): Flow<List<ScenarioEdgeEntity>>
    suspend fun saveScenarioNodes(nodes: List<ScenarioNodeEntity>)
    suspend fun saveScenarioEdges(edges: List<ScenarioEdgeEntity>)
    fun getFrontsForCampaign(campaignId: String): Flow<List<FrontEntity>>
    suspend fun saveFronts(fronts: List<FrontEntity>)
    
    // Session / Scenario State
    suspend fun initializeScenarioForCharacter(characterId: Long, campaignId: String)
    
    // Bootstrapper
    fun checkIfCampaignExists(id: String): Boolean
    suspend fun importExternalCampaign(payload: CampaignImportPayload)
}
