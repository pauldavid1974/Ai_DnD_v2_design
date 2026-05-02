package com.pauldavid74.ai_dnd.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores a JSON-serialized snapshot of the entire encounter state.
 * Used for deterministic rollbacks when AI hallucinations occur.
 */
@Entity(tableName = "turn_state_snapshots")
data class TurnStateSnapshot(
    @PrimaryKey val id: Int = 1, // Singleton row for the latest turn snapshot
    val snapshotJson: String,
    val timestamp: Long = System.currentTimeMillis()
)
