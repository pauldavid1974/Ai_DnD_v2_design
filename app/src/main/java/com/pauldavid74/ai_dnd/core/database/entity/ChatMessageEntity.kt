package com.pauldavid74.ai_dnd.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pauldavid74.ai_dnd.feature.game.MessageSender

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val characterId: Long,
    val sender: MessageSender,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
