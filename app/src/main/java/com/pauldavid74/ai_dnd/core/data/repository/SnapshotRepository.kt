package com.pauldavid74.ai_dnd.core.data.repository

import com.pauldavid74.ai_dnd.core.database.dao.EntityNodeDao
import com.pauldavid74.ai_dnd.core.database.entity.EntityNode
import com.pauldavid74.ai_dnd.core.database.entity.TurnStateSnapshot
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

interface SnapshotRepository {
    suspend fun takeSnapshot()
    suspend fun rollback()
}

@Singleton
class SnapshotRepositoryImpl @Inject constructor(
    private val entityNodeDao: EntityNodeDao,
    private val json: Json
) : SnapshotRepository {

    override suspend fun takeSnapshot() {
        val entities = entityNodeDao.getEncounterEntities().first()
        val snapshotJson = json.encodeToString(entities)
        entityNodeDao.saveSnapshot(TurnStateSnapshot(snapshotJson = snapshotJson))
    }

    override suspend fun rollback() {
        val snapshot = entityNodeDao.getLatestSnapshot() ?: return
        val entities = json.decodeFromString<List<EntityNode>>(snapshot.snapshotJson)
        
        entityNodeDao.clearEncounter()
        entities.forEach {
            entityNodeDao.upsertEntity(it)
        }
    }
}
