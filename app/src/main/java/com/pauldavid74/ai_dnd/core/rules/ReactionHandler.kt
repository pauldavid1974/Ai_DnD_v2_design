package com.pauldavid74.ai_dnd.core.rules

import com.pauldavid74.ai_dnd.core.database.dao.EntityNodeDao
import com.pauldavid74.ai_dnd.core.database.entity.EntityNode
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed class ReactionTrigger {
    data class DamageTaken(val targetId: String, val amount: Int) : ReactionTrigger()
    data class SpellCast(val casterId: String, val spellId: String) : ReactionTrigger()
}

data class ReactionRequest(
    val entityId: String,
    val trigger: ReactionTrigger,
    val availableReactions: List<String>
)

interface ReactionHandler {
    val reactionRequests: SharedFlow<ReactionRequest>
    suspend fun broadcastTrigger(trigger: ReactionTrigger)
    suspend fun resolveReaction(entityId: String, reactionId: String?)
}

@Singleton
class ReactionHandlerImpl @Inject constructor(
    private val entityNodeDao: EntityNodeDao
) : ReactionHandler {

    private val _reactionRequests = MutableSharedFlow<ReactionRequest>()
    override val reactionRequests = _reactionRequests.asSharedFlow()

    override suspend fun broadcastTrigger(trigger: ReactionTrigger) {
        // Query database for entities with unspent reactions and valid triggers
        // For MVP, we simulate finding valid reactors
        val potentialReactors = findPotentialReactors(trigger)
        
        potentialReactors.forEach { reactor ->
            if (canReact(reactor, trigger)) {
                _reactionRequests.emit(
                    ReactionRequest(
                        entityId = reactor.id,
                        trigger = trigger,
                        availableReactions = listOf("Shield", "Counterspell", "Opportunity Attack") // Simplified
                    )
                )
            }
        }
    }

    private suspend fun findPotentialReactors(trigger: ReactionTrigger): List<EntityNode> {
        // In real implementation, query EntityNodeDao for entities within range
        return emptyList() // Placeholder
    }

    private fun canReact(entity: EntityNode, trigger: ReactionTrigger): Boolean {
        if (entity.hasUsedReaction) return false
        
        // SRD 5.2.1: Check "One Spell Slot Per Turn" limit
        if (trigger is ReactionTrigger.SpellCast && entity.leveledSpellCastThisTurn) {
            // Can't use reaction spell if already cast a leveled spell on their own turn
            // Wait, the rule is "you can expend only ONE spell slot to cast a spell" PER TURN.
            // This applies even to reactions on your own turn.
            return false 
        }
        
        return true
    }

    override suspend fun resolveReaction(entityId: String, reactionId: String?) {
        // Update database: mark reaction as used
        val entity = entityNodeDao.getEntityById(entityId) ?: return
        entityNodeDao.upsertEntity(entity.copy(hasUsedReaction = true))
        
        println("Resolved reaction for $entityId: $reactionId")
    }
}
