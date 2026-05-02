# Phase 4: Adjudication Logic & Rollback

## User Prompt
Proceed to Phase 4

## Plan Context
Phase 4 focus: Adjudication Logic & Rollback
Implement validators and the Memento-based rollback mechanism.
- ActionValidator: LoS Raycast algorithm and Euclidean distance checks between actor and referenced environmental target nodes.
- ResourceValidator: Query actor's inventory and feature tables for resource_cost.
- SnapshotRepository: JSON Serialization snapshot strategy using kotlinx.serialization and Room @TypeConverter. Wipe and replace EntityNode/Inventory tables on rollback.
- EncounterStateMachine: Integration of guards and transition logic. Bypasses local execution to BounceState for failed validation.
