# Memory Systems & Context Engineering {#memory-systems-context-engineering}

**CRITICAL DIRECTIVE:** The AI must NEVER be relied upon to remember the campaign history organically. Feeding the entire chat transcript into the context window causes \"Memory Wall\" degradation, excessive API costs, and eventual narrative collapse. The agent must implement a strict Just-In-Time (JIT) context injection system.

## 1. The Prompt Orchestrator (JIT Injection) {#the-prompt-orchestrator-jit-injection}

Before every network call to the LLM (The Brain), the local Kotlin PromptFactory must query the Room SQLite database to assemble a highly compressed, modular system prompt.

### Injection Components:

1.  **The Abstracted State:** The player\'s active semantic HP bucket (e.g., \"wounded\"), current location, and active conditions.

2.  **The Chronicler Summary:** The most recent JSON summary of the overarching plot.

3.  **The Short-Term Window:** ONLY the last 3 pairs of chat exchanges (User Intent + AI Narration).

4.  **The Immediate Truth:** The AdjudicationResult from the local Kotlin engine detailing the exact math of the current turn.

*By injecting only what is immediately relevant, the LLM hallucinates an infinite memory without actually holding the data.*

## 2. The Chronicler Background Loop {#the-chronicler-background-loop}

To compress older chat logs, the system employs a background process known as \"The Chronicler.\"

- **Trigger:** Every 5-10 turns, or immediately after a combat encounter ends.

- **Action:** The system sends a batch of raw chat transcripts to a fast, cheap LLM endpoint (e.g., Claude 3.5 Haiku or Groq Llama 3) using the session_summary JSON schema.

- **Storage:** The returned JSON array of memory_updates is parsed by the Kotlin engine and UPSERTED into the Room database. The raw text logs can then be safely dropped from the active context window.

## 3. Preparation for GraphRAG (Knowledge Graphs) {#preparation-for-graphrag-knowledge-graphs}

To solve multi-hop lore retrieval (e.g., \"Who was the blacksmith in the town we visited 3 weeks ago?\"), the Room database must be structured to map entities relationally, mirroring a Knowledge Graph.

- **Nodes:** NPCs, Towns, Factions, Artifacts.

- **Edges:** Relationships (e.g., \"Garrick\" -\> *fears* -\> \"Fire\", \"Lost Mine\" -\> *located_in* -\> \"Phandalin\").

- **Execution:** When the player mentions a Named Entity, the local engine queries the Room DB for that entity and its immediate edges, injecting those specific facts into the JIT Prompt.
