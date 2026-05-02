package com.pauldavid74.ai_dnd.feature.pregame.character

data class CharacterCreationState(
    val name: String = "",
    val characterClass: String = "",
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
    val createdCharacterId: Long? = null
) {
    val isValid: Boolean get() {
        val nameValid = name.isNotBlank()
        val classValid = characterClass.isNotBlank()
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
        return nameValid && classValid && statsValid
    }
}
enum class GenerationMethod {
    STANDARD_ARRAY, POINT_BUY
}
