package com.pauldavid74.ai_dnd.core.data.repository

import com.pauldavid74.ai_dnd.core.network.AiProvider
import com.pauldavid74.ai_dnd.core.network.model.AiModel
import com.pauldavid74.ai_dnd.core.security.KeyManager
import io.ktor.client.*
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AiProviderRepositoryTest {

    private lateinit var repository: AiProviderRepository
    private val openAiProvider = mockk<AiProvider>()
    private val anthropicProvider = mockk<AiProvider>()
    private val keyManager = mockk<KeyManager>()
    private val httpClient = mockk<HttpClient>()
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setup() {
        every { openAiProvider.id } returns "openai"
        every { anthropicProvider.id } returns "anthropic"
        
        repository = AiProviderRepositoryImpl(
            providers = setOf(openAiProvider, anthropicProvider),
            keyManager = keyManager,
            httpClient = httpClient,
            json = json
        )
    }

    @Test
    fun `getAvailableModels routes to correct provider and uses API key`() = runTest {
        val apiKey = "sk-test-key"
        val models = listOf(AiModel("gpt-4", "GPT-4", "openai"))
        
        every { keyManager.getApiKey("openai") } returns apiKey
        coEvery { openAiProvider.getAvailableModels(apiKey) } returns models
        
        val result = repository.getAvailableModels("openai")
        
        assertEquals(models, result)
    }

    @Test
    fun `getAvailableModels returns empty list if key is missing`() = runTest {
        every { keyManager.getApiKey("openai") } returns null
        
        val result = repository.getAvailableModels("openai")
        
        assertTrue(result.isEmpty())
    }

    @Test
    fun `streamChat routes to correct provider and uses API key`() = runTest {
        val apiKey = "sk-test-key"
        val expectedFlow = flowOf("Hello", " world")
        
        every { keyManager.getApiKey("anthropic") } returns apiKey
        coEvery { anthropicProvider.streamChat(apiKey, "claude-3", "Hi") } returns expectedFlow
        
        val resultFlow = repository.streamChat("anthropic", "claude-3", "Hi")
        val resultList = resultFlow.toList()
        
        assertEquals(listOf("Hello", " world"), resultList)
    }

    @Test
    fun `hasKey returns true if key exists`() {
        every { keyManager.getApiKey("groq") } returns "some-key"
        assertTrue(repository.hasKey("groq"))
    }
}
