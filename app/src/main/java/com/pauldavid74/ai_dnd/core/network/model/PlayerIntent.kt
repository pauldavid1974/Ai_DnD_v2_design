package com.pauldavid74.ai_dnd.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Polymorphic root for all player actions deduced by the AI (Phase A).
 * Strictly mapped to SRD 5.2.1 mechanics.
 */
@Serializable
sealed class PlayerIntent {
    abstract val impossibilityScore: Int
}

@Serializable
@SerialName("cast_spell")
data class CastSpellIntent(
    val spellId: String,
    val targetNodes: List<String>,
    val castLevel: Int,
    val originNode: String? = null,
    override val impossibilityScore: Int = 0
) : PlayerIntent()

@Serializable
@SerialName("melee_attack")
data class MeleeAttackIntent(
    val weaponId: String,
    val targetNode: String,
    val isFinesse: Boolean = false,
    val isMastery: Boolean = false,
    override val impossibilityScore: Int = 0
) : PlayerIntent()

@Serializable
data class Coordinate(val x: Int, val y: Int)

@Serializable
@SerialName("move")
data class MoveIntent(
    val pathCoordinates: List<Coordinate>,
    val destinationNode: String? = null,
    override val impossibilityScore: Int = 0
) : PlayerIntent()

@Serializable
@SerialName("improvised_action")
data class ImprovisedActionIntent(
    val actionDescription: String,
    val referencedEnvironmentIds: List<String>,
    override val impossibilityScore: Int = 0
) : PlayerIntent()
