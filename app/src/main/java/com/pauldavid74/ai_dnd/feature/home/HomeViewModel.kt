package com.pauldavid74.ai_dnd.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pauldavid74.ai_dnd.core.data.repository.GameRepository
import com.pauldavid74.ai_dnd.core.security.KeyManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: GameRepository,
    private val keyManager: KeyManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeState())
    val uiState: StateFlow<HomeState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun deleteCharacter(character: com.pauldavid74.ai_dnd.core.database.entity.CharacterEntity) {
        viewModelScope.launch {
            repository.deleteCharacter(character)
        }
    }

    fun loadData() {
        viewModelScope.launch {
            combine(
                repository.getAllCampaigns(),
                repository.getAllCharacters()
            ) { campaigns, characters ->
                HomeState(
                    recentCampaigns = campaigns,
                    characters = characters,
                    isLoading = false,
                    isApiKeyMissing = !keyManager.hasAnyValidKey()
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }
}
