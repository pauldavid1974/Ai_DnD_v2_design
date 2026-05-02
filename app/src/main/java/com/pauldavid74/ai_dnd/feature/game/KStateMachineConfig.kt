package com.pauldavid74.ai_dnd.feature.game

import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.state.DefaultState
import ru.nsk.kstatemachine.statemachine.createStateMachine
import ru.nsk.kstatemachine.state.transition

sealed class GameEvent : Event {
    data class SendMessage(val text: String) : GameEvent()
    object IntentDeducted : GameEvent()
    object MathAdjudicated : GameEvent()
    object OutcomeGenerated : GameEvent()
    object ChronicleTriggered : GameEvent()
    object ErrorOccurred : GameEvent()
}

// In a real scenario, I'd integrate this with the ViewModel.
// For now, it defines the logic mentioned in the prompt.
