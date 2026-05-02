package com.pauldavid74.ai_dnd.core.di

import android.content.Context
import androidx.room.Room
import com.pauldavid74.ai_dnd.core.database.AppDatabase
import com.pauldavid74.ai_dnd.core.database.dao.CampaignDao
import com.pauldavid74.ai_dnd.core.database.dao.CharacterDao
import com.pauldavid74.ai_dnd.core.database.dao.ChatMessageDao
import com.pauldavid74.ai_dnd.core.database.dao.MemoryDao
import com.pauldavid74.ai_dnd.core.database.dao.ScenarioDao
import com.pauldavid74.ai_dnd.core.database.dao.SrdReferenceDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideCharacterDao(database: AppDatabase): CharacterDao {
        return database.characterDao()
    }

    @Provides
    fun provideChatMessageDao(database: AppDatabase): ChatMessageDao {
        return database.chatMessageDao()
    }

    @Provides
    fun provideMemoryDao(database: AppDatabase): MemoryDao {
        return database.memoryDao()
    }

    @Provides
    fun provideSrdReferenceDao(database: AppDatabase): SrdReferenceDao {
        return database.srdReferenceDao()
    }

    @Provides
    fun provideScenarioDao(database: AppDatabase): ScenarioDao {
        return database.scenarioDao()
    }

    @Provides
    fun provideCampaignDao(database: AppDatabase): CampaignDao {
        return database.campaignDao()
    }
}
