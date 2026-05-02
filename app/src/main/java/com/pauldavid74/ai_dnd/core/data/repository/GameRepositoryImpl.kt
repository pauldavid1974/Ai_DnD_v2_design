package com.pauldavid74.ai_dnd.core.data.repository

import com.pauldavid74.ai_dnd.core.database.dao.CampaignDao
import com.pauldavid74.ai_dnd.core.database.dao.CharacterDao
import com.pauldavid74.ai_dnd.core.database.dao.MemoryDao
import com.pauldavid74.ai_dnd.core.database.dao.ScenarioDao
import com.pauldavid74.ai_dnd.core.database.dao.SrdReferenceDao
import com.pauldavid74.ai_dnd.core.database.entity.CampaignEntity
import com.pauldavid74.ai_dnd.core.database.entity.CharacterEntity
import com.pauldavid74.ai_dnd.core.database.entity.FrontEntity
import com.pauldavid74.ai_dnd.core.database.entity.MemoryEntity
import com.pauldavid74.ai_dnd.core.database.entity.ScenarioEdgeEntity
import com.pauldavid74.ai_dnd.core.database.entity.ScenarioNodeEntity
import com.pauldavid74.ai_dnd.core.database.entity.SrdReferenceEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameRepositoryImpl @Inject constructor(
    private val characterDao: CharacterDao,
    private val memoryDao: MemoryDao,
    private val srdReferenceDao: SrdReferenceDao,
    private val scenarioDao: ScenarioDao,
    private val campaignDao: CampaignDao
) : GameRepository {

    override fun getAllCharacters(): Flow<List<CharacterEntity>> = characterDao.getAllCharacters()

    override suspend fun getCharacter(id: Long): CharacterEntity? = characterDao.getCharacterById(id)

    override suspend fun saveCharacter(character: CharacterEntity): Long = characterDao.insertCharacter(character)

    override suspend fun updateCharacter(character: CharacterEntity) = characterDao.updateCharacter(character)

    override suspend fun deleteCharacter(character: CharacterEntity) = characterDao.deleteCharacter(character)

    override fun getAllMemories(): Flow<List<MemoryEntity>> = memoryDao.getAllMemories()

    override suspend fun addMemory(memory: MemoryEntity) = memoryDao.upsertMemory(memory)

    override suspend fun getMemory(key: String): MemoryEntity? = memoryDao.getMemoryByKey(key)

    override suspend fun getSrdReference(id: String): SrdReferenceEntity? = srdReferenceDao.getReferenceById(id)

    override suspend fun getSrdReferencesByCategory(category: String): List<SrdReferenceEntity> = 
        srdReferenceDao.getReferencesByCategory(category)

    override suspend fun seedSrdData(references: List<SrdReferenceEntity>) = srdReferenceDao.insertReferences(references)

    // Scenario / Campaign
    override fun getAllCampaigns(): Flow<List<CampaignEntity>> = campaignDao.getAllCampaigns()

    override suspend fun saveCampaign(campaign: CampaignEntity) = campaignDao.insertCampaign(campaign)

    override fun getNodesForCampaign(campaignId: String): Flow<List<ScenarioNodeEntity>> = 
        scenarioDao.getNodesForCampaign(campaignId)

    override fun getEdgesForCampaign(campaignId: String): Flow<List<ScenarioEdgeEntity>> = 
        scenarioDao.getEdgesForCampaign(campaignId)

    override suspend fun saveScenarioNodes(nodes: List<ScenarioNodeEntity>) = scenarioDao.insertNodes(nodes)

    override suspend fun saveScenarioEdges(edges: List<ScenarioEdgeEntity>) = scenarioDao.insertEdges(edges)

    override fun getFrontsForCampaign(campaignId: String): Flow<List<FrontEntity>> = 
        campaignDao.getFrontsForCampaign(campaignId)

    override suspend fun saveFronts(fronts: List<FrontEntity>) = campaignDao.insertFronts(fronts)
}
