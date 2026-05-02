package com.pauldavid74.ai_dnd.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a spatial node in the combat encounter.
 * This is the source of truth for all mathematical adjudication in Phase B.
 */
@Entity(
    tableName = "entity_nodes",
    indices = [
        Index(value = ["x"]),
        Index(value = ["y"])
    ]
)
data class EntityNode(
    @PrimaryKey val id: String,
    val name: String,
    val x: Int,
    val y: Int,
    val hp: Int,
    val maxHp: Int,
    val ac: Int,
    val status: String = "ALIVE", // ALIVE, UNCONSCIOUS, DEAD
    val initiative: Int = 0,
    val entityType: String, // PLAYER, NPC, MONSTER, HAZARD
    
    // SRD 5.2.1 Resource Tracking
    val hasUsedAction: Boolean = false,
    val hasUsedBonusAction: Boolean = false,
    val hasUsedReaction: Boolean = false,
    val leveledSpellCastThisTurn: Boolean = false
)
