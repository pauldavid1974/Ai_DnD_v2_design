package com.pauldavid74.ai_dnd.feature.game

import com.pauldavid74.ai_dnd.core.data.repository.AiProviderRepository
import com.pauldavid74.ai_dnd.core.data.repository.GameRepository
import com.pauldavid74.ai_dnd.core.database.entity.CharacterEntity
import com.pauldavid74.ai_dnd.core.domain.factory.Chronicler
import com.pauldavid74.ai_dnd.core.domain.factory.PromptFactory
import com.pauldavid74.ai_dnd.core.network.model.IntentDeductionResponse
import com.pauldavid74.ai_dnd.core.rules.CombatEngine
import com.pauldavid74.ai_dnd.core.rules.DiceEngine
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val gameRepository = mockk<GameRepository>()
    private val aiRepository = mockk<AiProviderRepository>()
    private val keyManager = mockk<KeyManager>()
    private val promptFactory = mockk<PromptFactory>()
    private val diceEngine = DiceEngine()
    private val combatEngine = CombatEngine()
    private val chronicler = mockk<Chronicler>(relaxed = true)
    private val json = Json { ignoreUnknownKeys = true }

    private lateinit var viewModel: GameViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        every { keyManager.getActiveProvider() } returns "openai"
        every { keyManager.getActiveModel(any()) } returns "gpt-4"
        
        viewModel = GameViewModel(
            gameRepository, aiRepository, keyManager, promptFactory, chronicler, diceEngine, combatEngine, json
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
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
        viewModel.loadCampaign(1L)

        val intentJson = """{"intent_detected": true, "mechanic_type": "none", "narration_prefix": "You do something."}"""
        val outcomeJson = """{"final_narration": "Something happens."}"""

        every { promptFactory.createIntentPrompt(any(), any(), any(), any(), any(), any()) } returns "intent-prompt"
        coEvery { aiRepository.streamChat(any(), any(), "intent-prompt") } returns flowOf(intentJson)
        
        every { promptFactory.createOutcomePrompt(any(), any(), any()) } returns "outcome-prompt"
        coEvery { aiRepository.streamChat(any(), any(), "outcome-prompt") } returns flowOf(outcomeJson)

        viewModel.onSendMessage("I walk north")

        assertEquals(2, viewModel.uiState.value.chatMessages.size)
        assertEquals("I walk north", viewModel.uiState.value.chatMessages[0].content)
        assertEquals("Something happens.", viewModel.uiState.value.chatMessages[1].content)
        assertEquals(GameUiStatus.AwaitingInput, viewModel.uiState.value.uiStatus)
    }
}
