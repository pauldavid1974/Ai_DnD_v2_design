package com.pauldavid74.ai_dnd.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pauldavid74.ai_dnd.core.data.repository.AiProviderRepository
import com.pauldavid74.ai_dnd.core.network.model.AiModel
import com.pauldavid74.ai_dnd.core.network.model.LLMProvider
import com.pauldavid74.ai_dnd.core.security.KeyManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val keyManager: KeyManager,
    private val aiRepository: AiProviderRepository,
    private val gameRepository: com.pauldavid74.ai_dnd.core.data.repository.GameRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsState())
    val uiState: StateFlow<SettingsState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            gameRepository.getAllCampaigns().collect { campaigns ->
                _uiState.update { it.copy(installedCampaigns = campaigns) }
            }
        }

        val activeProviderId = keyManager.getActiveProvider() ?: ""
        val allProviderIds = LLMProvider.ALL_PROVIDERS.map { it.id }
        
        val initialKeys = allProviderIds.associateWith { keyManager.getApiKey(it) ?: "" }
        val initialUrls = allProviderIds.associateWith { keyManager.getCustomBaseUrl(it) ?: "" }
        val initialSelectedModels = allProviderIds.associateWith { keyManager.getActiveModel(it) ?: "" }
        val initialSavedProviders = allProviderIds.filter { keyManager.getApiKey(it) != null }.toSet()

        _uiState.update { it.copy(
            providerKeys = initialKeys,
            providerUrls = initialUrls,
            selectedProviderId = activeProviderId,
            selectedModels = initialSelectedModels,
            savedProviders = initialSavedProviders,
            isModelSaved = allProviderIds.associateWith { (keyManager.getActiveModel(it) ?: "").isNotEmpty() }
        ) }

        // Background fetch for already verified active provider
        if (activeProviderId.isNotEmpty() && initialSavedProviders.contains(activeProviderId)) {
            loadModelsForProvider(activeProviderId)
        }
    }

    private fun loadModelsForProvider(providerId: String) {
        viewModelScope.launch {
            try {
                val models = aiRepository.getAvailableModels(providerId)
                if (models.isNotEmpty()) {
                    val filteredModels = filterChatModels(models)
                    _uiState.update { it.copy(
                        availableModels = it.availableModels + (providerId to filteredModels)
                    ) }
                }
            } catch (e: Exception) {
                // Ignore background fetch errors
            }
        }
    }

    private fun filterChatModels(models: List<AiModel>): List<AiModel> {
        val filtered = models.filter { model ->
            val id = model.id.lowercase()
            id.contains("gpt") || id.contains("claude") || id.contains("llama") || 
            id.contains("mistral") || id.contains("gemini") || id.contains("deepseek") ||
            id.contains("qwen") || id.contains("phi") || id.contains("gemma")
        }
        return if (filtered.isEmpty()) models else filtered
    }

    fun onKeyChanged(key: String) {
        val providerId = _uiState.value.selectedProviderId
        _uiState.update { state -> 
            state.copy(
                providerKeys = state.providerKeys + (providerId to key),
                verificationResults = state.verificationResults + (providerId to null),
                savedProviders = state.savedProviders - providerId
            ) 
        }
    }

    fun onUrlChanged(url: String) {
        val providerId = _uiState.value.selectedProviderId
        _uiState.update { state ->
            state.copy(
                providerUrls = state.providerUrls + (providerId to url),
                savedProviders = state.savedProviders - providerId
            )
        }
    }

    fun onProviderSelected(providerId: String) {
        _uiState.update { it.copy(selectedProviderId = providerId) }
        if (_uiState.value.savedProviders.contains(providerId) && _uiState.value.availableModels[providerId] == null) {
            loadModelsForProvider(providerId)
        }
    }

    fun onModelSelected(modelId: String) {
        val providerId = _uiState.value.selectedProviderId
        _uiState.update { state ->
            state.copy(
                selectedModels = state.selectedModels + (providerId to modelId),
                isModelSaved = state.isModelSaved + (providerId to false)
            )
        }
    }

    fun saveModel() {
        val state = _uiState.value
        val modelId = state.currentSelectedModel
        keyManager.saveActiveModel(state.selectedProviderId, modelId)
        keyManager.saveActiveProvider(state.selectedProviderId)
        _uiState.update { it.copy(
            isModelSaved = it.isModelSaved + (state.selectedProviderId to true)
        ) }
    }

    fun verifyKeyAndFetchModels(apiKey: String) {
        val state = _uiState.value
        val providerId = state.selectedProviderId
        val customUrl = state.providerUrls[providerId]
        
        viewModelScope.launch {
            _uiState.update { it.copy(isVerifying = true) }
            _uiState.update { it.copy(verificationResults = it.verificationResults + (providerId to null)) }
            
            try {
                keyManager.saveApiKey(providerId, apiKey)
                if (!customUrl.isNullOrBlank()) {
                    keyManager.saveCustomBaseUrl(providerId, customUrl)
                }
                
                val models = aiRepository.getAvailableModels(providerId)
                val filteredModels = filterChatModels(models)
                
                _uiState.update { s ->
                    s.copy(
                        isVerifying = false,
                        verificationResults = s.verificationResults + (providerId to VerificationResult.Success),
                        availableModels = s.availableModels + (providerId to filteredModels),
                        savedProviders = s.savedProviders + providerId
                    )
                }
            } catch (e: Exception) {
                keyManager.deleteApiKey(providerId)
                _uiState.update { s ->
                    s.copy(
                        isVerifying = false,
                        verificationResults = s.verificationResults + (providerId to VerificationResult.Failure(e.message ?: "Connection failed")),
                        savedProviders = s.savedProviders - providerId
                    )
                }
            }
        }
    }
}
