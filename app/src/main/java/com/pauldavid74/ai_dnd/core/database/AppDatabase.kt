package com.pauldavid74.ai_dnd.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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

@Database(
    entities = [
        CharacterEntity::class,
        MemoryEntity::class,
        SrdReferenceEntity::class,
        CampaignEntity::class,
        ScenarioNodeEntity::class,
        ScenarioEdgeEntity::class,
        FrontEntity::class,
        ChatMessageEntity::class
    ],
    version = 4,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun characterDao(): CharacterDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun memoryDao(): MemoryDao
    abstract fun srdReferenceDao(): SrdReferenceDao
    abstract fun scenarioDao(): ScenarioDao
    abstract fun campaignDao(): CampaignDao

    companion object {
        const val DATABASE_NAME = "ai_dnd_database"
    }
}
