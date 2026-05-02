package com.pauldavid74.ai_dnd.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pauldavid74.ai_dnd.core.domain.model.Clue
import com.pauldavid74.ai_dnd.core.domain.model.NodeType

@Entity(tableName = "scenario_nodes")
data class ScenarioNodeEntity(
    @PrimaryKey val id: String,
    val campaignId: String,
    val title: String,
    val description: String,
    val type: NodeType,
    val clues: List<Clue>
)

@Entity(tableName = "scenario_edges")
data class ScenarioEdgeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val campaignId: String,
    val sourceNodeId: String,
    val targetNodeId: String,
    val description: String
)
