package com.pauldavid74.ai_dnd.core.rules

import com.pauldavid74.ai_dnd.core.network.model.MeleeAttackIntent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import com.pauldavid74.ai_dnd.core.data.repository.SnapshotRepository
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.statemachine.*

@OptIn(ExperimentalCoroutinesApi::class)
class EncounterStateMachineTest {
    private val actionValidator = mockk<ActionValidator>(relaxed = true)
    private val resourceValidator = mockk<ResourceValidator>(relaxed = true)
    private val snapshotRepository = mockk<SnapshotRepository>(relaxed = true)

    @Test
    fun `test state transitions`() = runTest {
        val encounterStateMachine = EncounterStateMachine(
            this, actionValidator, resourceValidator, snapshotRepository
        )
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
        
        // Wait for validation to finish (since it launches a coroutine)
        machine.processEvent(EncounterEvent.ValidationPassedEvent(intent))
        
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
