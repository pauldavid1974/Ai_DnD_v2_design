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
import com.pauldavid74.ai_dnd.core.network.model.IntentDeductionResponse
import com.pauldavid74.ai_dnd.core.network.model.GenerativeOutcomeResponse
import com.pauldavid74.ai_dnd.core.rules.CombatEngine
import com.pauldavid74.ai_dnd.core.rules.DiceEngine
import com.pauldavid74.ai_dnd.core.rules.DiceRoll
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
    private val json: Json
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameState())
    val uiState: StateFlow<GameState> = _uiState.asStateFlow()

    private val _rollHistory = MutableStateFlow<List<String>>(emptyList())

    private var turnCount = 0

    private var stateMachine: StateMachine? = null

    init {
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

    fun rollDice(count: Int, sides: Int, modifier: Int = 0) {
        val diceRoll = com.pauldavid74.ai_dnd.core.rules.DiceRoll(count, sides, modifier)
        val result = diceEngine.roll(diceRoll)
        val modStr = if (modifier == 0) "" else if (modifier > 0) "+$modifier" else "$modifier"
        val notation = "${count}d${sides}$modStr"
        val resultString = "$notation -> ${result.rolls}${if(modifier != 0) " $modStr" else ""} = ${result.total}"
        
        // Add to roll history for the Character Sheet tab
        _rollHistory.update { (listOf(resultString) + it).take(20) }

        // ALSO: Push a system message to the chat history for immediate visibility
        val chatRollMessage = ChatMessage(
            sender = MessageSender.SYSTEM,
            content = "DICE_ROLL:$resultString"
        )
        _uiState.update { it.copy(chatMessages = it.chatMessages + chatRollMessage) }
        persistChatMessage(chatRollMessage)
    }

    init {
        viewModelScope.launch {
            stateMachine = createStateMachine(viewModelScope) {
                val awaitingInput = addInitialState(DefaultState("AwaitingInput"))
                val deducingIntent = addState(DefaultState("DeducingIntent"))
                val adjudicatingMath = addState(DefaultState("AdjudicatingMath"))
                val generatingOutcome = addState(DefaultState("GeneratingOutcome"))
                val chronicling = addState(DefaultState("Chronicling"))

                awaitingInput {
                    transition<GameEvent.SendMessage> { targetState = deducingIntent }
                }
                deducingIntent {
                    transition<GameEvent.IntentDeducted> { targetState = adjudicatingMath }
                    transition<GameEvent.ErrorOccurred> { targetState = awaitingInput }
                }
                adjudicatingMath {
                    transition<GameEvent.MathAdjudicated> { targetState = generatingOutcome }
                }
                generatingOutcome {
                    transition<GameEvent.OutcomeGenerated> { targetState = awaitingInput }
                    transition<GameEvent.ChronicleTriggered> { targetState = chronicling }
                }
                chronicling {
                    transition<GameEvent.OutcomeGenerated> { targetState = awaitingInput }
                }

                // We'll use State listeners instead
                awaitingInput {
                    onEntry { _uiState.update { it.copy(uiStatus = GameUiStatus.AwaitingInput) } }
                }
                deducingIntent {
                    onEntry { _uiState.update { it.copy(uiStatus = GameUiStatus.DeducingIntent) } }
                }
                adjudicatingMath {
                    onEntry { _uiState.update { it.copy(uiStatus = GameUiStatus.AdjudicatingMath) } }
                }
                generatingOutcome {
                    onEntry { _uiState.update { it.copy(uiStatus = GameUiStatus.GeneratingOutcome) } }
                }
                chronicling {
                    onEntry { _uiState.update { it.copy(uiStatus = GameUiStatus.Chronicling) } }
                }
            }
        }
    }

    fun loadCampaign(characterId: Long) {
        viewModelScope.launch {
            val character = gameRepository.getCharacter(characterId)
            _uiState.update { it.copy(character = character) }
            
            // Restore chat history
            val savedMessages = gameRepository.getChatMessages(characterId).first()
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
            stateMachine?.processEvent(GameEvent.SendMessage(text))
            executeTwoCallCycle(text)
        }
    }

    private fun executeTwoCallCycle(userInput: String) {
        viewModelScope.launch {
            try {
                Log.d("GameViewModel", "Executing Two-Call Cycle for: $userInput")
                val character = _uiState.value.character ?: run {
                    Log.e("GameViewModel", "No character loaded! Aborting cycle.")
                    return@launch
                }
                val chatHistory = _uiState.value.chatMessages
                    .filter { it.sender != MessageSender.SYSTEM }
                    .takeLast(6)
                    .chunked(2)
                    .map { Pair(it.getOrNull(0)?.content ?: "", it.getOrNull(1)?.content ?: "") }

                // Call 1: Intent Deduction
                val providerId = keyManager.getActiveProvider() ?: "openai"
                val modelId = keyManager.getActiveModel(providerId) ?: "gpt-4"
                
                Log.d("GameViewModel", "Call 1: Intent Deduction using $providerId ($modelId)")

                // JIT Memory Injection
                val contextText = userInput + chatHistory.joinToString { it.first + it.second }
                val entities = detectEntities(contextText)
                val relevantMemories = entities.mapNotNull { key -> 
                    gameRepository.getMemory(key) 
                }
                
                // Active Fronts
                val activeFronts = gameRepository.getFrontsForCampaign("default").first().map {
                    com.pauldavid74.ai_dnd.core.domain.model.Front(it.id, it.name, it.description, it.doom, it.portents)
                }

                val intentPrompt = promptFactory.createIntentPrompt(character, null, chatHistory, relevantMemories, activeFronts, userInput)
                var intentJson = ""
                aiRepository.streamChat(providerId, modelId, intentPrompt).collect { chunk ->
                    intentJson += chunk
                }
                
                if (intentJson.isEmpty()) {
                    Log.e("GameViewModel", "Intent JSON is empty! Check API key for $providerId")
                    throw Exception("Failed to deduce intent. Is your $providerId API key set in Settings?")
                }

                Log.d("GameViewModel", "Received Intent JSON: $intentJson")
                val sanitizedIntent = intentJson.trim()
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()
                val intent = json.decodeFromString<IntentDeductionResponse>(sanitizedIntent)
                _uiState.update { it.copy(kineticEffect = KineticEffect.LightClick) }
                stateMachine?.processEvent(GameEvent.IntentDeducted)

                // Intercept: Adjudication
                val adjudication = adjudicate(intent)
                val summary = adjudication.getSummary()
                Log.d("GameViewModel", "Adjudication complete: $summary")
                
                // Add AI's dice roll to chat history
                if (summary.isNotEmpty()) {
                    val chatRollMessage = ChatMessage(
                        sender = MessageSender.SYSTEM,
                        content = "DICE_ROLL:$summary"
                    )
                    _uiState.update { it.copy(chatMessages = it.chatMessages + chatRollMessage) }
                    persistChatMessage(chatRollMessage)
                }

                // Wiki Lookups
                val wikiContext = if (intent.wikiLookups.isNotEmpty()) {
                    intent.wikiLookups.mapNotNull { gameRepository.getSrdReference(it)?.contentJson }
                        .joinToString("\n")
                } else null

                _uiState.update { it.copy(
                    diceResult = summary,
                    kineticEffect = if (adjudication is AdjudicationResult.Hit) KineticEffect.HeavyThud else KineticEffect.LightClick
                ) }
                stateMachine?.processEvent(GameEvent.MathAdjudicated)
                
                // Call 2: Generative Outcome
                val activeProviderId = keyManager.getActiveProvider() ?: "openai"
                val activeModelId = keyManager.getActiveModel(activeProviderId) ?: "gpt-4"
                
                Log.d("GameViewModel", "Call 2: Generative Outcome using $activeProviderId ($activeModelId)")
                val outcomePrompt = promptFactory.createOutcomePrompt(adjudication, intent.narrationPrefix, wikiContext)
                
                var fullResponse = ""
                aiRepository.streamChat(activeProviderId, activeModelId, outcomePrompt).collect { chunk ->
                    fullResponse += chunk
                    Log.v("GameViewModel", "Outcome chunk: $chunk")
                    
                    // Attempt to extract narration on the fly
                    val narrationMatch = Regex("\"final_narration\"\\s*:\\s*\"(.*?)(?<!\\\\)\"").find(fullResponse)
                    if (narrationMatch != null) {
                        val currentNarration = narrationMatch.groupValues[1]
                            .replace("\\\"", "\"")
                            .replace("\\n", "\n")
                        _uiState.update { it.copy(
                            streamingMessage = ChatMessage(MessageSender.AI, currentNarration)
                        )}
                    } else {
                        val partialMatch = Regex("\"final_narration\"\\s*:\\s*\"(.*)").find(fullResponse)
                        if (partialMatch != null) {
                            val partial = partialMatch.groupValues[1]
                                .replace("\\\"", "\"")
                                .replace("\\n", "\n")
                            _uiState.update { it.copy(
                                streamingMessage = ChatMessage(MessageSender.AI, partial)
                            )}
                        }
                    }
                }
                
                if (fullResponse.isEmpty()) {
                    Log.e("GameViewModel", "Outcome response is empty!")
                    throw Exception("Failed to generate outcome.")
                }

                Log.d("GameViewModel", "Received Outcome JSON: $fullResponse")
                try {
                    val sanitizedOutcome = fullResponse.trim()
                        .removePrefix("```json")
                        .removePrefix("```")
                        .removeSuffix("```")
                        .trim()
                    val outcome = json.decodeFromString<GenerativeOutcomeResponse>(sanitizedOutcome)
                    val finalMsg = ChatMessage(MessageSender.AI, outcome.finalNarration)
                    _uiState.update { state ->
                        state.copy(
                            chatMessages = state.chatMessages + finalMsg,
                            streamingMessage = null,
                            availableChoices = outcome.uiChoices,
                            diceResult = null,
                            kineticEffect = when (outcome.hapticTrigger) {
                                "bounce" -> KineticEffect.HeavyThud
                                "expand" -> KineticEffect.SuccessCrescendo
                                "wobble" -> KineticEffect.StatusWobble
                                else -> null
                            }
                        )
                    }
                    persistChatMessage(finalMsg)
                    stateMachine?.processEvent(GameEvent.OutcomeGenerated)
                    checkChronicler()
                } catch (e: Exception) {
                    Log.e("GameViewModel", "JSON Parsing Error: ${e.message}")
                    val fallbackMsg = ChatMessage(MessageSender.AI, fullResponse)
                     _uiState.update { it.copy(
                        chatMessages = it.chatMessages + fallbackMsg,
                        streamingMessage = null
                    )}
                    persistChatMessage(fallbackMsg)
                    stateMachine?.processEvent(GameEvent.OutcomeGenerated)
                    checkChronicler()
                }

            } catch (e: Exception) {
                Log.e("GameViewModel", "Two-Call Cycle Error", e)
                stateMachine?.processEvent(GameEvent.ErrorOccurred)
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    private fun checkChronicler() {
        turnCount++
        if (turnCount >= 5) {
            turnCount = 0
            viewModelScope.launch {
                val currentMessages = _uiState.value.chatMessages
                stateMachine?.processEvent(GameEvent.ChronicleTriggered)
                _uiState.update { it.copy(kineticEffect = KineticEffect.GlitchUpdate) }
                chronicler.chronicleSession(currentMessages)
                stateMachine?.processEvent(GameEvent.OutcomeGenerated)
            }
        }
    }

    fun onEffectConsumed() {
        _uiState.update { it.copy(kineticEffect = null) }
    }

    private fun detectEntities(text: String): List<String> {
        // Simple regex to find capitalized words (Named Entities)
        val regex = Regex("\\b[A-Z][a-z]+\\b")
        return regex.findAll(text).map { it.value }.distinct().toList()
    }

    private suspend fun adjudicate(intent: IntentDeductionResponse): AdjudicationResult {
        val character = _uiState.value.character!!
        return when (intent.mechanicType) {
            "attack_roll" -> {
                val mod = when(intent.statRequired) {
                    "str" -> character.strengthModifier
                    "dex" -> character.dexterityModifier
                    else -> character.strengthModifier
                }
                val (hit, roll) = combatEngine.resolveAttack(intent.difficultyClass ?: 10, mod + character.proficiencyBonus)
                if (hit) {
                    val damage = combatEngine.calculateDamage("1d8", mod)
                    if (intent.targetId == "player") {
                        val updatedChar = combatEngine.applyDamage(character, damage.total)
                        gameRepository.updateCharacter(updatedChar)
                        _uiState.update { it.copy(character = updatedChar) }
                    }
                    AdjudicationResult.Hit(damage.total, "wounded", roll.total)
                } else {
                    AdjudicationResult.Miss(roll.total)
                }
            }
            "saving_throw" -> {
                val mod = when(intent.statRequired) {
                    "str" -> character.strengthModifier
                    "dex" -> character.dexterityModifier
                    "con" -> character.constitutionModifier
                    "int" -> character.intelligenceModifier
                    "wis" -> character.wisdomModifier
                    "cha" -> character.charismaModifier
                    else -> 0
                }
                val (success, roll) = combatEngine.resolveSavingThrow(intent.difficultyClass ?: 10, mod + character.proficiencyBonus)
                if (success) {
                    AdjudicationResult.Success(roll.total, intent.difficultyClass ?: 10)
                } else {
                    val damage = combatEngine.calculateDamage("1d6", 0)
                    val updatedChar = combatEngine.applyDamage(character, damage.total)
                    gameRepository.updateCharacter(updatedChar)
                    _uiState.update { it.copy(character = updatedChar) }
                    AdjudicationResult.Failure(roll.total, intent.difficultyClass ?: 10)
                }
            }
            "skill_check" -> {
                 val mod = when(intent.statRequired) {
                    "str" -> character.strengthModifier
                    "dex" -> character.dexterityModifier
                    "int" -> character.intelligenceModifier
                    "wis" -> character.wisdomModifier
                    "cha" -> character.charismaModifier
                    else -> 0
                }
                val roll = diceEngine.roll(DiceRoll(1, 20, mod))
                val dc = intent.difficultyClass ?: 10
                if (roll.total >= dc) AdjudicationResult.Success(roll.total, dc) 
                else AdjudicationResult.Failure(roll.total, dc)
            }
            else -> {
                Log.d("GameViewModel", "Adjudication: None (Type: ${intent.mechanicType})")
                AdjudicationResult.None
            }
        }
    }
}
