package com.pauldavid74.ai_dnd.feature.pregame.levelup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pauldavid74.ai_dnd.core.data.repository.GameRepository
import com.pauldavid74.ai_dnd.core.rules.CharacterCreationEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LevelUpViewModel @Inject constructor(
    private val repository: GameRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LevelUpState())
    val uiState: StateFlow<LevelUpState> = _uiState.asStateFlow()

    fun loadCharacter(id: Long) {
        viewModelScope.launch {
            val character = repository.getCharacter(id)
            _uiState.update { it.copy(character = character) }
        }
    }

    fun addFeature(feature: String) {
        if (feature.isBlank()) return
        _uiState.update { it.copy(newFeatures = it.newFeatures + feature) }
    }

    fun addSpell(spell: String) {
        if (spell.isBlank()) return
        _uiState.update { it.copy(newSpells = it.newSpells + spell) }
    }

    fun levelUp() {
        val state = _uiState.value
        val character = state.character ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val hitDie = when (character.characterClass.lowercase()) {
                    "barbarian" -> 12
                    "fighter", "paladin", "ranger" -> 10
                    "bard", "cleric", "druid", "monk", "rogue", "warlock" -> 8
                    "sorcerer", "wizard" -> 6
                    else -> 8
                }
                
                val hpGain = CharacterCreationEngine.calculateLevelUpHp(hitDie, character.constitutionModifier)
                val newMaxHp = character.maxHp + hpGain
                
                val updatedCharacter = character.copy(
                    level = character.level + 1,
                    maxHp = newMaxHp,
                    currentHp = character.currentHp + hpGain, // Or just heal to max? Instruction says gains additional HP.
                    classFeatures = character.classFeatures + state.newFeatures,
                    spells = character.spells + state.newSpells
                )
                
                repository.saveCharacter(updatedCharacter)
                _uiState.update { it.copy(isComplete = true, isSaving = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isSaving = false) }
            }
        }
    }
}
