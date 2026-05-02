package com.pauldavid74.ai_dnd.core.rules

import org.junit.Assert.*
import org.junit.Test

class DiceEngineTest {
    private val engine = DiceEngine()

    @Test
    fun testRollNormalRange() {
        val roll = DiceRoll(1, 20)
        repeat(100) {
            val result = engine.roll(roll)
            assertTrue(result.total in 1..20)
            assertEquals(1, result.rolls.size)
        }
    }

    @Test
    fun testRollWithModifier() {
        val roll = DiceRoll(2, 6, 5)
        repeat(100) {
            val result = engine.roll(roll)
            assertTrue(result.total in 7..17) // (2*1 + 5) to (2*6 + 5)
            assertEquals(2, result.rolls.size)
            assertEquals(5, result.modifier)
        }
    }

    @Test
    fun testCriticalSuccess() {
        // Since we use SecureRandom, we can't easily force a 20 without mocking.
        // But we can check that if it rolls a 20, isCriticalSuccess is true.
        // For 1d20, it should be true if roll is 20.
        val roll = DiceRoll(1, 20)
        var sawCrit = false
        repeat(1000) {
            val result = engine.roll(roll)
            if (result.rolls[0] == 20) {
                assertTrue(result.isCriticalSuccess)
                sawCrit = true
            } else {
                assertFalse(result.isCriticalSuccess)
            }
        }
        assertTrue("Should have seen at least one crit in 1000 rolls", sawCrit)
    }

    @Test
    fun testCriticalFailure() {
        val roll = DiceRoll(1, 20)
        var sawFail = false
        repeat(1000) {
            val result = engine.roll(roll)
            if (result.rolls[0] == 1) {
                assertTrue(result.isCriticalFailure)
                sawFail = true
            } else {
                assertFalse(result.isCriticalFailure)
            }
        }
        assertTrue("Should have seen at least one crit fail in 1000 rolls", sawFail)
    }

    @Test
    fun testAdvantage() {
        val roll = DiceRoll(1, 20, mode = RollMode.Advantage)
        repeat(100) {
            val result = engine.roll(roll)
            assertEquals(1, result.rolls.size)
            assertTrue(result.total in 1..20)
        }
    }
}
