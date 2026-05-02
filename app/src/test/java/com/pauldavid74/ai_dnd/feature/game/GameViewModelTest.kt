package com.pauldavid74.ai_dnd.feature.game

import com.pauldavid74.ai_dnd.core.data.repository.AiProviderRepository
import com.pauldavid74.ai_dnd.core.data.repository.GameRepository
import com.pauldavid74.ai_dnd.core.data.repository.IntentExtractor
import com.pauldavid74.ai_dnd.core.data.repository.SnapshotRepository
import com.pauldavid74.ai_dnd.core.database.entity.CharacterEntity
import com.pauldavid74.ai_dnd.core.network.model.MeleeAttackIntent
import com.pauldavid74.ai_dnd.core.domain.factory.Chronicler
import com.pauldavid74.ai_dnd.core.domain.factory.PromptFactory
import com.pauldavid74.ai_dnd.core.rules.*
import com.pauldavid74.ai_dnd.core.security.KeyManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val gameRepository = mockk<GameRepository>(relaxed = true)
    private val aiRepository = mockk<AiProviderRepository>(relaxed = true)
    private val keyManager = mockk<KeyManager>(relaxed = true)
    private val promptFactory = mockk<PromptFactory>(relaxed = true)
    private val diceEngine = DiceEngine()
    private val combatEngine = CombatEngine()
    private val chronicler = mockk<Chronicler>(relaxed = true)
    private val intentExtractor = mockk<IntentExtractor>(relaxed = true)
    private val snapshotRepository = mockk<SnapshotRepository>(relaxed = true)
    private val actionValidator = mockk<ActionValidator>(relaxed = true)
    private val resourceValidator = mockk<ResourceValidator>(relaxed = true)
    private val reactionHandler = mockk<ReactionHandler>(relaxed = true)
    private val json = Json { ignoreUnknownKeys = true }

    private lateinit var viewModel: GameViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        every { keyManager.getActiveProvider() } returns "openai"
        every { keyManager.getActiveModel(any()) } returns "gpt-4"
        every { gameRepository.getAllMemories() } returns flowOf(emptyList())
        
        viewModel = GameViewModel(
            gameRepository, aiRepository, keyManager, promptFactory, chronicler, diceEngine, 
            combatEngine, intentExtractor, snapshotRepository, actionValidator, 
            resourceValidator, reactionHandler, json
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test viewModel creation`() {
        // No-op
    }

    @Test
    fun `onSendMessage transitions through states and updates messages`() = runTest {
        val character = CharacterEntity(
            name = "Grog", 
            species = "Half-Orc",
            characterClass = "Barbarian", 
            background = "Outlander",
            originFeat = "Tough",
            alignment = "Chaotic Neutral",
            level = 1, experiencePoints = 0,
            strength = 16, dexterity = 10, constitution = 14, intelligence = 8, wisdom = 10, charisma = 8,
            currentHp = 14, maxHp = 14
        )
        
        coEvery { gameRepository.getCharacter(1L) } returns character
        coEvery { gameRepository.getMemory(any()) } returns null
        coEvery { gameRepository.getFrontsForCampaign(any()) } returns flowOf(emptyList())
        coEvery { gameRepository.getChatMessages(any()) } returns flowOf(emptyList())
        viewModel.loadCampaign(1L)

        val meleeIntent = MeleeAttackIntent("sword", "enemy1")
        coEvery { intentExtractor.extractIntent(any()) } returns flowOf(meleeIntent)
        coEvery { actionValidator.validate(any(), any()) } returns ValidationResult.Success
        coEvery { resourceValidator.canAfford(any(), any()) } returns true
        
        val outcomeJson = """{"final_narration": "Something happens."}"""

        every { promptFactory.createOutcomePrompt(any(), any(), any()) } returns "outcome-prompt"
        coEvery { aiRepository.streamChat(any(), any(), "outcome-prompt") } returns flowOf(outcomeJson)

        viewModel.onSendMessage("I walk north")
        testDispatcher.scheduler.advanceUntilIdle()

        val messages = viewModel.uiState.value.chatMessages
        assertEquals(3, messages.size)
        assertEquals("I walk north", messages[1].content)
        assertTrue(messages[2].content.contains("Something happens."))
        assertEquals(GameUiStatus.AwaitingInput, viewModel.uiState.value.uiStatus)
    }
}
