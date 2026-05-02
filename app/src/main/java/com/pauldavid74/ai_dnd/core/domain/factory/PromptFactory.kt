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
            
            TASK: Provide a brief, atmospheric opening narration for a new adventure. Set the scene and end with a prompt for the player to act.
            Output ONLY the narration text.
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
            - Recent Plot: ${lastSummary ?: "Just starting the adventure."}
            $memoryContext
            $frontContext
            
            LAST EXCHANGES:
            ${chatHistory.joinToString("\n") { "User: ${it.first}\nAI: ${it.second}" }}
            
            USER INPUT: "$userInput"
            
            TASK: Deduce the player's intent and output the Intent Deduction JSON Schema.
            - mechanic_type: one of [skill_check, attack_roll, saving_throw, none]
            - stat_required: one of [str, dex, con, int, wis, cha]
            - narration_prefix: Atmospheric prose describing the *attempt* before the dice hit the table. NEVER narrate the outcome.
            
            IMPORTANT: Output ONLY valid JSON. Do not include any other text or explanation.
        """.trimIndent()
    }

    fun createOutcomePrompt(
        adjudication: AdjudicationResult,
        narrationPrefix: String,
        wikiContext: String? = null
    ): String {
        return """
            SYSTEM: The deterministic dice have rolled. 
            PREVIOUS ATTEMPT: $narrationPrefix
            
            ADJUDICATION RESULT:
            - Success: ${adjudication is AdjudicationResult.Success || adjudication is AdjudicationResult.Hit}
            - Details: ${adjudication.getSummary()}
            ${if (wikiContext != null) "\nSRD RULES REFERENCE:\n$wikiContext" else ""}
            
            TASK: Narrate the physical impact and final outcome of this action using the Generative Outcome JSON Schema.
            - final_narration: Visceral, atmospheric prose describing the physical impact.
            - haptic_trigger: one of [resist, expand, bounce, wobble]
            
            IMPORTANT: Output ONLY valid JSON. Do not include any other text or explanation.
        """.trimIndent()
    }
}

sealed class AdjudicationResult {
    abstract fun getSummary(): String

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
