package com.pauldavid74.ai_dnd.core.rules

import com.pauldavid74.ai_dnd.core.domain.model.Front
import com.pauldavid74.ai_dnd.core.domain.model.GrimPortent
import kotlin.random.Random

object FrontManager {

    fun triggerPortent(front: Front, portentId: String): Front {
        val updatedPortents = front.portents.map {
            if (it.id == portentId) it.copy(isTriggered = true) else it
        }
        return front.copy(portents = updatedPortents)
    }

    fun isDoomImminent(front: Front): Boolean {
        // Doom is imminent if all portents are triggered
        return front.portents.isNotEmpty() && front.portents.all { it.isTriggered }
    }

    /**
     * Advances the world clock and checks if any portents trigger.
     * PRD Section 3.2: Grim Portents advance based on in-game time.
     */
    fun advanceClock(fronts: List<Front>, hoursPassed: Int): List<Front> {
        return fronts.map { front ->
            // Simple logic: 20% chance to trigger next portent if 8+ hours pass
            if (hoursPassed >= 8 && Random.nextFloat() < 0.2f) {
                val nextPortent = front.portents.firstOrNull { !it.isTriggered }
                if (nextPortent != null) {
                    triggerPortent(front, nextPortent.id)
                } else front
            } else front
        }
    }
}
