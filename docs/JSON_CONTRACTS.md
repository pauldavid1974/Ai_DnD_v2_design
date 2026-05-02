# Structured JSON Contracts

**CRITICAL DIRECTIVE:** The communication bridge between the local Kotlin engine (The Bones) and the remote LLM (The Brain) relies exclusively on strict, statically typed JSON schemas. The Android Studio agent MUST implement these exact schemas using kotlinx.serialization data classes.

Do not allow the LLM to return raw strings or unstructured markdown for any mechanical resolution.

## 1. Intent Deduction Schema (API Call 1) {#intent-deduction-schema-api-call-1}

When the player submits natural language input, the LLM must return this JSON structure to declare what mechanic the local engine needs to adjudicate.

{  
\"intent_detected\": true,  
\"mechanic_type\": \"skill_check \| attack_roll \| saving_throw \| none\",  
\"target_id\": \"string (optional - ID of the entity being targeted)\",  
\"stat_required\": \"str \| dex \| con \| int \| wis \| cha\",  
\"difficulty_class\": 15,  
\"advantage_state\": \"normal \| advantage \| disadvantage\",  
\"narration_prefix\": \"Atmospheric prose describing the \*attempt\* before the dice hit the table. NEVER narrate the outcome.\"  
}

- **Agent Implementation Note:** Create a sealed class PlayerIntent in Kotlin to map these mechanic_type strings to local domain events.

## 2. Generative Outcome Schema (API Call 2) {#generative-outcome-schema-api-call-2}

After the local Kotlin engine rolls the dice, it injects the mathematical truth into the prompt. The LLM must return this JSON structure containing the final atmospheric narration and contextual UI choices.

{  
\"final_narration\": \"Visceral, atmospheric prose describing the physical impact of the locally generated math. (e.g., The goblin reels back, clutching a gushing wound as its status becomes bloodied.)\",  
\"haptic_trigger\": \"resist \| expand \| bounce \| wobble\",  
\"ui_choices\": \[  
{  
\"label\": \"Press the attack\",  
\"action_type\": \"combat\"  
},  
{  
\"label\": \"Attempt to parley\",  
\"action_type\": \"dialogue\"  
}  
\]  
}

## 3. The Chronicler Schema (Background Memory) {#the-chronicler-schema-background-memory}

The background background agent digests the session history and outputs this schema to compress the context window. The Kotlin engine saves this to the Room database.

{  
\"session_summary\": \"A 3-sentence summary of the last 10 turns.\",  
\"memory_updates\": \[  
{  
\"entity_id\": \"npc_garrick\",  
\"new_fact\": \"Garrick revealed he is terrified of fire.\"  
},  
{  
\"entity_id\": \"quest_lost_mine\",  
\"state_change\": \"Clue discovered: The map is hidden in the tavern cellar.\"  
}  
\]  
}

## 4. Kotlin Serialization Rules {#kotlin-serialization-rules}

- Use @Serializable on all Data Classes representing these contracts.

- Use @SerialName(\"snake_case_name\") to map the JSON keys to Kotlin\'s camelCase variable conventions.

- The Ktor Client must be configured with ContentNegotiation and the Json { ignoreUnknownKeys = true } feature to prevent app crashes if the LLM hallucinates an extra field.
