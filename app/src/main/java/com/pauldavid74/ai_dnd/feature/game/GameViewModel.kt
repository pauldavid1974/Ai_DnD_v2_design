package com.pauldavid74.ai_dnd.feature.game

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pauldavid74.ai_dnd.core.data.repository.AiProviderRepository
import com.pauldavid74.ai_dnd.core.data.repository.GameRepository
import com.pauldavid74.ai_dnd.core.security.KeyManager
import com.pauldavid74.ai_dnd.core.database.entity.ChatMessageEntity
import com.pauldavid74.ai_dnd.core.domain.factory.AdjudicationResult
import com.pauldavid74.ai_dnd.core.domain.factory.Chronicler
import com.pauldavid74.ai_dnd.core.domain.factory.PromptFactory
import com.pauldavid74.ai_dnd.core.network.model.GenerativeOutcomeResponse
import com.pauldavid74.ai_dnd.core.rules.CombatEngine
import com.pauldavid74.ai_dnd.core.rules.DiceEngine
import com.pauldavid74.ai_dnd.core.rules.DiceRoll
import com.pauldavid74.ai_dnd.core.data.repository.IntentExtractor
import com.pauldavid74.ai_dnd.core.data.repository.SnapshotRepository
import com.pauldavid74.ai_dnd.core.rules.*
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.statemachine.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class GameViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val aiRepository: AiProviderRepository,
    private val keyManager: KeyManager,
    private val promptFactory: PromptFactory,
    private val chronicler: Chronicler,
    private val diceEngine: DiceEngine,
    private val combatEngine: CombatEngine,
    private val intentExtractor: IntentExtractor,
    private val snapshotRepository: SnapshotRepository,
    private val actionValidator: ActionValidator,
    private val resourceValidator: ResourceValidator,
    private val reactionHandler: ReactionHandler,
    private val json: Json
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameState())
    val uiState: StateFlow<GameState> = _uiState.asStateFlow()

    private val _rollHistory = MutableStateFlow<List<String>>(emptyList())

    private var turnCount = 0

    private val encounterStateMachine = EncounterStateMachine(
        viewModelScope, actionValidator, resourceValidator, snapshotRepository, reactionHandler
    )

    init {
        viewModelScope.launch {
            encounterStateMachine.start()
            observeStateMachine()
        }
        
        viewModelScope.launch {
            gameRepository.getAllMemories().collect { memories ->
                _uiState.update { it.copy(sessionMemories = memories) }
            }
        }
        viewModelScope.launch {
            _rollHistory.collect { history ->
                _uiState.update { it.copy(rollHistory = history) }
            }
        }
    }

    private suspend fun observeStateMachine() {
        encounterStateMachine.stateMachine.activeStatesFlow().collect { states ->
            val status = when {
                states.any { it.name == "EntityTurn" } -> GameUiStatus.AwaitingInput
                states.any { it.name == "ValidationState" } -> GameUiStatus.DeducingIntent
                states.any { it.name == "ActionResolution" } -> {
                    // Trigger Phase C
                    triggerGenerativeOutcome()
                    GameUiStatus.AdjudicatingMath
                }
                states.any { it.name == "AwaitingInterruptResolution" } -> GameUiStatus.AwaitingReaction
                else -> GameUiStatus.AwaitingInput
            }
            _uiState.update { it.copy(uiStatus = status) }
        }
    }

    private fun triggerGenerativeOutcome() {
        viewModelScope.launch {
            try {
                if (_uiState.value.character == null) return@launch
                
                _uiState.update { it.copy(uiStatus = GameUiStatus.GeneratingOutcome) }

                val providerId = keyManager.getActiveProvider() ?: "openai"
                val modelId = keyManager.getActiveModel(providerId) ?: "gpt-4"
                
                // Simplified outcome generation logic for Phase 6
                val outcomePrompt = promptFactory.createOutcomePrompt(AdjudicationResult.None, "The action proceeds.", null)
                
                var fullResponse = ""
                aiRepository.streamChat(providerId, modelId, outcomePrompt).collect { chunk ->
                    fullResponse += chunk
                    _uiState.update { it.copy(
                        streamingMessage = ChatMessage(MessageSender.AI, fullResponse)
                    )}
                }
                
                val finalMsg = ChatMessage(MessageSender.AI, fullResponse)
                _uiState.update { state ->
                    state.copy(
                        chatMessages = state.chatMessages + finalMsg,
                        streamingMessage = null
                    )
                }
                persistChatMessage(finalMsg)
                
                encounterStateMachine.stateMachine.processEvent(EncounterEvent.ActionResolved)
                
                turnCount++
                if (turnCount >= 5) {
                    turnCount = 0
                    val currentMessages = _uiState.value.chatMessages
                    chronicler.chronicleSession(currentMessages)
                }
            } catch (e: Exception) {
                Log.e("GameViewModel", "Outcome generation error", e)
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun loadCampaign(characterId: Long) {
        viewModelScope.launch {
            val character = gameRepository.getCharacter(characterId)
            _uiState.update { it.copy(character = character) }
            
            // Restore chat history
            gameRepository.getChatMessages(characterId).first().let { savedMessages ->
                if (savedMessages.isNotEmpty()) {
                    val chatMessages = savedMessages.map { entity ->
                        ChatMessage(entity.sender, entity.content, entity.timestamp)
                    }
                    _uiState.update { it.copy(chatMessages = chatMessages) }
                } else {
                    startAdventure(character!!)
                }
            }
        }
    }

    private fun startAdventure(character: com.pauldavid74.ai_dnd.core.database.entity.CharacterEntity) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(uiStatus = GameUiStatus.GeneratingOutcome) }
                val providerId = keyManager.getActiveProvider() ?: "openai"
                val modelId = keyManager.getActiveModel(providerId) ?: "gpt-4"
                val introPrompt = promptFactory.createIntroPrompt(character)
                
                var introText = ""
                aiRepository.streamChat(providerId, modelId, introPrompt).collect { chunk ->
                    introText += chunk
                    _uiState.update { it.copy(
                        streamingMessage = ChatMessage(MessageSender.AI, introText)
                    )}
                }
                
                val finalIntro = ChatMessage(MessageSender.AI, introText)
                _uiState.update { state ->
                    state.copy(
                        chatMessages = state.chatMessages + finalIntro,
                        streamingMessage = null,
                        uiStatus = GameUiStatus.AwaitingInput
                    )
                }
                persistChatMessage(finalIntro)
            } catch (e: Exception) {
                Log.e("GameViewModel", "Error starting adventure", e)
                _uiState.update { it.copy(uiStatus = GameUiStatus.AwaitingInput, error = "Failed to start adventure: ${e.message}") }
            }
        }
    }

    private fun persistChatMessage(message: ChatMessage) {
        val characterId = _uiState.value.character?.id ?: return
        viewModelScope.launch {
            gameRepository.saveChatMessage(
                ChatMessageEntity(
                    characterId = characterId,
                    sender = message.sender,
                    content = message.content,
                    timestamp = message.timestamp
                )
            )
        }
    }

    fun rollDice(count: Int, sides: Int, modifier: Int = 0) {
        val diceRoll = DiceRoll(count, sides, modifier)
        val result = diceEngine.roll(diceRoll)
        val modStr = if (modifier == 0) "" else if (modifier > 0) "+$modifier" else modifier.toString()
        val notation = "${count}d$sides$modStr"
        val resultString = "$notation -> ${result.rolls}${if(modifier != 0) " $modStr" else ""} = ${result.total}"
        
        _rollHistory.update { (listOf(resultString) + it).take(20) }

        val chatRollMessage = ChatMessage(
            sender = MessageSender.SYSTEM,
            content = "DICE_ROLL:$resultString"
        )
        _uiState.update { it.copy(chatMessages = it.chatMessages + chatRollMessage) }
        persistChatMessage(chatRollMessage)
    }

    fun onSendMessage(text: String) {
        if (text.isBlank() || _uiState.value.uiStatus != GameUiStatus.AwaitingInput) return

        val userMsg = ChatMessage(MessageSender.USER, text)
        _uiState.update { state ->
            state.copy(
                chatMessages = state.chatMessages + userMsg
            )
        }
        persistChatMessage(userMsg)
        
        viewModelScope.launch {
            // Process Phase A
            intentExtractor.extractIntent(text).collect { intent ->
                encounterStateMachine.stateMachine.processEvent(EncounterEvent.IntentEvent(intent))
                // Proceed with Two-Call Cycle Phase B/C
                // Phase B is handled by the state machine
            }
        }
    }

    fun onUndo() {
        viewModelScope.launch {
            encounterStateMachine.stateMachine.processEvent(EncounterEvent.RollbackRequested)
        }
    }

    fun onEffectConsumed() {
        _uiState.update { it.copy(kineticEffect = null) }
    }
}
