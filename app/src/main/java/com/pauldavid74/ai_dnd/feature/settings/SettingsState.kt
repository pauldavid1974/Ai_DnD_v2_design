package com.pauldavid74.ai_dnd.feature.settings

import com.pauldavid74.ai_dnd.core.network.model.AiModel
import com.pauldavid74.ai_dnd.core.network.model.LLMProvider

data class SettingsState(
    val providerKeys: Map<String, String> = emptyMap(),
    val providerUrls: Map<String, String> = emptyMap(),
    val availableModels: Map<String, List<AiModel>> = emptyMap(),
    val selectedModels: Map<String, String> = emptyMap(),
    val selectedProviderId: String = "",
    val isLoadingModels: Boolean = false,
    val isVerifying: Boolean = false,
    val verificationResults: Map<String, VerificationResult?> = emptyMap(),
    val savedProviders: Set<String> = emptySet(),
    val isModelSaved: Map<String, Boolean> = emptyMap(),
    val installedCampaigns: List<com.pauldavid74.ai_dnd.core.database.entity.CampaignEntity> = emptyList(),
    val error: String? = null
) {
    val currentProvider: LLMProvider? get() = LLMProvider.ALL_PROVIDERS.find { it.id == selectedProviderId }
    
    val currentKey: String get() = providerKeys[selectedProviderId] ?: ""
    
    val currentUrl: String get() = providerUrls[selectedProviderId] ?: currentProvider?.baseUrl ?: ""
    
    val currentModels: List<AiModel> get() = availableModels[selectedProviderId] ?: emptyList()
    
    val currentSelectedModel: String get() = selectedModels[selectedProviderId].let {
        if (it.isNullOrBlank()) currentProvider?.defaultModel ?: "" else it
    }
    
    val currentVerificationResult: VerificationResult? get() = verificationResults[selectedProviderId]
    
    val isCurrentKeySaved: Boolean get() = savedProviders.contains(selectedProviderId)

    val isCurrentModelSaved: Boolean get() = isModelSaved[selectedProviderId] ?: false
}

sealed class VerificationResult {
    object Success : VerificationResult()
    data class Failure(val message: String) : VerificationResult()
}
