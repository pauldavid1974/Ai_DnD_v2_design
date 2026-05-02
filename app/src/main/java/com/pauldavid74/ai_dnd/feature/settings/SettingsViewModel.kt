package com.pauldavid74.ai_dnd.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pauldavid74.ai_dnd.core.data.repository.AiProviderRepository
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
    private val aiRepository: AiProviderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsState())
    val uiState: StateFlow<SettingsState> = _uiState.asStateFlow()

    init {
        _uiState.update { state ->
            val activeProvider = keyManager.getActiveProvider()
            state.copy(
                openAiKey = keyManager.getApiKey(KeyManager.PROVIDER_OPENAI) ?: "",
                anthropicKey = keyManager.getApiKey(KeyManager.PROVIDER_ANTHROPIC) ?: "",
                groqKey = keyManager.getApiKey(KeyManager.PROVIDER_GROQ) ?: "",
                selectedProviderId = activeProvider,
                selectedModelId = keyManager.getActiveModel(activeProvider) ?: ""
            )
        }
        fetchModels()
    }

    fun onOpenAiKeyChanged(key: String) {
        _uiState.update { it.copy(openAiKey = key, isSaved = false) }
    }

    fun onAnthropicKeyChanged(key: String) {
        _uiState.update { it.copy(anthropicKey = key, isSaved = false) }
    }

    fun onGroqKeyChanged(key: String) {
        _uiState.update { it.copy(groqKey = key, isSaved = false) }
    }

    fun saveKeys() {
        val state = _uiState.value
        keyManager.saveApiKey(KeyManager.PROVIDER_OPENAI, state.openAiKey)
        keyManager.saveApiKey(KeyManager.PROVIDER_ANTHROPIC, state.anthropicKey)
        keyManager.saveApiKey(KeyManager.PROVIDER_GROQ, state.groqKey)
        _uiState.update { it.copy(isSaved = true) }
        fetchModels()
    }

    fun onProviderSelected(providerId: String) {
        _uiState.update { it.copy(selectedProviderId = providerId) }
        keyManager.saveActiveProvider(providerId)
        
        // Try to restore last selected model for this provider
        val lastModelId = keyManager.getActiveModel(providerId)
        if (lastModelId != null) {
            _uiState.update { it.copy(selectedModelId = lastModelId) }
        }
        
        fetchModels()
    }

    fun onModelSelected(modelId: String) {
        _uiState.update { it.copy(selectedModelId = modelId) }
        keyManager.saveActiveModel(_uiState.value.selectedProviderId, modelId)
    }

    private fun fetchModels() {
        val providerId = _uiState.value.selectedProviderId
        if (!aiRepository.hasKey(providerId)) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingModels = true, error = null) }
            try {
                val models = aiRepository.getAvailableModels(providerId)
                val currentSelectedModelId = _uiState.value.selectedModelId
                
                // If current model is not in the new list, or if we didn't have a model selected,
                // pick the first one. Otherwise keep the restored one.
                val modelToSelect = if (models.any { it.id == currentSelectedModelId }) {
                    currentSelectedModelId
                } else {
                    val firstModelId = models.firstOrNull()?.id ?: ""
                    if (firstModelId.isNotEmpty()) {
                        keyManager.saveActiveModel(providerId, firstModelId)
                    }
                    firstModelId
                }

                _uiState.update { it.copy(
                    availableModels = models,
                    selectedModelId = modelToSelect,
                    isLoadingModels = false
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingModels = false, error = "Failed to fetch models: ${e.message}") }
            }
        }
    }
}
