package com.pauldavid74.ai_dnd.core.rules

import com.pauldavid74.ai_dnd.core.database.entity.CharacterEntity
import org.junit.Assert.*
import org.junit.Test

class CombatEngineTest {
    private val engine = CombatEngine()

    @Test
    fun testResolveAttack() {
        // Test basic hit/miss logic
        // We can't control the dice, but we can test the boundaries
        val (isHit, result) = engine.resolveAttack(targetAc = 10, modifier = 0)
        if (result.total >= 10 && !result.isCriticalFailure) {
            assertTrue(isHit)
        } else if (result.isCriticalSuccess) {
            assertTrue(isHit)
        } else {
            assertFalse(isHit)
        }
    }

    @Test
    fun testResolveSavingThrow() {
        val (isSuccess, result) = engine.resolveSavingThrow(dc = 15, modifier = 0)
        if (result.total >= 15) {
            assertTrue(isSuccess)
        } else {
            assertFalse(isSuccess)
        }
    }

    @Test
    fun testResolveAbilityCheck() {
        val (isSuccess, result) = engine.resolveAbilityCheck(dc = 12, modifier = 3)
        if (result.total >= 12) {
            assertTrue(isSuccess)
        } else {
            assertFalse(isSuccess)
        }
    }

    @Test
    fun testCalculateDamage() {
        val result = engine.calculateDamage("1d8", modifier = 3)
        assertTrue(result.total in 4..11)
        assertEquals(3, result.modifier)
    }

    @Test
    fun testCalculateCriticalDamage() {
        // Critical damage should double the dice
        val result = engine.calculateCriticalDamage("1d8", modifier = 3)
        assertEquals(2, result.rolls.size)
        assertTrue(result.total in 5..19) // (2*1 + 3) to (2*8 + 3)
    }

    @Test
    fun testApplyDamage() {
        val character = CharacterEntity(
            name = "Test",
            species = "Human",
            characterClass = "Fighter",
            background = "Soldier",
            originFeat = "Tough",
            alignment = "Neutral",
            level = 1,
            experiencePoints = 0,
            strength = 10, dexterity = 10, constitution = 10,
            intelligence = 10, wisdom = 10, charisma = 10,
            currentHp = 10, maxHp = 10
        )
        
        val damaged = engine.applyDamage(character, 4)
        assertEquals(6, damaged.currentHp)
        
        val overDamaged = engine.applyDamage(character, 15)
        assertEquals(0, overDamaged.currentHp)
    }

    @Test
    fun testApplyDamageWithTempHp() {
        val character = CharacterEntity(
            name = "Test",
            species = "Human",
            characterClass = "Fighter",
            background = "Soldier",
            originFeat = "Tough",
            alignment = "Neutral",
            level = 1,
            experiencePoints = 0,
            strength = 10, dexterity = 10, constitution = 10,
            intelligence = 10, wisdom = 10, charisma = 10,
            currentHp = 10, maxHp = 10,
            temporaryHp = 5
        )
        
        val damaged = engine.applyDamage(character, 3)
        assertEquals(10, damaged.currentHp)
        assertEquals(2, damaged.temporaryHp)
        
        val moreDamaged = engine.applyDamage(character, 7)
        assertEquals(8, moreDamaged.currentHp)
        assertEquals(0, moreDamaged.temporaryHp)
    }

    @Test
    fun testHpBuckets() {
        assertEquals(HpBucket.HEALTHY, engine.getHpBucket(10, 10))
        assertEquals(HpBucket.HEALTHY, engine.getHpBucket(8, 10))
        assertEquals(HpBucket.WOUNDED, engine.getHpBucket(7, 10))
        assertEquals(HpBucket.WOUNDED, engine.getHpBucket(5, 10))
        assertEquals(HpBucket.BLOODIED, engine.getHpBucket(4, 10))
        assertEquals(HpBucket.BLOODIED, engine.getHpBucket(2, 10))
        assertEquals(HpBucket.NEAR_DEATH, engine.getHpBucket(1, 10))
        assertEquals(HpBucket.DEAD, engine.getHpBucket(0, 10))
    }

    // Helper extension to call calculateDamage with critical=true more easily if needed
    private fun CombatEngine.calculateCriticalDamage(notation: String, modifier: Int): RollResult {
        return calculateDamage(notation, modifier, isCritical = true)
    }
}
