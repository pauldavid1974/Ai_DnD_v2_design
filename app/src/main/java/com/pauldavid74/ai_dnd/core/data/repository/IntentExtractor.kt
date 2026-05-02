package com.pauldavid74.ai_dnd.core.data.repository

import com.pauldavid74.ai_dnd.core.network.model.PlayerIntent
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

import io.ktor.http.*

class IntentExtractionException(message: String) : Exception(message)

interface IntentExtractor {
    suspend fun extractIntent(playerText: String): Flow<PlayerIntent>
}

@Singleton
class IntentExtractorImpl @Inject constructor(
    private val httpClient: HttpClient,
    private val json: Json
) : IntentExtractor {

    private val jsonConfig = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    override suspend fun extractIntent(playerText: String): Flow<PlayerIntent> = flow {
        val response = try {
            httpClient.post("https://api.example.com/v1/intent") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("text" to playerText))
            }
        } catch (e: Exception) {
            println("Request failed: $e")
            throw e
        }

        val channel = response.bodyAsChannel()
        val buffer = StringBuilder()

        while (!channel.isClosedForRead) {
            val line = channel.readUTF8Line() ?: break
            if (line.isBlank()) continue
            
            if (line.startsWith("data: ")) {
                val data = line.substring(6).trim()
                if (data == "[DONE]") break
                
                buffer.append(data)
                
                try {
                    val currentJson = buffer.toString()
                    val intent = jsonConfig.decodeFromString<PlayerIntent>(currentJson)
                    
                    if (intent.impossibilityScore > 85) {
                        throw IntentExtractionException("Action is mechanically impossible (Score: ${intent.impossibilityScore})")
                    }
                    
                    emit(intent)
                    buffer.clear()
                } catch (e: Exception) {
                    if (e is IntentExtractionException) throw e
                    continue
                }
            }
        }
    }
}
