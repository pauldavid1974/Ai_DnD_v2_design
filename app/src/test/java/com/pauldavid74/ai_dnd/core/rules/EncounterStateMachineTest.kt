package com.pauldavid74.ai_dnd.core.rules

import com.pauldavid74.ai_dnd.core.network.model.MeleeAttackIntent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.statemachine.*
import org.junit.Assert.assertEquals
import org.junit.Test
import ru.nsk.kstatemachine.event.Event

@OptIn(ExperimentalCoroutinesApi::class)
class EncounterStateMachineTest {

    @Test
    fun `test state transitions`() = runTest {
        val encounterStateMachine = EncounterStateMachine(this)
        encounterStateMachine.start()
        val machine = encounterStateMachine.stateMachine

        assertEquals("CombatEncounter", machine.activeStates().first().name)
        
        // Navigate to EntityTurn
        machine.processEvent(EncounterEvent.NextRound)
        machine.processEvent(EncounterEvent.NextTurn)
        
        assertTrue(machine.activeStates().any { it.name == "EntityTurn" })

        // Trigger Intent
        val intent = MeleeAttackIntent("sword", "enemy1")
        machine.processEvent(EncounterEvent.IntentEvent(intent))
        
        assertTrue(machine.activeStates().any { it.name == "ActionResolution" })

        // Resolve Action
        machine.processEvent(EncounterEvent.ActionResolved)
        assertTrue(machine.activeStates().any { it.name == "EntityTurn" })

        // End Encounter
        machine.processEvent(EncounterEvent.EncounterEnded)
        assertTrue(machine.activeStates().any { it.name == "End" })
    }
    
    private fun assertTrue(condition: Boolean) = org.junit.Assert.assertTrue(condition)
}
