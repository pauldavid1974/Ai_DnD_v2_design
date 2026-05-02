package com.pauldavid74.ai_dnd.feature.home

import com.pauldavid74.ai_dnd.core.database.entity.CampaignEntity
import com.pauldavid74.ai_dnd.core.database.entity.CharacterEntity

data class HomeState(
    val recentCampaigns: List<CampaignEntity> = emptyList(),
    val characters: List<CharacterEntity> = emptyList(),
    val isLoading: Boolean = true,
    val isApiKeyMissing: Boolean = false
)
