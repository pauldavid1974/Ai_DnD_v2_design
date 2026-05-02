package com.pauldavid74.ai_dnd.core.data.repository

import com.pauldavid74.ai_dnd.core.database.entity.CharacterEntity
import com.pauldavid74.ai_dnd.core.domain.factory.PromptFactory
import com.pauldavid74.ai_dnd.core.network.model.CastSpellIntent
import com.pauldavid74.ai_dnd.core.network.model.PlayerIntent
import com.pauldavid74.ai_dnd.core.security.KeyManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IntentExtractorTest {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val aiRepository = mockk<AiProviderRepository>()
    private val keyManager = mockk<KeyManager>(relaxed = true)
    private val promptFactory = mockk<PromptFactory>(relaxed = true)
    private val character = mockk<CharacterEntity>(relaxed = true)

    @Test
    fun `test decoding PlayerIntent`() {
        val jsonString = "{\"type\":\"cast_spell\",\"spellId\":\"fireball\",\"targetNodes\":[\"enemy1\"],\"castLevel\":3,\"originNode\":\"center\",\"impossibilityScore\":10}"
        val intent = json.decodeFromString<PlayerIntent>(jsonString)
        assertTrue(intent is CastSpellIntent)
        assertEquals("fireball", (intent as CastSpellIntent).spellId)
    }

    @Test
    fun `extractIntent should aggregate chunks and emit intent`() = runTest {
        // Arrange
        val jsonString = "{\"type\":\"cast_spell\",\"spellId\":\"fireball\",\"targetNodes\":[\"enemy1\"],\"castLevel\":3,\"originNode\":\"center\",\"impossibilityScore\":10}"
        coEvery { aiRepository.streamChat(any(), any(), any()) } returns flowOf(jsonString)
        
        val extractor = IntentExtractorImpl(aiRepository, keyManager, promptFactory, json)

        // Act
        val intents = extractor.extractIntent("I cast fireball", character, emptyList()).toList()

        // Assert
        assertEquals(1, intents.size)
        val intent = intents[0] as CastSpellIntent
        assertEquals("fireball", intent.spellId)
        assertEquals(10, intent.impossibilityScore)
    }

    @Test(expected = IntentExtractionException::class)
    fun `extractIntent should throw exception if impossibilityScore is high`() = runTest {
        // Arrange
        val jsonString = "{\"type\":\"melee_attack\",\"weaponId\":\"longsword\",\"targetNode\":\"enemy1\",\"impossibilityScore\":90}"
        coEvery { aiRepository.streamChat(any(), any(), any()) } returns flowOf(jsonString)
        
        val extractor = IntentExtractorImpl(aiRepository, keyManager, promptFactory, json)

        // Act
        extractor.extractIntent("I jump to the moon", character, emptyList()).toList()
    }
}
