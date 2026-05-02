package com.pauldavid74.ai_dnd.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Front(
    val id: String,
    val name: String,
    val description: String,
    val doom: String, // What happens if not stopped
    val portents: List<GrimPortent> = emptyList()
)

@Serializable
data class GrimPortent(
    val id: String,
    val description: String,
    val isTriggered: Boolean = false
)
