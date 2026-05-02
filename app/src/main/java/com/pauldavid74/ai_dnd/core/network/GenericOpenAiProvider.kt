package com.pauldavid74.ai_dnd.core.network

import android.util.Log
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

import io.ktor.client.statement.*

class GenericOpenAiProvider(
    override val id: String,
    override val baseUrl: String,
    private val httpClient: HttpClient,
    private val json: Json
) : AiProvider {

    override suspend fun getAvailableModels(apiKey: String): List<AiModel> {
        return try {
            val url = if (baseUrl.endsWith("/")) "${baseUrl}models" else "$baseUrl/models"
            Log.d("GenericOpenAiProvider", "[$id] Fetching models from: $url")
            
            val response: HttpResponse = httpClient.get(url) {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
            }
            
            if (response.status != HttpStatusCode.OK) {
                Log.e("GenericOpenAiProvider", "[$id] Error status: ${response.status}")
                return emptyList()
            }

            val responseText = response.bodyAsText()
            Log.d("GenericOpenAiProvider", "[$id] Raw response: ${responseText.take(500)}")
            
            val modelResponse: OpenAiModelsResponse = json.decodeFromString(responseText)
            modelResponse.data.map { AiModel(it.id, it.id, id) }
        } catch (e: Exception) {
            Log.e("GenericOpenAiProvider", "[$id] Connection failed. URL: $baseUrl", e)
            emptyList()
        }
    }

    override suspend fun streamChat(apiKey: String, modelId: String, prompt: String): Flow<String> = flow {
        val url = if (baseUrl.endsWith("/")) "${baseUrl}chat/completions" else "$baseUrl/chat/completions"
        Log.d("GenericOpenAiProvider", "[$id] Streaming chat from: $url")

        try {
            httpClient.serverSentEvents(
                urlString = url,
                request = {
                    method = HttpMethod.Post
                    header(HttpHeaders.Authorization, "Bearer $apiKey")
                    contentType(ContentType.Application.Json)
                    setBody(OpenAiChatRequest(
                        model = modelId,
                        messages = listOf(ChatMessage("user", prompt)),
                        stream = true
                    ))
                }
            ) {
                this.incoming.collect { event ->
                    val data = event.data ?: return@collect
                    if (data == "[DONE]") return@collect
                    
                    try {
                        val chunk = json.decodeFromString<OpenAiChatChunk>(data)
                        chunk.choices.firstOrNull()?.delta?.content?.let { emit(it) }
                    } catch (e: Exception) {
                        Log.v("GenericOpenAiProvider", "Skipping non-chat chunk: $data")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("GenericOpenAiProvider", "[$id] Chat request failed", e)
            throw e
        }
    }

    @Serializable
    private data class OpenAiModelsResponse(val data: List<OpenAiModel>)
    
    @Serializable
    private data class OpenAiModel(val id: String)

    @Serializable
    private data class OpenAiChatRequest(
        val model: String,
        val messages: List<ChatMessage>,
        val stream: Boolean
    )

    @Serializable
    private data class ChatMessage(val role: String, val content: String)

    @Serializable
    private data class OpenAiChatChunk(val choices: List<Choice>)

    @Serializable
    private data class Choice(val delta: Delta)

    @Serializable
    private data class Delta(val content: String? = null)
}
