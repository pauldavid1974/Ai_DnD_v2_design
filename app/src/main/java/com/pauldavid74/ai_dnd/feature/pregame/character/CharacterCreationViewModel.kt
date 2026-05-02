package com.pauldavid74.ai_dnd.feature.pregame.character

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pauldavid74.ai_dnd.core.data.repository.GameRepository
import com.pauldavid74.ai_dnd.core.database.entity.CharacterEntity
import com.pauldavid74.ai_dnd.core.rules.CharacterCreationEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CharacterCreationViewModel @Inject constructor(
    private val repository: GameRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CharacterCreationState())
    val uiState: StateFlow<CharacterCreationState> = _uiState.asStateFlow()

    fun onNameChanged(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun onClassChanged(characterClass: String) {
        _uiState.update { it.copy(characterClass = characterClass) }
    }

    fun onMethodChanged(method: GenerationMethod) {
        _uiState.update { state ->
            if (method == GenerationMethod.STANDARD_ARRAY) {
                state.copy(
                    generationMethod = method,
                    strength = 15, dexterity = 14, constitution = 13,
                    intelligence = 12, wisdom = 10, charisma = 8,
                    pointsRemaining = 0
                )
            } else {
                state.copy(
                    generationMethod = method,
                    strength = 8, dexterity = 8, constitution = 8,
                    intelligence = 8, wisdom = 8, charisma = 8,
                    pointsRemaining = 27
                )
            }
        }
    }

    fun updateStat(stat: String, value: Int) {
        _uiState.update { state ->
            val statName = stat.lowercase()
            
            if (state.generationMethod == GenerationMethod.STANDARD_ARRAY) {
                // In Standard Array, we swap values if the target value is already taken
                val currentStats = mapOf(
                    "strength" to state.strength,
                    "dexterity" to state.dexterity,
                    "constitution" to state.constitution,
                    "intelligence" to state.intelligence,
                    "wisdom" to state.wisdom,
                    "charisma" to state.charisma
                )
                
                val existingStatForValue = currentStats.entries.find { it.value == value }?.key
                val currentValueForStat = currentStats[statName] ?: 8
                
                var newState = state
                if (existingStatForValue != null && existingStatForValue != statName) {
                    // Swap: give the current value of this stat to the stat that had the target value
                    newState = updateStatValue(newState, existingStatForValue, currentValueForStat)
                }
                updateStatValue(newState, statName, value)
            } else {
                // Point Buy logic
                val newState = updateStatValue(state, statName, value)
                val scores = listOf(
                    newState.strength, newState.dexterity, newState.constitution,
                    newState.intelligence, newState.wisdom, newState.charisma
                )
                val cost = CharacterCreationEngine.calculatePointBuyCost(scores)
                newState.copy(pointsRemaining = 27 - cost)
            }
        }
    }

    private fun updateStatValue(state: CharacterCreationState, stat: String, value: Int): CharacterCreationState {
        return when (stat.lowercase()) {
            "strength" -> state.copy(strength = value)
            "dexterity" -> state.copy(dexterity = value)
            "constitution" -> state.copy(constitution = value)
            "intelligence" -> state.copy(intelligence = value)
            "wisdom" -> state.copy(wisdom = value)
            "charisma" -> state.copy(charisma = value)
            else -> state
        }
    }

    fun saveCharacter() {
        Log.d("CharacterCreation", "saveCharacter() called. State: ${_uiState.value}")
        val state = _uiState.value
        if (!state.isValid) {
            Log.w("CharacterCreation", "saveCharacter() ignored: State is NOT valid.")
            _uiState.update { it.copy(error = "Invalid character configuration. Make sure name and class are filled and stats are correctly assigned.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                // Calculate HP based on class hit die (MVP mapping)
                val hitDie = when (state.characterClass.lowercase()) {
                    "barbarian" -> 12
                    "fighter", "paladin", "ranger" -> 10
                    "bard", "cleric", "druid", "monk", "rogue", "warlock" -> 8
                    "sorcerer", "wizard" -> 6
                    else -> 8
                }
                
                val conMod = (state.constitution - 10) / 2
                val maxHp = CharacterCreationEngine.calculateInitialHp(hitDie, conMod)

                val character = CharacterEntity(
                    name = state.name,
                    characterClass = state.characterClass,
                    level = 1,
                    experiencePoints = 0,
                    strength = state.strength,
                    dexterity = state.dexterity,
                    constitution = state.constitution,
                    intelligence = state.intelligence,
                    wisdom = state.wisdom,
                    charisma = state.charisma,
                    currentHp = maxHp,
                    maxHp = maxHp
                )
                Log.d("CharacterCreation", "Saving character: $character")
                val newId = repository.saveCharacter(character)
                Log.d("CharacterCreation", "Character saved with ID: $newId")
                _uiState.update { it.copy(createdCharacterId = newId, isComplete = true, isSaving = false) }
            } catch (e: Exception) {
                Log.e("CharacterCreation", "Error saving character", e)
                _uiState.update { it.copy(error = e.message, isSaving = false) }
            }
        }
    }
}
