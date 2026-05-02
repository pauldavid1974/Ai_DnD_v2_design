# Phase 1: Project Setup and Foundational Data Layer

## User Prompt
create a plan to implement this plan in phases. follow the github workflow to plan the commit. plan to stop after each phase and wait for permission to continue.

## Plan Context
Implement the complete, production-ready, and flawless Kotlin codebase for a hybrid AI-adjudicated Dungeons & Dragons simulator. This application relies on a strictly defined "Two-Call Cycle" to prevent Generative AI hallucinations and ensure perfect mathematical determinism. The architecture is composed of Phase A (Intent Deduction via AI), Phase B (Local Adjudication via pure Kotlin), and Phase C (Generative Outcome via AI).

Phase 1 focus: Setup dependencies and define the core Room schema for spatial tracking and snapshots.
- [NEW] EntityNode.kt: Represents characters, monsters, and environmental hazards on a 5ft grid.
- [NEW] TurnStateSnapshot.kt: Stores a JSON-serialized snapshot of the encounter for rollbacks.
- [NEW] EntityNodeDao.kt: Includes the circular AoE Euclidean distance query.
