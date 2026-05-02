package com.pauldavid74.ai_dnd.core.rules

import com.pauldavid74.ai_dnd.core.database.entity.CharacterEntity

/**
 * Handles deterministic logic for Character Sheets according to SRD 5.2.1.
 */
object CharacterSheetManager {

    /**
     * Calculates Initiative based on Dex Modifier and optional Alert feat bonus.
     */
    fun calculateInitiative(character: CharacterEntity): Int {
        var initiative = character.dexterityModifier
        if (character.originFeat.equals("Alert", ignoreCase = true)) {
            initiative += character.proficiencyBonus
        }
        return initiative
    }

    /**
     * Calculates Passive Perception: 10 + Wis Modifier + Proficiency (if applicable).
     * Note: Simplified for MVP to 10 + Wis Mod.
     */
    fun calculatePassivePerception(character: CharacterEntity): Int {
        return 10 + character.wisdomModifier
    }

    /**
     * Standardized speed to 30ft for all species in SRD 5.2.1 (unless modified).
     */
    fun calculateSpeed(character: CharacterEntity): Int {
        // Standardized to 30ft as per Character Sheet Upgrade requirement [cite: 1267, 1285]
        return 30
    }

    /**
     * Returns a list of skills with their calculated totals (Ability Mod + Proficiency).
     * For MVP, we assume a basic list of core skills mapping.
     */
    fun getSkillModifiers(character: CharacterEntity): Map<String, Int> {
        return mapOf(
            "Acrobatics" to character.dexterityModifier,
            "Athletics" to character.strengthModifier,
            "Insight" to character.wisdomModifier,
            "Perception" to character.wisdomModifier,
            "Stealth" to character.dexterityModifier,
            "Survival" to character.wisdomModifier
        )
    }
}
