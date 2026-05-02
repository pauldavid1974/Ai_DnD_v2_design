# Agent Build Phases & Execution Protocol {#agent-build-phases-execution-protocol}

**CRITICAL DIRECTIVE:** You are currently acting as the primary execution agent for this Android application. You MUST read the current project state to determine which phase we are in. **DO NOT jump ahead.** You cannot build UI elements for systems that do not have underlying database schemas or tested Domain logic.

## Execution Rules

1.  Every phase requires atomic Git commits.

2.  Every domain logic implementation must pass unit tests before moving to the UI layer.

3.  If a user asks you to implement a feature from Phase 4 while you are in Phase 2, you must politely warn them and suggest completing the current phase first.

## Phase 1: Foundation & Data Layer {#phase-1-foundation-data-layer}

**Goal:** Scaffold the unyielding deterministic architecture.

- Scaffold the Android project using Jetpack Compose and Material 3.

- Wire Dependency Injection using Dagger Hilt.

- Build the Room SQLite Database schema for both the immutable SRD Reference Data and mutable Campaign/Player State.

## Phase 2: The Bones (Local Rules Engine)

**Goal:** Implement the deterministic math without any LLM interference.

- Build the local Kotlin dice cryptographic parser.

- Implement ability modifier calculators ((score - 10) / 2).

- Build deterministic combat resolution systems and HP managers.

- **Gate:** This phase is complete only when unit tests prove the engine can roll dice, calculate modifiers, and apply damage autonomously.

## Phase 3: AI Provider Layer

**Goal:** Establish the bridge to \"The Brain\".

- Implement the Bring-Your-Own-Key (BYOK) secure storage using EncryptedSharedPreferences.

- Set up the Ktor Client for SSE (Server-Sent Events) streaming.

- Implement dynamic network endpoints to fetch available models from various providers (Anthropic, OpenAI, Groq, etc.).

## Phase 4: Pre-Game & DAG Initialization {#phase-4-pre-game-dag-initialization}

**Goal:** Build the algorithmic preparation logic.

- Develop the Character Creation Wizard.

- Build the Level-Up Wizard UI for players to manually input new spells and class features as string arrays.

- Implement the Alexandrian Node-Based Scenario initialization (Directed Acyclic Graphs).

- Write the local patch-script that analyzes generated DAGs for disconnected islands or dead-ends before allowing the game to start.

- Initialize \"Fronts\" and Grim Portents for dynamic world antagonism.

## Phase 5: Narrative Orchestrator & Live Chat {#phase-5-narrative-orchestrator-live-chat}

**Goal:** Connect the Bones to the Brain via the Two-Call Cycle.

- Build the PromptFactory for Just-In-Time (JIT) context injection.

- Wire the Chronicler background service to summarize memory and store memory_updates to Room.

- Implement the live chat interface, ensuring the UI state is governed by the MVVM/MVI loops.

## Phase 6: Game Feel, Kinetics & FSM {#phase-6-game-feel-kinetics-fsm}

**Goal:** Mask network latency and execute sensory abstractions.

- Enforce KStateMachine constraints across all UI transitions to prevent tearing.

- Implement Semantic HP Bucket translations (hiding raw integers from the AI) and map them to Jetpack Compose color matrix vignettes.

- Build the typewriter text streaming LaunchedEffect.

- Integrate Android Haptic primitives (Resist, Expand, Bounce, Wobble) tied exactly to local dice roll outcomes.
