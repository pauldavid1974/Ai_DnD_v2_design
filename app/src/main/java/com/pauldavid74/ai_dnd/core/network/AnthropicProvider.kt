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
import kotlinx.serialization.json.*

class AnthropicProvider(
    private val httpClient: HttpClient,
    private val json: Json
) : AiProvider {
    override val id = "anthropic"
    override val baseUrl = "https://api.anthropic.com/v1"

    override suspend fun getAvailableModels(apiKey: String): List<AiModel> {
        val response: AnthropicModelsResponse = httpClient.get("$baseUrl/models") {
            header("x-api-key", apiKey)
            header("anthropic-version", "2023-06-01")
        }.body()
        return response.data.map { AiModel(it.id, it.display_name ?: it.id, id) }
    }

    override suspend fun streamChat(apiKey: String, modelId: String, prompt: String): Flow<String> = flow {
        httpClient.serverSentEvents(
            urlString = "$baseUrl/messages",
            request = {
                method = HttpMethod.Post
                header("x-api-key", apiKey)
                header("anthropic-version", "2023-06-01")
                contentType(ContentType.Application.Json)
                setBody(AnthropicChatRequest(
                    model = modelId,
                    messages = listOf(AnthropicMessage("user", prompt)),
                    max_tokens = 4096,
                    stream = true
                ))
            }
        ) {
            this.incoming.collect { event ->
                val data = event.data ?: return@collect
                try {
                    val element = json.parseToJsonElement(data)
                    if (element is JsonObject && element["type"]?.jsonPrimitive?.content == "content_block_delta") {
                        val delta = element["delta"]?.jsonObject
                        val text = delta?.get("text")?.jsonPrimitive?.content
                        if (text != null) emit(text)
                    }
                } catch (e: Exception) {
                    // Ignore malformed chunks
                }
            }
        }
    }

    @Serializable
    private data class AnthropicModelsResponse(val data: List<AnthropicModel>)

    @Serializable
    private data class AnthropicModel(val id: String, val display_name: String? = null)

    @Serializable
    private data class AnthropicChatRequest(
        val model: String,
        val messages: List<AnthropicMessage>,
        val max_tokens: Int,
        val stream: Boolean
    )

    @Serializable
    private data class AnthropicMessage(val role: String, val content: String)
}
