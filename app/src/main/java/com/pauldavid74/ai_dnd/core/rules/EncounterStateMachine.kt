package com.pauldavid74.ai_dnd.core.rules

import com.pauldavid74.ai_dnd.core.network.model.PlayerIntent
import kotlinx.coroutines.CoroutineScope
import ru.nsk.kstatemachine.event.DataEvent
import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.statemachine.createStateMachine

sealed interface EncounterEvent : Event {
    object StartEncounter : EncounterEvent
    object NextRound : EncounterEvent
    object NextTurn : EncounterEvent
    data class IntentEvent(override val data: PlayerIntent) : DataEvent<PlayerIntent>, EncounterEvent
    object ActionResolved : EncounterEvent
    object EncounterEnded : EncounterEvent
}

class EncounterStateMachine(
    private val scope: CoroutineScope
) {
    lateinit var stateMachine: StateMachine

    suspend fun start() {
        stateMachine = createStateMachine(scope, "EncounterStateMachine") {
            logger = StateMachine.Logger { println(it) }

            // Using standard state for now due to DataState compatibility issues in this version
            // We will manually extract the intent data in the transition
            val resolutionState = state("ActionResolution")

            val encounterState = state("CombatEncounter") {
                val roundState = state("CombatRound") {
                    val turnState = state("EntityTurn") {
                        onEntry {
                            println("Entering EntityTurn")
                        }
                    }
                    setInitialState(turnState)

                    transition<EncounterEvent.NextTurn>(targetState = turnState)
                }
                setInitialState(roundState)

                transition<EncounterEvent.NextRound>(targetState = roundState)
            }

            // Transition from EntityTurn to ActionResolution
            val turnState = encounterState.findState("CombatRound")!!.findState("EntityTurn")!! as State
            turnState.transition<EncounterEvent.IntentEvent>(targetState = resolutionState)

            resolutionState.apply {
                onEntry {
                    println("Resolving action")
                }
                transition<EncounterEvent.ActionResolved> {
                    targetState = turnState
                }
            }

            setInitialState(encounterState)

            val endState = finalState("End")
            transition<EncounterEvent.EncounterEnded>(targetState = endState)
        }
    }
}
