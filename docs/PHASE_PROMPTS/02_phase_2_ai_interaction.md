# Phase 2: AI Interaction Layer (Phase A)

## User Prompt
resume to the next phase

## Plan Context
Phase 2 focus: AI Interaction Layer (Phase A)
Define polymorphic JSON intents and implement the SSE extraction logic.
- PlayerIntent.kt: Sealed class for CastSpellIntent, MeleeAttackIntent, MoveIntent, ImprovisedActionIntent. capture the exhaustive range of SRD 5.2.1 mechanics.
- [NEW] IntentExtractor.kt: Ktor client implementation for SSE streaming and JSON decoding. aggregated JSON chunks, and utilizing Json.decodeFromString(PlayerIntent.serializer(), jsonString). handle IntentExtractionException for impossibilityScore > 85.
