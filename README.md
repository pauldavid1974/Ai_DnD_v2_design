# AI Dungeon Master (v2)

**Architecture:** Neuro-Symbolic AI / Software 2.0  
**Platform:** Android (Kotlin, Jetpack Compose)  
**Content Base:** D&D SRD 5.2.1 (CC-BY-4.0)

## Overview
AI Dungeon Master is a strict text-first, dark fantasy roleplaying application. It diverges from standard generative AI applications by employing a strict **Neuro-Symbolic** architecture. 

Generative LLMs are fundamentally incapable of executing deterministic tabletop mathematics without hallucination. Therefore, this application structurally isolates the "Bones" (a local, offline Kotlin rules engine) from the "Brain" (the remote LLM API). 

* **The Local Engine:** Handles all dice rolling, hit point tracking, inventory limits, and SRD 5.2.1 rules mathematically via a Room SQLite database.
* **The LLM:** Operates strictly as a narrator and intent-deducer, receiving only semantic abstractions (e.g., "target is bloodied") rather than raw integers.

## The Agentic Workspace (Software 2.0)
This repository is formatted as a machine-readable "Hard Drive" designed to be operated by autonomous IDE agents (Cursor, Android Studio Gemini, Cline). 

### Directory Structure
* `/docs`: Architectural blueprints, JSON contracts, and UI kinetic guidelines.
* `/wiki`: Hyper-compressed, machine-readable markdown files of the SRD 5.2.1 mechanics.
* `/src`: The Android Kotlin source code.
* `AGENT_DIRECTIVES.md`: The inviolable kernel rules governing AI code generation.

## Tech Stack
* **Language:** Kotlin
* **UI:** Jetpack Compose (Material 3)
* **Architecture:** Clean Architecture (MVVM/MVI) with Unidirectional Data Flow
* **Local State:** Room (SQLite)
* **Network / AI Bridge:** Ktor Client (SSE Streaming)
* **Dependency Injection:** Dagger Hilt
* **State Machine:** KStateMachine

## Legal Attribution
This work includes material taken from the System Reference Document 5.2.1 (“SRD 5.2.1”) published by Wizards of the Coast LLC and available at https://dnd.wizards.com/resources/systems-reference-document. The SRD 5.2.1 is licensed under the Creative Commons Attribution 4.0 International License.