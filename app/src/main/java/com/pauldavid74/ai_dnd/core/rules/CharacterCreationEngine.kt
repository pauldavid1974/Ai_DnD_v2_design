package com.pauldavid74.ai_dnd.core.rules

object CharacterCreationEngine {

    val standardArray = listOf(15, 14, 13, 12, 10, 8)

    private val pointBuyCosts = mapOf(
        8 to 0,
        9 to 1,
        10 to 2,
        11 to 3,
        12 to 4,
        13 to 5,
        14 to 7,
        15 to 9
    )

    fun calculatePointBuyCost(scores: List<Int>): Int {
        return scores.sumOf { pointBuyCosts[it] ?: 0 }
    }

    fun isPointBuyValid(scores: List<Int>, maxPoints: Int = 27): Boolean {
        if (scores.size != 6) return false
        if (scores.any { it < 8 || it > 15 }) return false
        return calculatePointBuyCost(scores) <= maxPoints
    }

    fun calculateInitialHp(hitDie: Int, constitutionModifier: Int): Int {
        return hitDie + constitutionModifier
    }

    fun calculateLevelUpHp(hitDie: Int, constitutionModifier: Int): Int {
        return (hitDie / 2) + 1 + constitutionModifier
    }
}
