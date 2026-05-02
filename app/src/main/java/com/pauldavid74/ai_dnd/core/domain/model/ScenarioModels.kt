package com.pauldavid74.ai_dnd.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ScenarioNode(
    val id: String,
    val title: String,
    val description: String,
    val type: NodeType,
    val clues: List<Clue> = emptyList()
)

@Serializable
enum class NodeType {
    LOCATION, NPC, EVENT, ITEM, FINALE, GOAL
}

@Serializable
data class Clue(
    val id: String,
    val description: String,
    val targetNodeId: String // Which node this clue leads to
)

@Serializable
data class ScenarioEdge(
    val sourceNodeId: String,
    val targetNodeId: String,
    val description: String
)

@Serializable
data class ScenarioGraph(
    val nodes: List<ScenarioNode>,
    val edges: List<ScenarioEdge>
)
