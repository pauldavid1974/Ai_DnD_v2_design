package com.pauldavid74.ai_dnd.core.rules

import com.pauldavid74.ai_dnd.core.database.entity.CharacterEntity
import com.pauldavid74.ai_dnd.core.data.repository.SnapshotRepository
import com.pauldavid74.ai_dnd.core.network.model.PlayerIntent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.statemachine.createStateMachine
import ru.nsk.kstatemachine.transition.TransitionParams

sealed interface EncounterEvent : Event {
    object StartEncounter : EncounterEvent
    object NextRound : EncounterEvent
    object NextTurn : EncounterEvent
    data class IntentEvent(val data: PlayerIntent, val character: CharacterEntity) : EncounterEvent
    data class ValidationPassedEvent(val data: PlayerIntent) : EncounterEvent
    data class AbilityCheckEvent(val data: PlayerIntent, val skillType: String, val dc: Int, val modifier: Int) : EncounterEvent
    data class AttackRollEvent(val data: PlayerIntent, val targetId: String, val weaponId: String, val modifier: Int) : EncounterEvent
    data class SpellRollEvent(val data: PlayerIntent, val spellId: String, val targetIds: List<String>, val level: Int, val dc: Int) : EncounterEvent
    data class AdjudicationResolvedEvent(val data: PlayerIntent, val adjudication: com.pauldavid74.ai_dnd.core.domain.factory.AdjudicationResult) : EncounterEvent
    data class ErrorEvent(val reason: String) : EncounterEvent
    object InterruptEvent : EncounterEvent
    object ReactionResolvedEvent : EncounterEvent
    object ActionResolved : EncounterEvent
    object EncounterEnded : EncounterEvent
    object RollbackRequested : EncounterEvent
}

class EncounterStateMachine(
    private val scope: CoroutineScope,
    private val actionValidator: ActionValidator,
    private val resourceValidator: ResourceValidator,
    private val snapshotRepository: SnapshotRepository,
    private val reactionHandler: ReactionHandler,
    private val combatEngine: CombatEngine
) {
    lateinit var stateMachine: StateMachine
    
    private val _adjudicationResults = MutableSharedFlow<com.pauldavid74.ai_dnd.core.domain.factory.AdjudicationResult>()
    val adjudicationResults: SharedFlow<com.pauldavid74.ai_dnd.core.domain.factory.AdjudicationResult> = _adjudicationResults.asSharedFlow()

    var latestAdjudication: com.pauldavid74.ai_dnd.core.domain.factory.AdjudicationResult = com.pauldavid74.ai_dnd.core.domain.factory.AdjudicationResult.None
        private set

    fun resetAdjudication() {
        latestAdjudication = com.pauldavid74.ai_dnd.core.domain.factory.AdjudicationResult.None
    }

    suspend fun start() {
        stateMachine = createStateMachine(scope, "EncounterStateMachine") {
            logger = StateMachine.Logger { println(it) }

            val encounterState = state("CombatEncounter") {
                val roundState = state("CombatRound") {
                    val turnState = state("EntityTurn") {
                        onEntry {
                            println("Entering EntityTurn")
                            snapshotRepository.takeSnapshot()
                        }
                    }
                    setInitialState(turnState)
                    
                    transition<EncounterEvent.NextTurn>(targetState = turnState)
                }
                setInitialState(roundState)
                
                transition<EncounterEvent.NextRound>(targetState = roundState)
            }

            val turnState = encounterState.findState("CombatRound")!!.findState("EntityTurn")!! as State
            val resolutionState = state("ActionResolution")
            val abilityCheckResolution = state("AbilityCheckResolution")
            val combatResolution = state("CombatResolution")
            val bounceState = state("BounceState")
            val damageCalculationState = state("DamageCalculation")
            val awaitingInterruptResolution = damageCalculationState.state("AwaitingInterruptResolution")

            val rollbackState = state("Rollback") {
                onEntry {
                    this@EncounterStateMachine.scope.launch {
                        snapshotRepository.rollback()
                        this@createStateMachine.processEvent(EncounterEvent.ActionResolved)
                    }
                }
                transition<EncounterEvent.ActionResolved>(targetState = turnState)
            }

            val validationState = state("ValidationState") {
                onEntry { params ->
                    val event = params.event as? EncounterEvent.IntentEvent
                    if (event == null) {
                        this@createStateMachine.processEvent(EncounterEvent.ErrorEvent("No intent found"))
                        return@onEntry
                    }
                    
                    val intent = event.data
                    val character = event.character
                    val actorId = "player1" // Simplified
                    
                    this@EncounterStateMachine.scope.launch {
                        if (!resourceValidator.canAfford(actorId, intent)) {
                            this@createStateMachine.processEvent(EncounterEvent.ErrorEvent("Insufficient resources"))
                            return@launch
                        }

                        val validation = actionValidator.validate(actorId, intent)
                        when (validation) {
                            is ValidationResult.Success -> {
                                this@createStateMachine.processEvent(EncounterEvent.ValidationPassedEvent(intent))
                            }
                            is ValidationResult.Failure -> {
                                this@createStateMachine.processEvent(EncounterEvent.ErrorEvent(validation.reason))
                            }
                            is ValidationResult.RequiresRoll -> {
                                val modifier = calculateModifier(character, validation.skillType)
                                this@createStateMachine.processEvent(EncounterEvent.AbilityCheckEvent(intent, validation.skillType, validation.dc, modifier))
                            }
                            is ValidationResult.RequiresAttackRoll -> {
                                val modifier = character.strengthModifier + character.proficiencyBonus // Simplified
                                this@createStateMachine.processEvent(EncounterEvent.AttackRollEvent(intent, validation.targetId, validation.weaponId, modifier))
                            }
                            is ValidationResult.RequiresSpellRoll -> {
                                val dc = 8 + character.intelligenceModifier + character.proficiencyBonus // Simplified
                                this@createStateMachine.processEvent(EncounterEvent.SpellRollEvent(intent, validation.spellId, validation.targetIds, validation.level, dc))
                            }
                        }
                    }
                }

                transition<EncounterEvent.ValidationPassedEvent>(targetState = resolutionState)
                transition<EncounterEvent.AbilityCheckEvent>(targetState = abilityCheckResolution)
                transition<EncounterEvent.AttackRollEvent>(targetState = combatResolution)
                transition<EncounterEvent.SpellRollEvent>(targetState = combatResolution)
                transition<EncounterEvent.ErrorEvent>(targetState = bounceState)
            }

            abilityCheckResolution.apply {
                onEntry { params ->
                    val event = params.event as? EncounterEvent.AbilityCheckEvent ?: return@onEntry
                    
                    this@EncounterStateMachine.scope.launch {
                        val adjudication = combatEngine.resolveAbilityCheck(event.dc, event.modifier)
                        
                        latestAdjudication = adjudication
                        _adjudicationResults.emit(adjudication)
                        this@createStateMachine.processEvent(EncounterEvent.AdjudicationResolvedEvent(event.data, adjudication))
                    }
                }
                
                transition<EncounterEvent.AdjudicationResolvedEvent>(targetState = resolutionState)
            }

            combatResolution.apply {
                onEntry { params ->
                    this@EncounterStateMachine.scope.launch {
                        val event = params.event
                        val adjudication = when (event) {
                            is EncounterEvent.AttackRollEvent -> {
                                // Real implementation would query DAO for target AC
                                val targetAc = 12 
                                combatEngine.resolveAttack(targetAc, event.modifier) 
                            }
                            is EncounterEvent.SpellRollEvent -> {
                                combatEngine.resolveSavingThrow(event.dc, 2) // Target modifier?
                            }
                            else -> com.pauldavid74.ai_dnd.core.domain.factory.AdjudicationResult.None
                        }
                        
                        latestAdjudication = adjudication
                        _adjudicationResults.emit(adjudication)
                        val originalIntent = (event as? EncounterEvent.AttackRollEvent)?.data
                            ?: (event as? EncounterEvent.SpellRollEvent)?.data
                            ?: throw IllegalStateException("No intent in combat resolution")

                        this@createStateMachine.processEvent(EncounterEvent.AdjudicationResolvedEvent(originalIntent, adjudication))
                    }
                }
                
                transition<EncounterEvent.AdjudicationResolvedEvent>(targetState = resolutionState)
            }

            turnState.transition<EncounterEvent.IntentEvent>(targetState = validationState)

            resolutionState.apply {
                onEntry {
                    println("Resolving action")
                }
                
                transition<EncounterEvent.InterruptEvent> {
                    targetState = damageCalculationState
                }
                
                transition<EncounterEvent.ActionResolved> {
                    targetState = turnState
                }
            }

            damageCalculationState.apply {
                awaitingInterruptResolution.onEntry {
                    println("Awaiting reaction resolution...")
                }
                
                transition<EncounterEvent.ReactionResolvedEvent> {
                    targetState = resolutionState
                }
            }

            bounceState.apply {
                onEntry {
                    println("Bouncing impossible action")
                    this@EncounterStateMachine.scope.launch {
                        this@createStateMachine.processEvent(EncounterEvent.ActionResolved)
                    }
                }
                transition<EncounterEvent.ActionResolved>(targetState = turnState)
            }

            setInitialState(encounterState)

            encounterState.transition<EncounterEvent.RollbackRequested>(targetState = rollbackState)

            val endState = finalState("End")
            transition<EncounterEvent.EncounterEnded>(targetState = endState)
        }
    }

    private fun calculateModifier(character: CharacterEntity, skillType: String): Int {
        return when (skillType.lowercase()) {
            "athletics" -> character.strengthModifier
            "acrobatics", "sleight_of_hand", "stealth" -> character.dexterityModifier
            "arcana", "history", "investigation", "nature", "religion" -> character.intelligenceModifier
            "animal_handling", "insight", "medicine", "perception", "survival" -> character.wisdomModifier
            "deception", "intimidation", "performance", "persuasion" -> character.charismaModifier
            else -> 0
        } + character.proficiencyBonus
    }
}
