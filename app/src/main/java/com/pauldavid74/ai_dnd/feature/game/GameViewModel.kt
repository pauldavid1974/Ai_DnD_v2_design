package com.pauldavid74.ai_dnd.feature.game

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pauldavid74.ai_dnd.core.data.repository.AiProviderRepository
import com.pauldavid74.ai_dnd.core.data.repository.GameRepository
import com.pauldavid74.ai_dnd.core.security.KeyManager
import com.pauldavid74.ai_dnd.core.database.dao.EntityNodeDao
import com.pauldavid74.ai_dnd.core.database.entity.EntityNode
import com.pauldavid74.ai_dnd.core.database.entity.ChatMessageEntity
import com.pauldavid74.ai_dnd.core.domain.factory.AdjudicationResult
import com.pauldavid74.ai_dnd.core.domain.factory.Chronicler
import com.pauldavid74.ai_dnd.core.domain.factory.PromptFactory
import com.pauldavid74.ai_dnd.core.network.model.GenerativeOutcomeResponse
import com.pauldavid74.ai_dnd.core.rules.CombatEngine
import com.pauldavid74.ai_dnd.core.rules.DiceEngine
import com.pauldavid74.ai_dnd.core.rules.DiceRoll
import com.pauldavid74.ai_dnd.core.data.repository.IntentExtractor
import com.pauldavid74.ai_dnd.core.data.repository.IntentExtractionException
import com.pauldavid74.ai_dnd.core.data.repository.SnapshotRepository
import com.pauldavid74.ai_dnd.core.network.model.PlayerIntent
import com.pauldavid74.ai_dnd.core.rules.*
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.statemachine.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
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
    private val entityNodeDao: EntityNodeDao,
    private val json: Json
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameState())
    val uiState: StateFlow<GameState> = _uiState.asStateFlow()

    private val _rollHistory = MutableStateFlow<List<String>>(emptyList())

    private var turnCount = 0
    private var lastDucedIntent: PlayerIntent? = null

    private val encounterStateMachine = EncounterStateMachine(
        viewModelScope, actionValidator, resourceValidator, snapshotRepository, reactionHandler, combatEngine
    )

    init {
        viewModelScope.launch {
            encounterStateMachine.start()
            observeStateMachine()
        }

        viewModelScope.launch {
            encounterStateMachine.adjudicationResults.collect { result ->
                _uiState.update { it.copy(diceResult = result.getSummary()) }
                
                // Also add to chat history as a system message
                val chatRollMessage = ChatMessage(
                    sender = MessageSender.SYSTEM,
                    content = "DICE_ROLL:${result.getSummary()}"
                )
                _uiState.update { it.copy(chatMessages = it.chatMessages + chatRollMessage) }
                persistChatMessage(chatRollMessage)

                delay(2400) // Match UI animation
                _uiState.update { it.copy(diceResult = null) }
            }
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

    private fun observeChatHistory(characterId: Long) {
        viewModelScope.launch {
            gameRepository.getChatMessages(characterId).collect { savedMessages ->
                val chatMessages = savedMessages.map { entity ->
                    ChatMessage(entity.sender, entity.content, entity.timestamp)
                }
                _uiState.update { it.copy(chatMessages = chatMessages) }
                
                // Update roll history from chat messages
                val historicalRolls = savedMessages
                    .filter { it.content.startsWith("DICE_ROLL:") }
                    .map { it.content.removePrefix("DICE_ROLL:") }
                    .reversed()
                    .take(20)
                
                _rollHistory.update { historicalRolls }
            }
        }
    }

    private suspend fun observeStateMachine() {
        encounterStateMachine.stateMachine.activeStatesFlow().collect { states ->
            val status = when {
                states.any { it.name == "EntityTurn" } -> GameUiStatus.AwaitingInput
                states.any { it.name == "ValidationState" } -> GameUiStatus.DeducingIntent
                states.any { it.name == "AbilityCheckResolution" } -> GameUiStatus.AdjudicatingMath
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
                val currentState = _uiState.value
                if (currentState.character == null) return@launch
                
                _uiState.update { it.copy(uiStatus = GameUiStatus.GeneratingOutcome) }

                val providerId = keyManager.getActiveProvider() ?: "openai"
                val modelId = keyManager.getActiveModel(providerId) ?: "gpt-4"
                
                val prefix = lastDucedIntent?.narrationPrefix ?: "The action proceeds."
                val historyPairs = currentState.chatMessages.filter { it.sender != MessageSender.SYSTEM }
                    .map { (if (it.sender == MessageSender.USER) "User" else "AI") to it.content }

                val outcomePrompt = promptFactory.createOutcomePrompt(
                    character = currentState.character,
                    adjudication = encounterStateMachine.latestAdjudication,
                    narrationPrefix = prefix,
                    chatHistory = historyPairs,
                    wikiContext = null
                )
                
                var accumulated = ""
                aiRepository.streamChat(providerId, modelId, outcomePrompt).collect { chunk ->
                    accumulated += chunk
                    
                    val partialNarration = extractPartialNarration(accumulated)
                    if (partialNarration != null) {
                        _uiState.update { it.copy(
                            streamingMessage = ChatMessage(MessageSender.AI, partialNarration)
                        )}
                    } else if (accumulated.isNotBlank() && !accumulated.trim().startsWith("{")) {
                        // Fallback for non-JSON streaming
                        _uiState.update { it.copy(
                            streamingMessage = ChatMessage(MessageSender.AI, accumulated)
                        )}
                    }
                }
                
                val sanitized = sanitizeJson(accumulated)
                val response = try {
                    json.decodeFromString<GenerativeOutcomeResponse>(sanitized)
                } catch (e: Exception) {
                    Log.w("GameViewModel", "Failed to parse outcome JSON, using raw text fallback", e)
                    GenerativeOutcomeResponse(
                        finalNarration = accumulated.ifBlank { "The DM nods in acknowledgment." }
                    )
                }
                
                val finalMsg = ChatMessage(MessageSender.AI, response.finalNarration)
                
                val effect = when (response.hapticTrigger) {
                    "bounce" -> KineticEffect.HeavyThud
                    "expand" -> KineticEffect.SuccessCrescendo
                    "resist" -> KineticEffect.LightClick
                    "wobble" -> KineticEffect.StatusWobble
                    else -> null
                }

                _uiState.update { state ->
                    state.copy(
                        chatMessages = state.chatMessages + finalMsg,
                        streamingMessage = null,
                        availableChoices = response.uiChoices,
                        kineticEffect = effect ?: state.kineticEffect,
                        uiStatus = GameUiStatus.AwaitingInput
                    )
                }
                persistChatMessage(finalMsg)
                
                encounterStateMachine.resetAdjudication()
                encounterStateMachine.stateMachine.processEvent(EncounterEvent.ActionResolved)
                
                turnCount++
                if (turnCount >= 2) {
                    turnCount = 0
                    val currentMessages = _uiState.value.chatMessages
                    chronicler.chronicleSession(currentMessages)
                }
            } catch (e: Exception) {
                Log.e("GameViewModel", "Outcome generation error", e)
                _uiState.update { it.copy(
                    uiStatus = GameUiStatus.AwaitingInput,
                    error = "The DM lost their train of thought. (${e.localizedMessage})"
                ) }
            }
        }
    }

    private fun sanitizeJson(raw: String): String {
        var cleaned = raw.trim()
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substringAfter("```json")
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substringAfter("```")
        }
        
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substringBeforeLast("```")
        }
        return cleaned.trim()
    }

    private fun extractPartialNarration(raw: String): String? {
        val marker = "\"final_narration\":"
        val index = raw.indexOf(marker)
        if (index == -1) return null
        
        val afterMarker = raw.substring(index + marker.length).trim()
        if (!afterMarker.startsWith("\"")) return null
        
        val contentStart = afterMarker.substring(1)
        // Find the next unescaped quote
        var contentEnd = -1
        var isEscaped = false
        for (i in contentStart.indices) {
            if (contentStart[i] == '\\') {
                isEscaped = !isEscaped
                continue
            }
            if (contentStart[i] == '\"' && !isEscaped) {
                contentEnd = i
                break
            }
            isEscaped = false
        }
        
        return if (contentEnd != -1) {
            contentStart.substring(0, contentEnd).replace("\\\"", "\"")
        } else {
            contentStart.replace("\\\"", "\"") // Still streaming
        }
    }

    fun loadCampaign(characterId: Long) {
        viewModelScope.launch {
            val character = gameRepository.getCharacter(characterId)
            _uiState.update { it.copy(character = character) }
            
            // Register character in spatial engine (Phase B)
            character?.let {
                entityNodeDao.upsertEntity(
                    EntityNode(
                        id = "player1", // Matches EncounterStateMachine's actorId
                        name = it.name,
                        x = 10, // Default starting pos
                        y = 10,
                        hp = it.currentHp,
                        maxHp = it.maxHp,
                        ac = 15, // Simplified
                        entityType = "PLAYER"
                    )
                )
            }

            observeChatHistory(characterId)

            // If history is empty, start intro
            gameRepository.getChatMessages(characterId).first().let { savedMessages ->
                if (savedMessages.isEmpty()) {
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
                
                // Fetch campaign details for context
                val campaign = if (character.campaignId.isNotEmpty()) {
                    gameRepository.getAllCampaigns().firstOrNull()?.find { it.id == character.campaignId }
                } else null

                val introPrompt = promptFactory.createIntroPrompt(character, campaign)
                
                var accumulated = ""
                aiRepository.streamChat(providerId, modelId, introPrompt).collect { chunk ->
                    accumulated += chunk
                    
                    val partialNarration = extractPartialNarration(accumulated)
                    if (partialNarration != null) {
                        _uiState.update { it.copy(
                            streamingMessage = ChatMessage(MessageSender.AI, partialNarration)
                        )}
                    } else if (accumulated.isNotBlank() && !accumulated.trim().startsWith("{")) {
                        _uiState.update { it.copy(
                            streamingMessage = ChatMessage(MessageSender.AI, accumulated)
                        )}
                    }
                }
                
                val sanitized = sanitizeJson(accumulated)
                val response = try {
                    json.decodeFromString<GenerativeOutcomeResponse>(sanitized)
                } catch (e: Exception) {
                    Log.w("GameViewModel", "Failed to parse intro JSON, using raw fallback", e)
                    GenerativeOutcomeResponse(
                        finalNarration = accumulated.ifBlank { "You stand at the beginning of your journey." },
                        uiChoices = listOf(
                            com.pauldavid74.ai_dnd.core.network.model.UiChoice("Look around", "improv"),
                            com.pauldavid74.ai_dnd.core.network.model.UiChoice("Check equipment", "improv")
                        )
                    )
                }
                
                val finalIntro = ChatMessage(MessageSender.AI, response.finalNarration)
                _uiState.update { state ->
                    state.copy(
                        chatMessages = state.chatMessages + finalIntro,
                        streamingMessage = null,
                        availableChoices = response.uiChoices,
                        uiStatus = GameUiStatus.AwaitingInput
                    )
                }
                persistChatMessage(finalIntro)
            } catch (e: Exception) {
                Log.e("GameViewModel", "Error starting adventure", e)
                _uiState.update { state -> 
                    state.copy(
                        uiStatus = GameUiStatus.AwaitingInput, 
                        error = "Failed to start adventure: ${e.message}",
                        availableChoices = listOf(
                            com.pauldavid74.ai_dnd.core.network.model.UiChoice("Try again", "improv")
                        )
                    )
                }
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
                chatMessages = state.chatMessages + userMsg,
                uiStatus = GameUiStatus.DeducingIntent,
                availableChoices = emptyList(), // Clear previous choices
                error = null
            )
        }
        persistChatMessage(userMsg)
        
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                val character = currentState.character ?: return@launch
                
                var intentFound = false
                // Process Phase A
                intentExtractor.extractIntent(
                    playerText = text,
                    character = character,
                    chatHistory = currentState.chatMessages.dropLast(1), // History before this message
                    memories = currentState.sessionMemories
                ).collect { intent ->
                    intentFound = true
                    lastDucedIntent = intent
                    encounterStateMachine.stateMachine.processEvent(EncounterEvent.IntentEvent(intent, character))
                }

                if (!intentFound) {
                    // Fallback: If no mechanical intent is found, treat as improvised action
                    val fallbackIntent = com.pauldavid74.ai_dnd.core.network.model.ImprovisedActionIntent(
                        actionDescription = text,
                        referencedEnvironmentIds = emptyList(),
                        narrationPrefix = "You attempt to $text."
                    )
                    lastDucedIntent = fallbackIntent
                    encounterStateMachine.stateMachine.processEvent(EncounterEvent.IntentEvent(fallbackIntent, character))
                }
            } catch (e: Exception) {
                Log.e("GameViewModel", "Intent extraction failed", e)
                _uiState.update { it.copy(
                    uiStatus = GameUiStatus.AwaitingInput,
                    error = "The DM is having trouble understanding. (${e.localizedMessage})"
                ) }
            }
        }
    }

    fun onUndo() {
        viewModelScope.launch {
            val characterId = _uiState.value.character?.id ?: return@launch
            
            // Delete until we've removed the last User message
            // We'll delete max 5 messages to be safe (AI narration + System Roll + User message)
            var deletedUserMsg = false
            for (i in 1..5) {
                val lastMsg = _uiState.value.chatMessages.lastOrNull() ?: break
                gameRepository.deleteLastMessage(characterId)
                if (lastMsg.sender == MessageSender.USER) {
                    deletedUserMsg = true
                    break
                }
                // Update local state temporarily for the loop
                _uiState.update { it.copy(chatMessages = it.chatMessages.dropLast(1)) }
            }
            
            if (deletedUserMsg) {
                encounterStateMachine.stateMachine.processEvent(EncounterEvent.RollbackRequested)
            }
        }
    }

    fun onEffectConsumed() {
        _uiState.update { it.copy(kineticEffect = null) }
    }

    fun showDetail(name: String, type: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDetailLoading = true, selectedDetail = null) }
            
            val referenceId = "${type.lowercase()}_${name.lowercase().replace(" ", "_")}"
            
            // 1. Try local cache first
            try {
                val existing = gameRepository.getSrdReference(referenceId)
                if (existing != null) {
                    _uiState.update { it.copy(
                        isDetailLoading = false,
                        selectedDetail = DetailInfo(name, type, existing.contentJson)
                    )}
                    return@launch
                }
            } catch (e: Exception) {
                Log.w("GameViewModel", "Failed to query SRD cache", e)
            }

            // 2. AI Fallback
            try {
                val providerId = keyManager.getActiveProvider() ?: "openai"
                val modelId = keyManager.getActiveModel(providerId) ?: "gpt-4"
                val prompt = """
                    SYSTEM: You are a D&D 5.2.1 encyclopedia.
                    TASK: Provide a concise, 1-paragraph mechanical and flavor description for the $type: '$name'.
                    RULE: Be specific about what it does in game terms if applicable.
                """.trimIndent()
                
                var accumulated = ""
                aiRepository.streamChat(providerId, modelId, prompt).collect { chunk ->
                    accumulated += chunk
                }

                val info = DetailInfo(name, type, accumulated.trim())
                
                // Cache it
                gameRepository.seedSrdData(listOf(
                    com.pauldavid74.ai_dnd.core.database.entity.SrdReferenceEntity(
                        id = referenceId,
                        category = type.uppercase(),
                        name = name,
                        contentJson = accumulated.trim()
                    )
                ))

                _uiState.update { it.copy(
                    isDetailLoading = false,
                    selectedDetail = info
                )}
            } catch (e: Exception) {
                Log.e("GameViewModel", "Failed to generate detail", e)
                _uiState.update { it.copy(
                    isDetailLoading = false,
                    error = "Could not find info for $name."
                )}
            }
        }
    }

    fun dismissDetail() {
        _uiState.update { it.copy(selectedDetail = null) }
    }
}
