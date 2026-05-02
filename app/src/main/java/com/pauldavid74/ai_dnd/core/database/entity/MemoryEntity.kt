package com.pauldavid74.ai_dnd.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // e.g., "NPC", "TOWN", "FACTION", "PLOT_SUMMARY"
    val key: String, // e.g., "Garrick", "Phandalin"
    val content: String, // The narrative summary or update
    val metadataJson: String = "{}", // JSON for relationships or tags
    val timestamp: Long = System.currentTimeMillis()
)
