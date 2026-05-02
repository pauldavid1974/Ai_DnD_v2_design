package com.pauldavid74.ai_dnd.core.rules

import com.pauldavid74.ai_dnd.core.database.entity.CharacterEntity
import kotlin.math.max

class CombatEngine(
    private val diceEngine: DiceEngine = DiceEngine()
) {
    private val parser = DiceParser()

    /**
     * Executes an attack roll.
     */
    fun resolveAttack(
        targetAc: Int,
        modifier: Int,
        mode: RollMode = RollMode.Normal
    ): com.pauldavid74.ai_dnd.core.domain.factory.AdjudicationResult {
        val diceRoll = DiceRoll(count = 1, sides = 20, modifier = modifier, mode = mode)
        val result = diceEngine.roll(diceRoll)
        
        val isHit = result.isCriticalSuccess || (result.total >= targetAc && !result.isCriticalFailure)
        
        return if (isHit) {
            com.pauldavid74.ai_dnd.core.domain.factory.AdjudicationResult.Hit(0, "ALIVE", result.total) // Damage to be added
        } else {
            com.pauldavid74.ai_dnd.core.domain.factory.AdjudicationResult.Miss(result.total)
        }
    }

    /**
     * Executes a saving throw.
     */
    fun resolveSavingThrow(
        dc: Int,
        modifier: Int,
        mode: RollMode = RollMode.Normal
    ): com.pauldavid74.ai_dnd.core.domain.factory.AdjudicationResult {
        val diceRoll = DiceRoll(count = 1, sides = 20, modifier = modifier, mode = mode)
        val result = diceEngine.roll(diceRoll)
        val isSuccess = result.total >= dc
        
        return if (isSuccess) {
            com.pauldavid74.ai_dnd.core.domain.factory.AdjudicationResult.Success(result.total, dc)
        } else {
            com.pauldavid74.ai_dnd.core.domain.factory.AdjudicationResult.Failure(result.total, dc)
        }
    }

    /**
     * Calculates damage, handling critical hits as per SRD (double dice).
     */
    fun calculateDamage(
        damageNotation: String,
        modifier: Int,
        isCritical: Boolean = false
    ): RollResult {
        val baseRoll = parser.parse(damageNotation) ?: throw IllegalArgumentException("Invalid damage notation: $damageNotation")
        
        val finalRoll = if (isCritical) {
            baseRoll.copy(count = baseRoll.count * 2, modifier = modifier)
        } else {
            baseRoll.copy(modifier = modifier)
        }
        
        return diceEngine.roll(finalRoll)
    }

    /**
     * Applies damage to a character's HP, clamped at 0.
     */
    fun applyDamage(character: CharacterEntity, damage: Int): CharacterEntity {
        val remainingDamageAfterTempHp = max(0, damage - character.temporaryHp)
        val newTempHp = max(0, character.temporaryHp - damage)
        val newCurrentHp = max(0, character.currentHp - remainingDamageAfterTempHp)
        
        return character.copy(
            currentHp = newCurrentHp,
            temporaryHp = newTempHp
        )
    }

    /**
     * Executes an ability check (Investigation, Perception, etc.).
     */
    fun resolveAbilityCheck(
        dc: Int,
        modifier: Int,
        mode: RollMode = RollMode.Normal
    ): com.pauldavid74.ai_dnd.core.domain.factory.AdjudicationResult {
        val diceRoll = DiceRoll(count = 1, sides = 20, modifier = modifier, mode = mode)
        val result = diceEngine.roll(diceRoll)
        val isSuccess = result.total >= dc
        
        return if (isSuccess) {
            com.pauldavid74.ai_dnd.core.domain.factory.AdjudicationResult.Success(result.total, dc)
        } else {
            com.pauldavid74.ai_dnd.core.domain.factory.AdjudicationResult.Failure(result.total, dc)
        }
    }

    /**
     * Translates raw HP into semantic buckets for AI and UI.
     */
    fun getHpBucket(currentHp: Int, maxHp: Int): HpBucket {
        if (currentHp <= 0) return HpBucket.DEAD
        val percentage = (currentHp.toDouble() / maxHp.toDouble()) * 100.0
        return when {
            percentage >= 80.0 -> HpBucket.HEALTHY
            percentage >= 50.0 -> HpBucket.WOUNDED
            percentage >= 11.0 -> HpBucket.BLOODIED
            else -> HpBucket.NEAR_DEATH
        }
    }
}
