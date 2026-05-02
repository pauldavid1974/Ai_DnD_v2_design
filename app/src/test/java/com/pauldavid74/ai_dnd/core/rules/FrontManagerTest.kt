package com.pauldavid74.ai_dnd.core.rules

import com.pauldavid74.ai_dnd.core.domain.model.Front
import com.pauldavid74.ai_dnd.core.domain.model.GrimPortent
import org.junit.Assert.*
import org.junit.Test

class FrontManagerTest {

    @Test
    fun `advanceClock does not trigger portent if hours passed is low`() {
        val front = Front("1", "Threat", "Desc", "Doom", listOf(GrimPortent("p1", "Portent")))
        val updated = FrontManager.advanceClock(listOf(front), 4)
        assertFalse(updated.first().portents.first().isTriggered)
    }

    @Test
    fun `triggerPortent updates specific portent`() {
        val front = Front("1", "Threat", "Desc", "Doom", listOf(GrimPortent("p1", "Portent")))
        val updated = FrontManager.triggerPortent(front, "p1")
        assertTrue(updated.portents.first().isTriggered)
    }

    @Test
    fun `isDoomImminent returns true when all portents triggered`() {
        val front = Front("1", "Threat", "Desc", "Doom", listOf(GrimPortent("p1", "Portent", isTriggered = true)))
        assertTrue(FrontManager.isDoomImminent(front))
    }
}
