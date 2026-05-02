package com.pauldavid74.ai_dnd.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pauldavid74.ai_dnd.core.database.entity.ScenarioEdgeEntity
import com.pauldavid74.ai_dnd.core.database.entity.ScenarioNodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScenarioDao {
    @Query("SELECT * FROM scenario_nodes WHERE campaignId = :campaignId")
    fun getNodesForCampaign(campaignId: String): Flow<List<ScenarioNodeEntity>>

    @Query("SELECT * FROM scenario_edges WHERE campaignId = :campaignId")
    fun getEdgesForCampaign(campaignId: String): Flow<List<ScenarioEdgeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNodes(nodes: List<ScenarioNodeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEdges(edges: List<ScenarioEdgeEntity>)
}
