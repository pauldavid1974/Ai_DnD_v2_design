package com.pauldavid74.ai_dnd.core.rules

enum class HpBucket {
    HEALTHY,    // 100% - 80%
    WOUNDED,    // 79% - 50%
    BLOODIED,   // 49% - 11%
    NEAR_DEATH, // 10% - 1%
    DEAD        // 0%
}

data class AttackOutcome(
    val isHit: Boolean,
    val attackRoll: RollResult,
    val damageRoll: RollResult? = null,
    val targetHpBefore: Int,
    val targetHpAfter: Int,
    val targetHpBucket: HpBucket
)
