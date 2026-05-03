package com.pauldavid74.ai_dnd.core.data.repository

import android.util.Log
import com.pauldavid74.ai_dnd.core.database.dao.CampaignDao
import com.pauldavid74.ai_dnd.core.database.dao.CharacterDao
import com.pauldavid74.ai_dnd.core.database.dao.ChatMessageDao
import com.pauldavid74.ai_dnd.core.database.dao.MemoryDao
import com.pauldavid74.ai_dnd.core.database.dao.ScenarioDao
import com.pauldavid74.ai_dnd.core.database.dao.SrdReferenceDao
import com.pauldavid74.ai_dnd.core.database.entity.CampaignEntity
import com.pauldavid74.ai_dnd.core.database.entity.CharacterEntity
import com.pauldavid74.ai_dnd.core.database.entity.ChatMessageEntity
import com.pauldavid74.ai_dnd.core.database.entity.FrontEntity
import com.pauldavid74.ai_dnd.core.database.entity.MemoryEntity
import com.pauldavid74.ai_dnd.core.database.entity.ScenarioEdgeEntity
import com.pauldavid74.ai_dnd.core.database.entity.ScenarioNodeEntity
import com.pauldavid74.ai_dnd.core.database.entity.SrdReferenceEntity
import com.pauldavid74.ai_dnd.core.domain.model.Clue
import com.pauldavid74.ai_dnd.core.domain.model.CampaignImportPayload
import com.pauldavid74.ai_dnd.core.domain.model.NodeType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameRepositoryImpl @Inject constructor(
    private val characterDao: CharacterDao,
    private val chatMessageDao: ChatMessageDao,
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

    override fun getChatMessages(characterId: Long): Flow<List<ChatMessageEntity>> = 
        chatMessageDao.getMessagesForCharacter(characterId)

    override suspend fun saveChatMessage(message: ChatMessageEntity) = 
        chatMessageDao.insertMessage(message)

    override suspend fun deleteChatHistory(characterId: Long) = 
        chatMessageDao.deleteMessagesForCharacter(characterId)

    override suspend fun deleteLastMessage(characterId: Long) =
        chatMessageDao.deleteLastMessage(characterId)

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

    override suspend fun initializeScenarioForCharacter(characterId: Long, campaignId: String) {
        Log.i("GameRepository", "Initializing scenario graph for character $characterId in campaign $campaignId (Session Zero)")
        // In a real implementation, this might involve setting the current node to the campaign's starting node,
        // or seeding initial memories/messages. For now, we'll treat the campaign data as a static graph
        // and assume the UI/DM logic will navigate it starting from nodes associated with the campaignId.
    }

    // Bootstrapper methods
    override fun checkIfCampaignExists(id: String): Boolean = campaignDao.existsById(id)

    override suspend fun importExternalCampaign(payload: CampaignImportPayload) {
        val campaignId = payload.campaign_metadata.id
        if (checkIfCampaignExists(campaignId)) {
            Log.d("CampaignBootstrapper", "Campaign $campaignId already exists. Skipping import.")
            return
        }

        Log.i("CampaignBootstrapper", "Importing campaign: $campaignId")

        // Create and save campaign
        val campaignEntity = CampaignEntity(
            id = payload.campaign_metadata.id,
            name = payload.campaign_metadata.name,
            description = payload.campaign_metadata.description,
            author = payload.campaign_metadata.author,
            licenseType = payload.campaign_metadata.licenseType,
            attributionText = payload.campaign_metadata.attributionText
        )
        saveCampaign(campaignEntity)

        // Convert and save nodes
        val scenarioNodes = payload.nodes.map { campaignNode ->
            val nodeType = when (campaignNode.type) {
                "SOCIAL" -> NodeType.NPC
                "COMBAT" -> NodeType.EVENT
                "EXPLORATION" -> NodeType.LOCATION
                "ITEM" -> NodeType.ITEM
                "FINALE" -> NodeType.FINALE
                "GOAL" -> NodeType.GOAL
                else -> {
                    Log.w("CampaignBootstrapper", "Unknown node type '${campaignNode.type}' for node ${campaignNode.id}. Defaulting to LOCATION.")
                    NodeType.LOCATION
                }
            }
            val scenarioClues = campaignNode.clues.map { campaignClue ->
                Clue(
                    id = campaignClue.clue_id,
                    description = campaignClue.condition,
                    targetNodeId = campaignClue.clue_id // Placeholder: true target node id not provided in clue
                )
            }
            ScenarioNodeEntity(
                id = campaignNode.id,
                campaignId = campaignId,
                title = campaignNode.title,
                description = campaignNode.description,
                type = nodeType,
                clues = scenarioClues
            )
        }
        saveScenarioNodes(scenarioNodes)

        // Convert and save edges
        val scenarioEdges = payload.edges.map { campaignEdge ->
            ScenarioEdgeEntity(
                id = 0, // auto-generated
                campaignId = campaignId,
                sourceNodeId = campaignEdge.sourceNodeId,
                targetNodeId = campaignEdge.targetNodeId,
                description = campaignEdge.description
            )
        }
        saveScenarioEdges(scenarioEdges)

        Log.i("CampaignBootstrapper", "Finished importing campaign: $campaignId")
    }
}
