package com.pauldavid74.ai_dnd.core.domain.factory

import com.pauldavid74.ai_dnd.core.data.repository.AiProviderRepository
import com.pauldavid74.ai_dnd.core.database.entity.ScenarioNodeEntity
import com.pauldavid74.ai_dnd.core.domain.model.*
import com.pauldavid74.ai_dnd.core.domain.validation.ScenarioValidator
import com.pauldavid74.ai_dnd.core.security.KeyManager
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScenarioPatchManager @Inject constructor(
    private val aiRepository: AiProviderRepository,
    private val keyManager: KeyManager,
    private val json: Json
) {

    suspend fun patchScenario(graph: ScenarioGraph, startNodeId: String): ScenarioGraph {
        var currentGraph = graph
        var attempts = 0
        val maxAttempts = 3

        while (attempts < maxAttempts) {
            val validation = ScenarioValidator.validate(currentGraph, startNodeId)
            if (validation.isValid) return currentGraph

            attempts++
            currentGraph = requestPatch(currentGraph, validation.errors)
        }

        return currentGraph // Return whatever we have after max attempts
    }

    private suspend fun requestPatch(graph: ScenarioGraph, errors: List<String>): ScenarioGraph {
        val providerId = keyManager.getActiveProvider() ?: "openai"
        val modelId = keyManager.getActiveModel(providerId) ?: "gpt-4"

        val prompt = """
            SYSTEM: You are an Alexandrian Node-Based Scenario Architect. 
            The following campaign graph has structural defects:
            ${errors.joinToString("\n")}
            
            CURRENT GRAPH:
            ${json.encodeToString(ScenarioGraph.serializer(), graph)}
            
            TASK: Provide a JSON patch to fix these defects. You must output the ENTIRE corrected ScenarioGraph.
        """.trimIndent()

        var responseJson = ""
        aiRepository.streamChat(providerId, modelId, prompt).collect { chunk ->
            responseJson += chunk
        }

        return try {
            json.decodeFromString<ScenarioGraph>(responseJson)
        } catch (e: Exception) {
            graph // If patching fails, return original
        }
    }
}
