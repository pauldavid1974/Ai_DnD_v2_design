package com.pauldavid74.ai_dnd.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class AiModel(
    val id: String,
    val name: String,
    val provider: String
)

@Serializable
data class NarrativePayload(
    val narration: String,
    val requested_action: RequestedAction? = null,
    val choices: List<String> = emptyList(),
    val memory_updates: List<String> = emptyList(),
    val wiki_lookups: List<String> = emptyList()
)

@Serializable
data class RequestedAction(
    val type: String,
    val parameter: String? = null,
    val dc: Int? = null
)
