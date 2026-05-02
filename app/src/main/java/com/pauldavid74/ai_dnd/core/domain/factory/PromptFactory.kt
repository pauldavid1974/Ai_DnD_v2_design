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
            - Inventory: ${character.inventory.joinToString(", ")}
            
            TASK: Provide a concrete, grounded opening narration for the START of a new adventure. 
            
            JSON SCHEMA:
            {
              "final_narration": "Physical detail of surroundings, clear location and situation...",
              "ui_choices": [
                { "label": "Short action suggestion 1", "action_type": "move|attack|spell|improv" },
                { "label": "Short action suggestion 2", "action_type": "..." }
              ]
            }
            
            IMPORTANT: Output ONLY valid, raw JSON. Do not include any other text.
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
        
        val historyContext = if (chatHistory.isNotEmpty()) {
            "\nRECENT CONVERSATION:\n" + chatHistory.joinToString("\n") { "${it.first}: ${it.second}" }
        } else ""
        
        return """
            SYSTEM: You are a D&D 5.2.1 Dungeon Master AI. Your task is to deduce the player's mechanical intent.
            CONTEXT:
            - Player: ${character.name} (Level ${character.level} ${character.characterClass})
            - Status: ${hpBucket.name.lowercase()}
            - Inventory: ${character.inventory.joinToString(", ")}
            $memoryContext
            $historyContext
            
            TASK: Map the user's input to a strict JSON intent.
            
            JSON SCHEMAS:
            1. Melee Attack:
            { "type": "melee_attack", "weaponId": "id", "targetNode": "target_id", "impossibilityScore": 0-100, "narration_prefix": "..." }
            
            2. Cast Spell:
            { "type": "cast_spell", "spellId": "id", "targetNodes": ["id"], "castLevel": N, "impossibilityScore": 0-100, "narration_prefix": "..." }
            
            3. Move:
            { "type": "move", "pathCoordinates": [{"x": N, "y": N}], "impossibilityScore": 0-100, "narration_prefix": "..." }
            
            4. Improvised Action (skill checks, interaction):
            { 
              "type": "improvised_action", 
              "actionDescription": "...", 
              "referencedEnvironmentIds": [], 
              "impossibilityScore": 0-100, 
              "narration_prefix": "...",
              "requires_check": true/false,
              "skill_type": "investigation|perception|stealth|athletics|etc",
              "dc": 10-25
            }
            
            RULES:
            - "requires_check": Set to true if the action (like searching, climbing, or lying) has a chance of failure as per SRD 5.2.1.
            - "skill_type": Use the most relevant D&D 5.2.1 skill (e.g., "investigation" for searching for clues).
            - "dc": Set a logical Difficulty Class (10=Easy, 15=Medium, 20=Hard).
            - "narration_prefix": Provide 1-2 sentences of grounded prose describing the *attempt* (not the outcome). Focus on physical action.
            - Output ONLY the raw JSON. Do NOT include markdown code blocks.
            - "impossibilityScore": 0 is routine, 100 is impossible.
            - If the user is just talking/asking a question, use "improvised_action" with the description of their inquiry.
            - Output ONLY the raw JSON object.
            
            USER INPUT: "$userInput"
        """.trimIndent()
    }

    fun createOutcomePrompt(
        adjudication: AdjudicationResult,
        narrationPrefix: String,
        chatHistory: List<Pair<String, String>>,
        wikiContext: String? = null
    ): String {
        val details = if (adjudication is AdjudicationResult.None) "No check required. Proceed with successful narration." else adjudication.getSummary()
        
        val historyContext = if (chatHistory.isNotEmpty()) {
            "\nRECENT CONVERSATION:\n" + chatHistory.joinToString("\n") { "${it.first}: ${it.second}" }
        } else ""

        return """
            SYSTEM: The deterministic dice have rolled. 
            PREVIOUS ATTEMPT: $narrationPrefix
            $historyContext
            
            ADJUDICATION RESULT:
            - Success: ${adjudication !is AdjudicationResult.Failure && adjudication !is AdjudicationResult.Miss}
            - Details: $details
            ${if (wikiContext != null) "\nSRD RULES REFERENCE:\n$wikiContext" else ""}
            
            TASK: Narrate the physical impact and final outcome.
            
            JSON SCHEMA:
            {
              "final_narration": "Visceral, grounded prose describing the outcome...",
              "haptic_trigger": "one of [resist, expand, bounce, wobble]",
              "ui_choices": [
                { "label": "Short action suggestion 1", "action_type": "move|attack|spell|improv" },
                { "label": "Short action suggestion 2", "action_type": "..." }
              ]
            }
            
            RULES:
            - Ensure the narration follows the PREVIOUS ATTEMPT and RECENT CONVERSATION.
            - Provide 2-3 logical next steps as "ui_choices".
            - final_narration should be specific and concrete. Avoid flowery language.
            - Output ONLY the raw JSON. Do NOT include markdown code blocks.
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
