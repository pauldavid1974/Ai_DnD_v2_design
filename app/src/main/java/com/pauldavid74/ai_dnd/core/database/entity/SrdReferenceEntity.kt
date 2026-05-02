package com.pauldavid74.ai_dnd.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "srd_references")
data class SrdReferenceEntity(
    @PrimaryKey val id: String, // e.g., "spell_fireball", "class_fighter"
    val category: String, // e.g., "SPELL", "CLASS", "MONSTER", "RULE"
    val name: String,
    val contentJson: String, // Machine-readable rule data (D&D 5.2.1)
    val lastUpdated: Long = System.currentTimeMillis()
)
