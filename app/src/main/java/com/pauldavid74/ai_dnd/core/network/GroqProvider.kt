package com.pauldavid74.ai_dnd.core.network

import com.pauldavid74.ai_dnd.core.network.model.AiModel
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.plugins.sse.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class GroqProvider(
    private val httpClient: HttpClient,
    private val json: Json
) : AiProvider {
    override val id = "groq"
    override val baseUrl = "https://api.groq.com/openai/v1"

    override suspend fun getAvailableModels(apiKey: String): List<AiModel> {
        val response: GroqModelsResponse = httpClient.get("$baseUrl/models") {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
        }.body()
        return response.data.map { AiModel(it.id, it.id, id) }
    }

    override suspend fun streamChat(apiKey: String, modelId: String, prompt: String): Flow<String> = flow {
        httpClient.serverSentEvents(
            urlString = "$baseUrl/chat/completions",
            request = {
                method = HttpMethod.Post
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(GroqChatRequest(
                    model = modelId,
                    messages = listOf(GroqMessage("user", prompt)),
                    stream = true
                ))
            }
        ) {
            this.incoming.collect { event ->
                val data = event.data ?: return@collect
                if (data == "[DONE]") return@collect
                
                try {
                    val chunk = json.decodeFromString<GroqChatChunk>(data)
                    chunk.choices.firstOrNull()?.delta?.content?.let { emit(it) }
                } catch (e: Exception) {
                    // Ignore malformed chunks
                }
            }
        }
    }

    @Serializable
    private data class GroqModelsResponse(val data: List<GroqModel>)
    
    @Serializable
    private data class GroqModel(val id: String)

    @Serializable
    private data class GroqChatRequest(
        val model: String,
        val messages: List<GroqMessage>,
        val stream: Boolean
    )

    @Serializable
    private data class GroqMessage(val role: String, val content: String)

    @Serializable
    private data class GroqChatChunk(val choices: List<Choice>)

    @Serializable
    private data class Choice(val delta: Delta)

    @Serializable
    private data class Delta(val content: String? = null)
}
