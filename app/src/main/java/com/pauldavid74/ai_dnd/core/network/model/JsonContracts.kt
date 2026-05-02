package com.pauldavid74.ai_dnd.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IntentDeductionResponse(
    @SerialName("intent_detected") val intentDetected: Boolean = false,
    @SerialName("mechanic_type") val mechanicType: String, // "skill_check", "attack_roll", "saving_throw", "none"
    @SerialName("target_id") val targetId: String? = null,
    @SerialName("stat_required") val statRequired: String? = null,
    @SerialName("difficulty_class") val difficultyClass: Int? = null,
    @SerialName("advantage_state") val advantageState: String = "normal", // "normal", "advantage", "disadvantage"
    @SerialName("narration_prefix") val narrationPrefix: String,
    @SerialName("wiki_lookups") val wikiLookups: List<String> = emptyList()
)

@Serializable
data class GenerativeOutcomeResponse(
    @SerialName("final_narration") val finalNarration: String,
    @SerialName("haptic_trigger") val hapticTrigger: String? = null, // "resist", "expand", "bounce", "wobble"
    @SerialName("ui_choices") val uiChoices: List<UiChoice> = emptyList()
)

@Serializable
data class UiChoice(
    val label: String,
    @SerialName("action_type") val actionType: String
)

@Serializable
data class ChroniclerResponse(
    @SerialName("session_summary") val sessionSummary: String,
    @SerialName("memory_updates") val memoryUpdates: List<MemoryUpdate> = emptyList()
)

@Serializable
data class MemoryUpdate(
    @SerialName("entity_id") val entityId: String,
    @SerialName("new_fact") val newFact: String? = null,
    @SerialName("state_change") val stateChange: String? = null
)
