package com.pauldavid74.ai_dnd.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.math.floor

@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val characterClass: String,
    val level: Int,
    val experiencePoints: Int,
    
    // Raw Ability Scores (1-30)
    val strength: Int,
    val dexterity: Int,
    val constitution: Int,
    val intelligence: Int,
    val wisdom: Int,
    val charisma: Int,
    
    // HP tracking
    val currentHp: Int,
    val maxHp: Int,
    val temporaryHp: Int = 0,
    
    // Class features and spells as simple string arrays (MVP Leveling Compromise)
    val classFeatures: List<String> = emptyList(),
    val spells: List<String> = emptyList(),
    val inventory: List<String> = emptyList()
) {
    // Computed Modifiers: floor((score - 10) / 2)
    val strengthModifier: Int get() = calculateModifier(strength)
    val dexterityModifier: Int get() = calculateModifier(dexterity)
    val constitutionModifier: Int get() = calculateModifier(constitution)
    val intelligenceModifier: Int get() = calculateModifier(intelligence)
    val wisdomModifier: Int get() = calculateModifier(wisdom)
    val charismaModifier: Int get() = calculateModifier(charisma)

    // Proficiency Bonus: 2 + ((Level - 1) / 4)
    val proficiencyBonus: Int get() = 2 + ((level - 1) / 4)

    private fun calculateModifier(score: Int): Int {
        return floor((score - 10) / 2.0).toInt()
    }
}
