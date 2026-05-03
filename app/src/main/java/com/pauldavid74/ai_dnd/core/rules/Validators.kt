package com.pauldavid74.ai_dnd.core.rules

import com.pauldavid74.ai_dnd.core.database.dao.CharacterDao
import com.pauldavid74.ai_dnd.core.database.dao.EntityNodeDao
import com.pauldavid74.ai_dnd.core.network.model.CastSpellIntent
import kotlinx.coroutines.flow.first
import com.pauldavid74.ai_dnd.core.network.model.ImprovisedActionIntent
import com.pauldavid74.ai_dnd.core.network.model.MeleeAttackIntent
import com.pauldavid74.ai_dnd.core.network.model.MoveIntent
import com.pauldavid74.ai_dnd.core.network.model.PlayerIntent
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

interface ActionValidator {
    suspend fun validate(actorId: String, intent: PlayerIntent): ValidationResult
}

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Failure(val reason: String) : ValidationResult()
    data class RequiresRoll(val skillType: String, val dc: Int) : ValidationResult()
    data class RequiresAttackRoll(val targetId: String, val weaponId: String) : ValidationResult()
    data class RequiresSpellRoll(val spellId: String, val targetIds: List<String>, val level: Int) : ValidationResult()
}

@Singleton
class ActionValidatorImpl @Inject constructor(
    private val entityNodeDao: EntityNodeDao,
    private val characterDao: CharacterDao
) : ActionValidator {

    override suspend fun validate(actorId: String, intent: PlayerIntent): ValidationResult {
        val actorNode = entityNodeDao.getEntityById(actorId) ?: return ValidationResult.Failure("Actor node not found")

        return when (intent) {
            is MeleeAttackIntent -> {
                val target = entityNodeDao.getEntityById(intent.targetNode) ?: return ValidationResult.Failure("Target not found")
                val distance = calculateDistance(actorNode.x, actorNode.y, target.x, target.y)
                if (distance > 5.0) { // Standard 5ft melee range
                    ValidationResult.Failure("Target too far: $distance ft")
                } else {
                    ValidationResult.RequiresAttackRoll(intent.targetNode, intent.weaponId)
                }
            }
            is CastSpellIntent -> {
                // Fetch CharacterEntity to check known spells
                // For MVP, we assume the actorId matches the character's long ID if it can be parsed, 
                // or we use a more robust mapping if available. 
                // Since actorId is "player1" in EncounterStateMachine, we need to handle this.
                val character = characterDao.getAllCharacters().first().find { it.name == actorNode.name }
                    ?: return ValidationResult.RequiresSpellRoll(intent.spellId, intent.targetNodes, intent.castLevel)

                val knownSpells = character.spells.map { it.lowercase() }
                if (!knownSpells.contains(intent.spellId.lowercase())) {
                    ValidationResult.Failure("You do not have the spell '${intent.spellId}' prepared or known.")
                } else {
                    ValidationResult.RequiresSpellRoll(intent.spellId, intent.targetNodes, intent.castLevel)
                }
            }
            is MoveIntent -> {
                ValidationResult.Success
            }
            is ImprovisedActionIntent -> {
                if (intent.requiresCheck && intent.skillType != null && intent.dc != null) {
                    ValidationResult.RequiresRoll(intent.skillType, intent.dc)
                } else {
                    ValidationResult.Success
                }
            }
        }
    }

    private fun calculateDistance(x1: Int, y1: Int, x2: Int, y2: Int): Double {
        return sqrt(((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)).toDouble())
    }
}

interface ResourceValidator {
    suspend fun canAfford(actorId: String, intent: PlayerIntent): Boolean
}

@Singleton
class ResourceValidatorImpl @Inject constructor(
    private val entityNodeDao: EntityNodeDao
) : ResourceValidator {
    override suspend fun canAfford(actorId: String, intent: PlayerIntent): Boolean {
        val actor = entityNodeDao.getEntityById(actorId) ?: return false
        
        return when (intent) {
            is CastSpellIntent -> {
                // In a real implementation, we'd check character's spell slots from CharacterEntity
                // For now, check the leveledSpellCastThisTurn flag on EntityNode for SRD 5.2.1 compliance
                if (intent.castLevel > 0 && actor.leveledSpellCastThisTurn) {
                    false
                } else {
                    true
                }
            }
            else -> true
        }
    }
}
