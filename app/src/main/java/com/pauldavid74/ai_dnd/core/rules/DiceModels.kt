package com.pauldavid74.ai_dnd.core.rules

sealed class RollMode {
    object Normal : RollMode()
    object Advantage : RollMode()
    object Disadvantage : RollMode()
}

data class DiceRoll(
    val count: Int,
    val sides: Int,
    val modifier: Int = 0,
    val mode: RollMode = RollMode.Normal
)

data class RollResult(
    val rolls: List<Int>,
    val modifier: Int,
    val total: Int,
    val isCriticalSuccess: Boolean = false,
    val isCriticalFailure: Boolean = false
)
