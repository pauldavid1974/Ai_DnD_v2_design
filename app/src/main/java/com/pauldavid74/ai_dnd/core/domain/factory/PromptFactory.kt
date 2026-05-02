package com.pauldavid74.ai_dnd.core.domain.factory

import com.pauldavid74.ai_dnd.core.database.entity.CharacterEntity
import com.pauldavid74.ai_dnd.core.database.entity.MemoryEntity
import com.pauldavid74.ai_dnd.core.domain.model.Front
import com.pauldavid74.ai_dnd.core.rules.CombatEngine
import com.pauldavid74.ai_dnd.core.rules.RollResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PromptFactory @Inject constructor(
    private val combatEngine: CombatEngine
) {

    fun createIntroPrompt(
        character: CharacterEntity
    ): String {
        return """
            SYSTEM: You are a D&D 5.2.1 Dungeon Master. 
            CONTEXT:
            - Player: ${character.name} (Level ${character.level} ${character.characterClass})
            - Inventory: ${character.inventory.joinToString(", ").ifBlank { "Nothing but the clothes on their back" }}
            
            TASK: Provide a concrete, grounded opening narration for the START of a new adventure. 
            - Describe the immediate surroundings with physical detail (sight, sound, smell).
            - Establish a clear location and situation.
            - Avoid flowery, vague atmospheric prose; focus on substance.
            - End with a prompt for the player to act.
            
            IMPORTANT: Output ONLY the narration text. Do not invent items not in the inventory.
        """.trimIndent()
    }

    fun createIntentPrompt(
        character: CharacterEntity,
        lastSummary: String?,
        chatHistory: List<Pair<String, String>>,
        relevantMemories: List<MemoryEntity>,
        activeFronts: List<Front>,
        userInput: String
    ): String {
        val hpBucket = combatEngine.getHpBucket(character.currentHp, character.maxHp)
        val memoryContext = if (relevantMemories.isNotEmpty()) {
            "\nRELEVANT LORE:\n" + relevantMemories.joinToString("\n") { "- ${it.key}: ${it.content}" }
        } else ""
        
        val frontContext = if (activeFronts.isNotEmpty()) {
            "\nACTIVE THREATS (FRONTS):\n" + activeFronts.joinToString("\n") { front ->
                "- ${front.name}: ${front.description}. Triggered Portents: ${front.portents.filter { it.isTriggered }.joinToString { it.description }}"
            }
        } else ""
        
        return """
            SYSTEM: You are a D&D 5.2.1 Dungeon Master. 
            CONTEXT:
            - Player: ${character.name} (Level ${character.level} ${character.characterClass})
            - Status: ${hpBucket.name.lowercase()}
            - Inventory: ${character.inventory.joinToString(", ").ifBlank { "Nothing" }}
            - Recent Plot: ${lastSummary ?: "Just starting the adventure."}
            $memoryContext
            $frontContext
            
            LAST EXCHANGES:
            ${chatHistory.joinToString("\n") { "User: ${it.first}\nAI: ${it.second}" }}
            
            USER INPUT: "$userInput"
            
            TASK: Deduce the player's intent. 
            - mechanic_type: one of [skill_check, attack_roll, saving_throw, none]
            - stat_required: one of [str, dex, con, int, wis, cha]
            - narration_prefix: Grounded prose describing the *attempt*. Focus on the physical action and the immediate environment. Do not use flowery language. NEVER narrate the outcome.
            
            IMPORTANT: Output ONLY valid, raw JSON. Do NOT use markdown code blocks (e.g., no ```json). Do not include any other text. Do not hallucinate equipment.
        """.trimIndent()
    }

    fun createOutcomePrompt(
        adjudication: AdjudicationResult,
        narrationPrefix: String,
        wikiContext: String? = null
    ): String {
        val isSuccess = adjudication is AdjudicationResult.Success || adjudication is AdjudicationResult.Hit || adjudication is AdjudicationResult.None
        val details = if (adjudication is AdjudicationResult.None) "No check required. Proceed with successful narration." else adjudication.getSummary()
        
        return """
            SYSTEM: The deterministic dice have rolled. 
            PREVIOUS ATTEMPT: $narrationPrefix
            
            ADJUDICATION RESULT:
            - Success: $isSuccess
            - Details: $details
            ${if (wikiContext != null) "\nSRD RULES REFERENCE:\n$wikiContext" else ""}
            
            TASK: Narrate the physical impact and final outcome.
            - final_narration: Visceral, grounded prose. Describe the physical consequences and changes to the environment using sight, sound, and touch. 
            - Avoid flowery language, vague metaphors, or atmospheric filler. Be specific and concrete.
            - haptic_trigger: one of [resist, expand, bounce, wobble]
            
            IMPORTANT: Output ONLY valid, raw JSON. Do NOT use markdown code blocks (e.g., no ```json). Do not include any other text.
        """.trimIndent()
    }
}

sealed class AdjudicationResult {
    abstract fun getSummary(): String

    object None : AdjudicationResult() {
        override fun getSummary() = ""
    }

    data class Hit(val damage: Int, val targetStatus: String, val rollTotal: Int) : AdjudicationResult() {
        override fun getSummary() = "Hit! Damage: $damage. Target is now $targetStatus. (Total Roll: $rollTotal)"
    }
    
    data class Miss(val rollTotal: Int) : AdjudicationResult() {
        override fun getSummary() = "Miss. (Total Roll: $rollTotal)"
    }

    data class Success(val rollTotal: Int, val dc: Int) : AdjudicationResult() {
        override fun getSummary() = "Success! (Total Roll: $rollTotal vs DC $dc)"
    }

    data class Failure(val rollTotal: Int, val dc: Int) : AdjudicationResult() {
        override fun getSummary() = "Failure. (Total Roll: $rollTotal vs DC $dc)"
    }
}
