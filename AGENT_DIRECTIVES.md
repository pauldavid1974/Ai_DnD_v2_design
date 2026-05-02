# Android Studio Agent Directives: The \"Kernel\"

**CRITICAL DIRECTIVE:** Read this file before executing any task. This file supersedes all inherent training assumptions.

## 1. The Neuro-Symbolic Boundary (Absolute Law) {#the-neuro-symbolic-boundary-absolute-law}

This project operates on a strict Neuro-Symbolic Artificial Intelligence (NSAI) paradigm.

- **The Bones (Local Engine):** Native Kotlin code is the absolute sovereign truth. The Room database and Kotlin logic handle *all* deterministic math, HP tracking, inventory, dice rolls, and SRD 5.2.1 mechanics.

- **The Brain (The LLM):** The generative AI is *only* a storyteller and intent-deducer.

- **FORBIDDEN:** You must NEVER write code that allows the LLM API to determine if an attack hits, calculate damage integers, or track character HP. The LLM API receives semantic states (e.g., \"bloodied\") and returns narration.

- **MVP Leveling Compromise:** Do not attempt to hardcode complex class progression trees (subclasses, feats). The Kotlin engine must only handle universal level-up math (HP and Proficiency Bonus scaling). Class-specific features should be stored in the database as simple string arrays that the LLM interprets narratively.

## 2. Code Generation & Architecture Constraints {#code-generation-architecture-constraints}

- **Language & UI:** Kotlin, Jetpack Compose (Material 3).

- **Architecture Pattern:** Strict Clean Architecture (MVVM/MVI) with Unidirectional Data Flow.

- **Dependency Injection:** Use Dagger Hilt exclusively.

- **Database:** Use Room SQLite.

- **Network:** Use Ktor Client.

- **File Size Constraint:** Do not write monolithic files. If a ViewModel or Manager class exceeds 400 lines, you must proactively suggest refactoring it into smaller, atomic use-cases.

## 3. Test-Driven Iteration (TDI) {#test-driven-iteration-tdi}

- Before writing implementation code for core:engine (the deterministic math layer), you MUST write the JUnit tests based on the SRD mechanics.

- You must verify that the local Kotlin engine correctly calculates modifiers and dice rolls without any LLM intervention.

## 4. No Hallucinations, No Mock Data {#no-hallucinations-no-mock-data}

- Do not invent new SRD mechanics. Reference the /wiki directory for exact D&D 5.2.1 math and rules.

- Do not use placeholder APIs or mock generation outside of explicit test environments.

## 5. UI & State Machine Strictness {#ui-state-machine-strictness}

- **No remember for core state:** Never store core game data (HP, inventory) in localized Compose remember blocks. All state must be hoisted to the ViewModel as a StateFlow.

- **KStateMachine:** All transitions between the user typing, the network resolving, and the dice rolling must be governed by a strict Finite State Machine to prevent illegal UI states (e.g., UI showing a roll while network is offline).

## 6. Execution Protocol {#execution-protocol}

- Output only production-ready Kotlin. Do not leave // TODO comments for core mechanics.

- If a requested feature conflicts with these directives, STOP and report the conflict before writing any code.

* **Prompt Archiving:** Whenever the user provides a major multi-step prompt (such as initiating a new Phase), you must first save the raw text of their prompt into a new markdown file within `/docs/PHASE_PROMPTS/` using a sequential naming convention (e.g., `01_phase_1_foundation.md`). This ensures all architectural instructions are version-controlled alongside the code.

## 7. GitHub & Version Control
* **CRITICAL:** You MUST read and strictly follow the rules in /docs/GITHUB_WORKFLOW.md every time you touch source control.
* Do not make large, monolithic commits. Keep them atomic and focused.
* You must run tests and ensure the app compiles before staging any commit.
