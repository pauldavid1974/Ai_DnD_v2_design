package com.pauldavid74.ai_dnd.feature.pregame.levelup

import com.pauldavid74.ai_dnd.core.database.entity.CharacterEntity

data class LevelUpState(
    val character: CharacterEntity? = null,
    val newFeatures: List<String> = emptyList(),
    val newSpells: List<String> = emptyList(),
    val isSaving: Boolean = false,
    val error: String? = null,
    val isComplete: Boolean = false
)
