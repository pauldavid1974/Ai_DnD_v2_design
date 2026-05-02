package com.pauldavid74.ai_dnd.core.rules

import java.security.SecureRandom

class DiceParser {
    private val regex = Regex("""(\d+)d(\d+)(?:\s*([+-])\s*(\d+))?""")

    /**
     * Parses a dice notation string (e.g., "1d20+5", "2d6-1").
     * @param notation The dice notation to parse.
     * @param mode The roll mode (Normal, Advantage, Disadvantage).
     * @return A DiceRoll object or null if parsing fails.
     */
    fun parse(notation: String, mode: RollMode = RollMode.Normal): DiceRoll? {
        val cleanNotation = notation.lowercase().replace(" ", "")
        val match = regex.find(cleanNotation) ?: return null
        
        val count = match.groupValues[1].toInt()
        val sides = match.groupValues[2].toInt()
        val sign = match.groupValues[3]
        val modifierValue = match.groupValues[4].toIntOrNull() ?: 0
        
        val modifier = if (sign == "-") -modifierValue else modifierValue
        
        return DiceRoll(count, sides, modifier, mode)
    }
}

class DiceEngine(
    private val secureRandom: SecureRandom = SecureRandom()
) {
    /**
     * Executes a dice roll based on the provided DiceRoll configuration.
     */
    fun roll(diceRoll: DiceRoll): RollResult {
        return when (diceRoll.mode) {
            RollMode.Normal -> executeNormalRoll(diceRoll)
            RollMode.Advantage -> executeAdvantageRoll(diceRoll)
            RollMode.Disadvantage -> executeDisadvantageRoll(diceRoll)
        }
    }

    private fun executeNormalRoll(diceRoll: DiceRoll): RollResult {
        val rolls = List(diceRoll.count) { secureRandom.nextInt(diceRoll.sides) + 1 }
        val total = rolls.sum() + diceRoll.modifier
        
        val isCriticalSuccess = diceRoll.sides == 20 && diceRoll.count == 1 && rolls.first() == 20
        val isCriticalFailure = diceRoll.sides == 20 && diceRoll.count == 1 && rolls.first() == 1
        
        return RollResult(rolls, diceRoll.modifier, total, isCriticalSuccess, isCriticalFailure)
    }

    private fun executeAdvantageRoll(diceRoll: DiceRoll): RollResult {
        val firstRolls = List(diceRoll.count) { secureRandom.nextInt(diceRoll.sides) + 1 }
        val secondRolls = List(diceRoll.count) { secureRandom.nextInt(diceRoll.sides) + 1 }
        
        val firstTotal = firstRolls.sum()
        val secondTotal = secondRolls.sum()
        
        val finalRolls = if (firstTotal >= secondTotal) firstRolls else secondRolls
        val total = finalRolls.sum() + diceRoll.modifier
        
        val isCriticalSuccess = diceRoll.sides == 20 && finalRolls.contains(20)
        val isCriticalFailure = diceRoll.sides == 20 && finalRolls.all { it == 1 }
        
        return RollResult(finalRolls, diceRoll.modifier, total, isCriticalSuccess, isCriticalFailure)
    }

    private fun executeDisadvantageRoll(diceRoll: DiceRoll): RollResult {
        val firstRolls = List(diceRoll.count) { secureRandom.nextInt(diceRoll.sides) + 1 }
        val secondRolls = List(diceRoll.count) { secureRandom.nextInt(diceRoll.sides) + 1 }
        
        val firstTotal = firstRolls.sum()
        val secondTotal = secondRolls.sum()
        
        val finalRolls = if (firstTotal <= secondTotal) firstRolls else secondRolls
        val total = finalRolls.sum() + diceRoll.modifier
        
        val isCriticalSuccess = diceRoll.sides == 20 && finalRolls.all { it == 20 }
        val isCriticalFailure = diceRoll.sides == 20 && finalRolls.contains(1)
        
        return RollResult(finalRolls, diceRoll.modifier, total, isCriticalSuccess, isCriticalFailure)
    }
}
