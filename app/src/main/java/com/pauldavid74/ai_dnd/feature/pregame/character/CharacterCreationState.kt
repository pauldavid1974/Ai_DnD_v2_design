package com.pauldavid74.ai_dnd.feature.pregame.character

data class CharacterCreationState(
    val name: String = "",
    val species: String = "",
    val characterClass: String = "",
    val background: String = "",
    val originFeat: String = "",
    val alignment: String = "",
    val inventory: List<String> = emptyList(),
    val spells: List<String> = emptyList(),
    val strength: Int = 8,
    val dexterity: Int = 8,
    val constitution: Int = 8,
    val intelligence: Int = 8,
    val wisdom: Int = 8,
    val charisma: Int = 8,
    val generationMethod: GenerationMethod = GenerationMethod.STANDARD_ARRAY,
    val pointsRemaining: Int = 27,
    val availableArrayValues: List<Int> = listOf(15, 14, 13, 12, 10, 8),
    val isSaving: Boolean = false,
    val error: String? = null,
    val isComplete: Boolean = false,
    val createdCharacterId: Long? = null,
    val availableCampaigns: List<com.pauldavid74.ai_dnd.core.database.entity.CampaignEntity> = emptyList(),
    val selectedCampaignId: String? = null
) {
    val isValid: Boolean get() {
        val nameValid = name.isNotBlank()
        val speciesValid = species.isNotBlank()
        val classValid = characterClass.isNotBlank()
        val backgroundValid = background.isNotBlank()
        val alignmentValid = alignment.isNotBlank()
        val campaignValid = selectedCampaignId != null
        val statsValid = when (generationMethod) {
            GenerationMethod.STANDARD_ARRAY -> {
                val stats = listOf(strength, dexterity, constitution, intelligence, wisdom, charisma)
                stats.sorted() == listOf(8, 10, 12, 13, 14, 15)
            }
            GenerationMethod.POINT_BUY -> {
                pointsRemaining >= 0 && 
                listOf(strength, dexterity, constitution, intelligence, wisdom, charisma).all { it in 8..15 }
            }
        }
        return nameValid && speciesValid && classValid && backgroundValid && alignmentValid && campaignValid && statsValid
    }
}
enum class GenerationMethod {
    STANDARD_ARRAY, POINT_BUY
}
