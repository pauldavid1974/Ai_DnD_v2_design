package com.pauldavid74.ai_dnd.feature.settings

import com.pauldavid74.ai_dnd.core.network.model.AiModel

data class SettingsState(
    val openAiKey: String = "",
    val anthropicKey: String = "",
    val groqKey: String = "",
    val availableModels: List<AiModel> = emptyList(),
    val selectedModelId: String = "",
    val selectedProviderId: String = "openai",
    val isLoadingModels: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)
