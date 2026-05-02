package com.pauldavid74.ai_dnd.feature.game

import com.pauldavid74.ai_dnd.core.database.entity.CharacterEntity
import com.pauldavid74.ai_dnd.core.network.model.UiChoice

data class GameState(
    val character: CharacterEntity? = null,
    val chatMessages: List<ChatMessage> = emptyList(),
    val uiStatus: GameUiStatus = GameUiStatus.AwaitingInput,
    val diceResult: String? = null,
    val availableChoices: List<UiChoice> = emptyList(),
    val kineticEffect: KineticEffect? = null,
    val streamingMessage: ChatMessage? = null,
    val error: String? = null
)

sealed class KineticEffect {
    object HeavyThud : KineticEffect()
    object LightClick : KineticEffect()
    object SuccessCrescendo : KineticEffect()
    object StatusWobble : KineticEffect()
    object GlitchUpdate : KineticEffect()
}

sealed class GameUiStatus {
    object AwaitingInput : GameUiStatus()
    object DeducingIntent : GameUiStatus()
    object AdjudicatingMath : GameUiStatus()
    object GeneratingOutcome : GameUiStatus()
    object Chronicling : GameUiStatus()
}

data class ChatMessage(
    val sender: MessageSender,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageSender {
    USER, AI, SYSTEM
}
