package com.pauldavid74.ai_dnd.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pauldavid74.ai_dnd.core.database.entity.EntityNode
import com.pauldavid74.ai_dnd.core.database.entity.TurnStateSnapshot
import kotlinx.coroutines.flow.Flow

@Dao
interface EntityNodeDao {
    @Query("SELECT * FROM entity_nodes ORDER BY initiative DESC")
    fun getEncounterEntities(): Flow<List<EntityNode>>

    @Query("SELECT * FROM entity_nodes WHERE id = :id")
    suspend fun getEntityById(id: String): EntityNode?

    /**
     * SRD 5.2.1 Area of Effect (AoE) Spatial Query.
     * Calculates Euclidean distance directly in SQLite.
     */
    @Query("""
        SELECT * FROM entity_nodes 
        WHERE status != 'DEAD' 
        AND ((x - :originX) * (x - :originX) + (y - :originY) * (y - :originY)) <= (:radiusSq)
    """)
    suspend fun getEntitiesWithinRadius(originX: Float, originY: Float, radiusSq: Float): List<EntityNode>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntity(entity: EntityNode)

    @Update
    suspend fun updateEntities(entities: List<EntityNode>)

    @Query("DELETE FROM entity_nodes")
    suspend fun clearEncounter()

    // Memento Pattern: Snapshot persistence
    @Query("SELECT * FROM turn_state_snapshots WHERE id = 1")
    suspend fun getLatestSnapshot(): TurnStateSnapshot?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSnapshot(snapshot: TurnStateSnapshot)
}
