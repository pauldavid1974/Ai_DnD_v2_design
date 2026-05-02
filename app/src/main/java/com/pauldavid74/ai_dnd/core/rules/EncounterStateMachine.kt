package com.pauldavid74.ai_dnd.core.rules

import com.pauldavid74.ai_dnd.core.data.repository.SnapshotRepository
import com.pauldavid74.ai_dnd.core.network.model.PlayerIntent
import kotlinx.coroutines.CoroutineScope
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
    data class IntentEvent(val data: PlayerIntent) : EncounterEvent
    data class ValidationPassedEvent(val data: PlayerIntent) : EncounterEvent
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
    private val reactionHandler: ReactionHandler
) {
    lateinit var stateMachine: StateMachine

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
            val bounceState = state("BounceState")
            val damageCalculationState = state("DamageCalculation")
            val awaitingInterruptResolution = damageCalculationState.state("AwaitingInterruptResolution")

            val validationState = state("ValidationState") {
                onEntry { params ->
                    val event = params.event as? EncounterEvent.IntentEvent
                    if (event == null) {
                        this@createStateMachine.processEvent(EncounterEvent.ErrorEvent("No intent found"))
                        return@onEntry
                    }
                    
                    val intent = event.data
                    val actorId = "player1" // Simplified
                    
                    this@EncounterStateMachine.scope.launch {
                        if (!resourceValidator.canAfford(actorId, intent)) {
                            this@createStateMachine.processEvent(EncounterEvent.ErrorEvent("Insufficient resources"))
                            return@launch
                        }

                        val validation = actionValidator.validate(actorId, intent)
                        if (validation is ValidationResult.Success) {
                            this@createStateMachine.processEvent(EncounterEvent.ValidationPassedEvent(intent))
                        } else {
                            this@createStateMachine.processEvent(EncounterEvent.ErrorEvent((validation as ValidationResult.Failure).reason))
                        }
                    }
                }

                transition<EncounterEvent.ValidationPassedEvent>(targetState = resolutionState)
                transition<EncounterEvent.ErrorEvent>(targetState = bounceState)
            }

            turnState.transition<EncounterEvent.IntentEvent>(targetState = validationState)

            resolutionState.apply {
                onEntry {
                    println("Resolving action")
                    this@EncounterStateMachine.scope.launch {
                        reactionHandler.broadcastTrigger(ReactionTrigger.DamageTaken("target1", 10))
                    }
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

            bounceState.onEntry {
                println("Bouncing impossible action")
            }

            setInitialState(encounterState)

            encounterState.transition<EncounterEvent.RollbackRequested> {
                // Simplified rollback trigger
                this@EncounterStateMachine.scope.launch {
                    snapshotRepository.rollback()
                }
            }

            val endState = finalState("End")
            transition<EncounterEvent.EncounterEnded>(targetState = endState)
        }
    }
}
