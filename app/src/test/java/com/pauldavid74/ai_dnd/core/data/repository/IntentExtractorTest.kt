package com.pauldavid74.ai_dnd.core.data.repository

import com.pauldavid74.ai_dnd.core.network.model.CastSpellIntent
import com.pauldavid74.ai_dnd.core.network.model.PlayerIntent
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.utils.io.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
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
        val mockEngine = MockEngine { _ ->
            respond(
                content = "data: {\"type\":\"cast_spell\",\"spellId\":\"fireball\",\"targetNodes\":[\"enemy1\"],\"castLevel\":3,\"originNode\":\"center\",\"impossibilityScore\":10}\ndata: [DONE]\n",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/event-stream")
            )
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(json)
            }
        }
        val extractor = IntentExtractorImpl(client, json)

        // Act
        val intents = extractor.extractIntent("I cast fireball").toList()

        // Assert
        assertEquals(1, intents.size)
        val intent = intents[0] as CastSpellIntent
        assertEquals("fireball", intent.spellId)
        assertEquals(10, intent.impossibilityScore)
    }

    @Test(expected = IntentExtractionException::class)
    fun `extractIntent should throw exception if impossibilityScore is high`() = runTest {
        // Arrange
        val mockEngine = MockEngine { _ ->
            respond(
                content = "data: {\"type\":\"melee_attack\",\"weaponId\":\"longsword\",\"targetNode\":\"enemy1\",\"impossibilityScore\":90}\ndata: [DONE]\n",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/event-stream")
            )
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(json)
            }
        }
        val extractor = IntentExtractorImpl(client, json)

        // Act
        extractor.extractIntent("I jump to the moon").toList()
    }
}
