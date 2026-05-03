package com.pauldavid74.ai_dnd.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pauldavid74.ai_dnd.core.database.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE characterId = :characterId ORDER BY timestamp ASC")
    fun getMessagesForCharacter(characterId: Long): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE characterId = :characterId")
    suspend fun deleteMessagesForCharacter(characterId: Long)

    @Query("DELETE FROM chat_messages WHERE id = (SELECT id FROM chat_messages WHERE characterId = :characterId ORDER BY timestamp DESC LIMIT 1)")
    suspend fun deleteLastMessage(characterId: Long)
}
