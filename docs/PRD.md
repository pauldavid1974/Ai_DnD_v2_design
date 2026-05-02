Here is the fully rewritten, agent-ready Product Requirements Document (PRD), evolved to version 2.0. It fully integrates the Software 2.0 / LLM OS architecture, the strict Neuro-Symbolic boundary, advanced game feel kinetics, and node-based campaign generation. 

***

# AI Dungeon Master (AI_DnD) — Product Requirements Document

**Doc version:** 2.0 (LLM OS Architecture)
**Platform:** Android (Kotlin + Jetpack Compose)
**Content base:** SRD 5.2.1 (CC-BY-4.0, Wizards of the Coast — 2024 D&D rules)
[cite_start]**Architecture Paradigm:** Neuro-Symbolic Artificial Intelligence (NSAI) / Software 2.0 [cite: 1377, 2657]

> **Reader Note for IDE Agents:** This document is the ultimate source of truth for the project. [cite_start]The architecture relies on an absolute separation between the deterministic local engine (The Bones) and the generative language model (The Brain)[cite: 1425]. Do not hallucinate math, do not bypass the JSON contract, and strictly adhere to the agentic workspace rules defined herein.

---

## 1. Core Architecture — The Neuro-Symbolic Boundary

[cite_start]The entire application operates on a strict Neuro-Symbolic Artificial Intelligence (NSAI) paradigm[cite: 1377]. [cite_start]We completely abandon the idea of the LLM as a solitary engine that adjudicates rules[cite: 1376]. 

* [cite_start]**The Bones (Local Deterministic Engine):** Built in native Kotlin, this local engine acts as the unyielding referee[cite: 1426]. [cite_start]It handles all dice rolling, math, character sheets, hit points, inventory, and SRD 5.2.1 rules locally via a Room SQLite database[cite: 1426, 1450].
* [cite_start]**The Brain (Generative LLM):** The remote API operates strictly as a storyteller and adjudicator of intent[cite: 1426]. [cite_start]The LLM possesses absolutely zero authority to determine mathematical outcomes or manipulate raw integer stats[cite: 1426, 1602].

---

## 2. The Agentic Workspace (Software 2.0 Setup)

[cite_start]To allow IDE agents (Antigravity, Cursor, etc.) to build and modify this app without context degradation, the repository is structured as an "LLM OS" hard drive[cite: 2634].

### Directory Structure
* **`/raw`:** The "Basement." [cite_start]Contains immutable source materials like the `SRD_CC_v5.2.1.pdf` and legacy brainstorm drafts[cite: 2227, 2228].
* **`/docs`:** The "Blueprints." [cite_start]Contains project specifications including this `PRD.md`, `architecture.md`, and `git_protocol.md`[cite: 2230, 2462].
* **`/wiki`:** The "Knowledge Base." [cite_start]Contains LLM-optimized, compiled markdown files (e.g., `srd_combat_math.md`, `srd_conditions.md`) that serve as the strict reference layer for agent coding[cite: 2231, 2311].
* **`/src`:** The "Factory Floor." [cite_start]The actual Android Kotlin source code[cite: 2232].

### The Kernel (`.cursorrules` / `AGENTS.md`)
[cite_start]The root directory must contain the global rules file (the Kernel)[cite: 2170]. [cite_start]This file establishes the inviolable rules for the agent: it must enforce the NSAI boundary, utilize Test-Driven Iteration, and never fake/mock SRD data[cite: 1522, 1523, 1524].

---

## 3. Algorithmic Campaign Preparation

[cite_start]Campaign generation abandons linear autoregressive prompting, which causes narrative collapse when players deviate from the script[cite: 1381, 1385]. [cite_start]Instead, preparation utilizes programmatic, multi-agent workflows to build mathematical graph structures[cite: 1399, 1400].

### 3.1 Node-Based Scenario Design (DAGs)
* [cite_start]**The Structure:** Campaigns are generated as Directed Acyclic Graphs (DAGs) or complex scene graphs[cite: 1399]. [cite_start]Nodes represent locations, NPCs, or events, connected by edges representing clues or physical paths[cite: 1396, 1397].
* [cite_start]**Diagnostic/Patching Loop:** During initialization, a local script analyzes the AI-generated DAG for structural defects (Trap Cycles, Dead-End Nodes, State Asymmetry) and forces the LLM to provide JSON patches to repair disconnected narrative islands before gameplay begins[cite: 1403, 1404, 1406, 1410].

### 3.2 Dynamic Antagonism (Fronts)
* [cite_start]To simulate a living world, the LLM generates "Fronts" (adapted from *Dungeon World*) during Session Zero[cite: 1414]. 
* [cite_start]These are categorized into **Campaign Fronts** (long-term) and **Adventure Fronts** (episodic)[cite: 1417].
* [cite_start]Each Front features a specific **Danger** and sequential **Grim Portents**[cite: 1419]. [cite_start]As the in-game clock advances locally, the LLM is prompted to dynamically weave the consequences of these advancing threats into the narrative[cite: 1419, 1420].

---

## 4. The Synchronous Two-Call Resolution Cycle

[cite_start]Every player interaction that involves a mechanical rule must execute a highly strict, synchronous loop to keep the narrative tied to mathematical reality[cite: 1428].

1.  **The Intent Deduction Call:** The player provides natural language input. The LLM receives this and outputs a JSON payload *deducing* the mechanical requirement (e.g., requesting a Dexterity check vs. DC 14). [cite_start]It *does not* narrate the outcome[cite: 1429, 1431, 1432].
2.  **Local Deterministic Adjudication:** The Kotlin engine intercepts the JSON. [cite_start]It rolls simulated cryptographic dice, adds modifiers, applies damage, and updates the Room database in milliseconds[cite: 1433, 1434].
3.  **The Generative Outcome Call:** The engine packages the final mathematical truth into a `RollResult` and sends it back to the LLM. [cite_start]The LLM now acts purely as a narrator, streaming atmospheric prose that matches the local engine's unalterable decision[cite: 1435].

---

## 5. Structured JSON Contracts

[cite_start]The bridge between the Brain and the Bones relies on typed, statically checked JSON schemas[cite: 1440]. 

| JSON Key | Data Type | Operational Definition |
| :--- | :--- | :--- |
| `narration` | String | [cite_start]Atmospheric prose streamed to the UI's typewriter effect[cite: 1444]. |
| `requested_action` | Object | [cite_start]The deduced mechanical intent (skill check, attack, etc.)[cite: 1444]. |
| `choices` | Array | [cite_start]Contextual action chips (e.g., "Attack", "Investigate")[cite: 1444]. |
| `memory_updates` | Array | [cite_start]New narrative facts stored to SQLite by the Chronicler[cite: 1444]. |
| `wiki_lookups` | Array | [cite_start]Requests for specific SRD data needed for the next turn[cite: 1444]. |

---

## 6. Memory Systems & Context Injection

[cite_start]To prevent "Memory Wall" degradation and context bloating, the LLM is not trusted to remember the campaign[cite: 1448, 1849]. 

* [cite_start]**The Chronicler Persona:** A background background agent digests raw chat transcripts and compresses them into `memory_updates` JSON blocks, which are stored securely in the local Room database[cite: 1461, 1462].
* [cite_start]**Just-In-Time (JIT) Prompt Injection:** Before an LLM request, the local Prompt Orchestrator injects the active character sheet, the Chronicler’s summary, current node data, and only the last 3 chat exchanges[cite: 1463, 1465, 1466, 1467, 1468].
* **GraphRAG for Deep Lore:** For long-term worldbuilding, the system maps semantic relationships between Named Entities (NPCs, factions, towns) into Knowledge Graphs. [cite_start]This allows flawless multi-hop lore retrieval, completely eliminating hallucinated history[cite: 1473, 1474, 1476].

---

## 7. Game Feel, UI Kinetics & State Machines

[cite_start]Game feel is achieved by masking network latency with sensory feedback and strictly abstracting mathematics[cite: 1487, 1488].

### 7.1 Semantic HP Buckets
[cite_start]The AI is explicitly forbidden from seeing raw hit point integers[cite: 1735, 1736]. The Kotlin engine abstracts HP into semantic states:
* `healthy` (100%-80%)
* `wounded` (79%-50%)
* `bloodied` (49%-11%)
* [cite_start]`near_death` (10%-1%) [cite: 1483, 1739]

[cite_start]The AI receives these strings and naturally generates visceral descriptions of physical degradation, while the UI simultaneously maps these buckets to color matrix overlays (e.g., deep crimson vignettes)[cite: 1485, 1746].

### 7.2 Finite State Machines (FSM) & Kinetics
* [cite_start]**KStateMachine:** Orchestrates the Two-Call cycle natively, preventing illegal UI configurations (e.g., dice rolling offline)[cite: 1489, 1666].
* **Haptic Primitives:** Precisely timed hardware actuators enhance the tactile feel. 
    * [cite_start]*Resist:* Low ticks for UI drag tension[cite: 1720].
    * [cite_start]*Expand:* Used for successful saving throws/spells[cite: 1721].
    * [cite_start]*Bounce:* Heavy thuds the millisecond the local dice math concludes[cite: 1723].
    * [cite_start]*Wobble:* Disorienting spins for status conditions like Poisoned[cite: 1722].
* [cite_start]**Typewriter Effect:** Narration text streams dynamically via Jetpack Compose's `LaunchedEffect` and `Modifier.drawBehind` is used to paint visual bounding boxes over critical LLM-generated mechanical terms[cite: 1491, 1492, 1683, 1690].

---

## 8. Tech Stack Summary

* [cite_start]**Language & UI:** Kotlin, Jetpack Compose (Material 3, Dark Fantasy theme)[cite: 1809].
* [cite_start]**Architecture:** Clean Architecture, MVVM / MVI with Unidirectional Data Flow[cite: 1810, 1912].
* [cite_start]**Database:** Room (SQLite) for SRD reference and Campaign State[cite: 1811, 1913].
* [cite_start]**Network:** Ktor Client with SSE streaming[cite: 1812, 1914].
* [cite_start]**Dependency Injection:** Dagger Hilt[cite: 1813].
* [cite_start]**Security:** `EncryptedSharedPreferences` for secure Bring-Your-Own-Key (BYOK) storage[cite: 1813, 1908].
* [cite_start]**State Management:** KStateMachine (Kotlin Multiplatform FSM)[cite: 1666].

---

## 9. Phased Build Execution

[cite_start]The project must be executed by IDE agents in strict, atomic phases utilizing Git branches for isolation[cite: 2406, 2669].

* [cite_start]**Phase 1 — Data Layer:** Scaffold Android project, wire Dagger Hilt, and define Room Database schema for SRD and player entities[cite: 2384, 2386, 2388].
* [cite_start]**Phase 2 — The Bones (Local Rules Engine):** Build the local Kotlin dice parser, ability modifier calculators, and deterministic combat resolution systems[cite: 2426].
* [cite_start]**Phase 3 — AI Provider Layer:** Implement BYOK architecture and dynamic network endpoints to fetch available models from providers[cite: 2481, 2483].
* [cite_start]**Phase 4 — Pre-Game / Onboarding:** Develop Character Wizard, DAG scenario initialization, and Fronts assignment[cite: 1392, 1407, 1415].
* [cite_start]**Phase 5 — Narrative Orchestrator & Live Chat:** Build the `PromptFactory` for JIT injection and orchestrate the UI state loop for the live chat interface[cite: 2505, 2600, 2604].
* [cite_start]**Phase 6 — Polish & Kinetics:** Implement KStateMachine transitions, typewriter effects, haptic primitives, and HP bucket color transitions[cite: 1489, 1490, 1746].