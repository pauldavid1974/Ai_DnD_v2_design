# The Synchronous Two-Call Resolution Cycle

**CRITICAL DIRECTIVE:** The agent must never allow the LLM to narrate an outcome before the deterministic local engine has adjudicated the math. This document defines the mandatory 3-step loop for all mechanical interactions. Deviation from this sequence will result in system failure and narrative hallucinations.

## The Vulnerability

If a player types \"I swing my sword at the goblin,\" a standard LLM will immediately narrate \"You swing your sword, critically hitting the goblin and severing its head.\" It hallucinates the dice roll, the weapon damage, and the goblin\'s HP, bypassing the rules of D&D entirely.

## The NSAI Solution (The Loop)

To enforce the Neuro-Symbolic Boundary, every mechanical action must pass through this strict synchronous loop orchestrated by the Kotlin Domain Layer.

### Step 1: The Intent Deduction Call (API Call 1)

- **Trigger:** The player sends a chat message.

- **Action:** The text is sent to the LLM (The Brain) with a system prompt demanding a strict JSON response. The LLM\'s only job is to *deduce* what SRD 5.2.1 mechanic is being attempted.

- **Rule:** The LLM must NOT narrate the outcome. It only narrates the *attempt*.

- **Example Output (Conceptual):**  
  > {  
  > \"intent_type\": \"combat_attack\",  
  > \"target_id\": \"goblin_01\",  
  > \"weapon_id\": \"longsword\",  
  > \"narration_prefix\": \"You grip your longsword tightly and step into a wide arc\...\"  
  > }

### Step 2: Local Deterministic Adjudication (The Bones)

- **Trigger:** The Ktor network client receives and parses the Intent JSON.

- **Action:** The local Kotlin engine intercepts the flow and takes over completely offline.

  1.  Looks up the player\'s STR modifier and Proficiency Bonus from the Room DB.

  2.  Uses a cryptographic random number generator to roll 1d20.

  3.  Compares the total against the target\'s Armor Class.

  4.  Rolls damage if successful.

  5.  Deducts HP and updates the target\'s state in the Room DB.

- **Output:** A strict Kotlin sealed class representing the absolute mathematical truth.

  - *Example:* AdjudicationResult.Hit(damage = 8, targetStatusBucket = \"bloodied\").

### Step 3: The Generative Outcome Call (API Call 2)

- **Trigger:** The Kotlin engine packages the AdjudicationResult.

- **Action:** The system sends a *second* prompt to the LLM containing the unalterable mathematical truth.

  - *Hidden Prompt Injection:* \"System: The player successfully hit the goblin for 8 damage. The goblin\'s status is now \'bloodied\'. Narrate the physical impact of this outcome.\"

- **Output:** The LLM returns the final atmospheric prose via Server-Sent Events (SSE), which the UI consumes and renders using a typewriter text effect.

## UI State Machine Constraints

The Android UI must be locked into an Adjudicating state during this entire cycle.

- During Step 1, show a \"Thinking\...\" indicator.

- During Step 2, trigger a tactile haptic *Bounce* and briefly display the dice math on screen (e.g., \"14 + 3 = 17 vs AC 15\").

- During Step 3, stream the text.

- The user cannot send another message until Step 3 completes and the UI state returns to AwaitingInput.
