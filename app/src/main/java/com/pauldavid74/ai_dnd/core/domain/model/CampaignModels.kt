package com.pauldavid74.ai_dnd.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CampaignImportPayload(
    val campaign_metadata: CampaignMetadata,
    val nodes: List<CampaignNode>,
    val edges: List<CampaignEdge>
)

@Serializable
data class CampaignMetadata(
    val id: String,
    val name: String,
    val description: String,
    val author: String,
    val licenseType: String,
    val attributionText: String
)

@Serializable
data class CampaignNode(
    val id: String,
    val campaignId: String,
    val title: String,
    val type: String,
    val description: String,
    val clues: List<CampaignClue>,
    val threats: List<String>
)

@Serializable
data class CampaignClue(
    val clue_id: String,
    val condition: String,
    val reveal: String
)

@Serializable
data class CampaignEdge(
    val sourceNodeId: String,
    val targetNodeId: String,
    val description: String
)