package com.pauldavid74.ai_dnd.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pauldavid74.ai_dnd.core.database.entity.CampaignEntity
import com.pauldavid74.ai_dnd.core.database.entity.FrontEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CampaignDao {
    @Query("SELECT * FROM campaigns")
    fun getAllCampaigns(): Flow<List<CampaignEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCampaign(campaign: CampaignEntity)

    @Query("SELECT * FROM fronts WHERE campaignId = :campaignId")
    fun getFrontsForCampaign(campaignId: String): Flow<List<FrontEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFronts(fronts: List<FrontEntity>)
}
