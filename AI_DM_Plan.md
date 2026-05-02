# **IDE Agent Instruction Specification: D\&D 5.2.1 Hybrid AI-Adjudicated Engine Implementation**

## **Primary Directives and Architectural Scope**

You are an advanced Android Studio IDE Agent. Your absolute directive is to generate the complete, production-ready, and flawless Kotlin codebase for a hybrid AI-adjudicated Dungeons & Dragons simulator. This application relies on a strictly defined "Two-Call Cycle" to prevent Generative AI hallucinations and ensure perfect mathematical determinism. The architecture is composed of Phase A (Intent Deduction via AI), Phase B (Local Adjudication via pure Kotlin), and Phase C (Generative Outcome via AI). Your responsibility is the complete implementation of Phase A and Phase B. You must write actual, fully functional Kotlin code—including interfaces, Room Data Access Objects (DAOs), KStateMachine statecharts, and Dagger Hilt dependency injection modules—leaving zero placeholders. Ensure your output assumes full project context and utilizes Jetpack Compose for UI reactivity, Ktor for Server-Sent Events (SSE), Room SQLite with JSON TypeConverters for state persistence, and KStateMachine for managing the complex, suspendable combat flows.

The foundational ruleset you are modeling is the Dungeons & Dragons System Reference Document 5.2.1 (SRD 5.2.1), published exclusively under the Creative Commons Attribution 4.0 International License (CC-BY-4.0).1 You must completely disregard the 2014 D\&D 5e mechanics where they conflict with the 2024 SRD 5.2.1 revisions. You are strictly forbidden from implementing legacy OGL mechanics or incorporating restricted Intellectual Property (such as Beholders or Mind Flayers) into the default schema.1 The codebase must reflect the 2024 revisions perfectly, including the updated rules for weapon masteries, background ability score assignments, and the drastically altered spellcasting action economy.2

You must construct this application as a reactive, offline-first, state-driven engine. The LLM does not dictate the state; the LLM merely proposes an intent, which your Kotlin code mathematically validates, calculates, and applies to the local SQLite database. The KStateMachine instance acts as the absolute arbiter of this logic, suspending its execution to await user input for reactions, querying spatial data for Area of Effect (AoE) calculations, and intercepting impossible actions before they corrupt the campaign history.

## **Phase A: Intent Deduction and JSON Schema Generation**

Phase A represents the boundary between unstructured human language and your strictly typed Kotlin runtime. When the player dictates an action via text or voice, this input is sent to an external Large Language Model via a Ktor SSE client. However, the AI must not return prose; it must return a strictly formatted JSON object that perfectly maps to your Kotlin Data Transfer Objects (DTOs).7

You must implement a polymorphic JSON schema utilizing kotlinx.serialization. The serialization configuration must be robust, setting ignoreUnknownKeys \= true and explicitNulls \= false to prevent application crashes when the AI generates malformed keys. Implement a sealed class named PlayerIntent that serves as the root for all action types.

You must generate the following concrete data classes inheriting from PlayerIntent, ensuring they capture the exhaustive range of SRD 5.2.1 mechanics:

| Intent Class Definition | Required JSON Properties | Architectural Purpose |
| :---- | :---- | :---- |
| CastSpellIntent | spellId, targetNodes, castLevel, originNode | Triggers spellcasting validation, deducts specific spell slots, initiates saving throws. |
| MeleeAttackIntent | weaponId, targetNode, isFinesse, isMastery | Processes physical strikes, integrating the new SRD 5.2.1 Weapon Mastery properties.6 |
| MoveIntent | pathCoordinates, destinationNode | Updates spatial coordinates in the Room database, triggering opportunity attack checks. |
| ImprovisedActionIntent | actionDescription, referencedEnvironmentIds, impossibilityScore | Captures "Rule of Cool" actions that fall outside rigid combat mechanics, mapping them to skill checks. |

Implement the Ktor client using the HttpClient engine (e.g., CIO or OkHttp). Create an IntentExtractor repository class injected via Dagger Hilt. This repository must expose a suspend function extractIntent(playerText: String): Flow\<PlayerIntent\>. The function must handle the SSE stream, aggregating the JSON chunks, and utilizing the Json.decodeFromString(PlayerIntent.serializer(), jsonString) method to cast the output safely. If the LLM returns an invalid intent or an impossibilityScore exceeding a strict integer threshold (e.g., 85/100), the IntentExtractor must throw a custom IntentExtractionException. This exception will be caught by the state machine and routed to a fallback narrative generation, preventing the local engine from processing nonsense.

## **Phase B: Core Local Adjudication via KStateMachine**

Once Phase A delivers the structured PlayerIntent, the application enters Phase B. Phase B is entirely governed by KStateMachine, a Kotlin DSL library engineered for creating complex statecharts driven by Kotlin Coroutines.9 You must not build a custom state machine; rely exclusively on the com.github.nsk90:kstatemachine dependency.11

You must define the core combat loop using the createStateMachine(scope) suspendable factory function, which is the optimal choice for coroutine-heavy architectures.12 By injecting a CoroutineScope tied to the ViewModel (viewModelScope), you ensure the state machine lifecycle is bound to the user interface, preventing memory leaks during suspended transitions.12

Implement the state machine as a hierarchical statechart. The root instance is the EncounterStateMachine. Within this machine, define nested states to accurately model the D\&D combat structure.10

| State Hierarchy Level | KStateMachine DSL Implementation | SRD 5.2.1 Mechanical Function |
| :---- | :---- | :---- |
| **Encounter State** | state("CombatEncounter") {... } | Manages the global initiative array, lair actions, and environmental effects spanning the whole encounter. |
| **Round State** | state("CombatRound") {... } | Tracks round progression. Crucial for duration-based spells (e.g., *Bless* expiring after 10 rounds). |
| **Turn State** | state("EntityTurn") {... } | Manages a specific actor's action economy. Resets Reaction availability at the *start* of the turn.14 |
| **Resolution State** | dataState\<PlayerIntent\>("ActionResolution") {... } | A Typesafe state that directly receives the JSON payload from Phase A, ensuring memory-safe data passage.15 |

Implement the ActionResolution state specifically as a DataState\<PlayerIntent\>. The DataState API ensures that the event data parameter type matches the type expected by the target state, effectively forcing the Kotlin compiler to protect the transition from incompatible data types.15 When the IntentEvent (which must implement DataEvent\<PlayerIntent\>) is processed, the machine transitions into the ActionResolution state, automatically exposing the data field to the onEntry callback.15 Inside this onEntry block, you must execute the Room database transaction that mutates the game state.

## **Resolving the "Rule of Cool" vs. Nonsense**

A critical edge case in AI DM architectures is distinguishing between a mechanically impossible action (nonsense) and a creatively improvised action ("Rule of Cool"). You must programmatically adjudicate ImprovisedActionIntent payloads without defaulting to rigid video-game logic.

Implement an ActionValidator interface and bind it via Dagger Hilt. This validator operates within a transient ValidationState inside the KStateMachine. When the state machine receives an ImprovisedActionIntent, the ActionValidator executes a series of pure Kotlin logic gates against the Room database. First, perform a Line of Sight (LoS) Raycast algorithm and a Euclidean distance check between the actor and the referenced environmental target nodes. For example, if the intent states the player is swinging from a chandelier, the validator queries the EntityNode Room table to verify a chandelier node exists within a 5-foot spatial radius of the player.

If the spatial validation passes, the Kotlin engine dynamically constructs a SkillCheckEvent (e.g., an Acrobatics check against a DC calculated by the LLM in Phase A). The state machine transitions to a DiceRollWaitState, prompting the Jetpack Compose UI to request a dice roll from the user.

However, if the spatial validation fails (e.g., no chandelier exists), or if the LLM flagged an impossibilityScore higher than 85 (e.g., "I jump to the moon"), the state machine must transition immediately to a BounceState. You must implement the BounceState to bypass local execution entirely. In the BounceState's onEntry block, formulate a payload that triggers Phase C. Instruct the Phase C generative model to narrate the failure gracefully, maintaining player immersion while firmly denying the action (e.g., "You leap upwards, grasping for a chandelier that isn't there, and land awkwardly"). This pattern safely bounces impossible actions before they ever hit the mathematical local engine, preserving the integrity of the Room database.

## **Managing Asynchronous Interrupts and Reaction Mechanics**

The implementation of interrupts and reactions represents the most severe architectural hurdle. In the D\&D 2024 SRD 5.2.1, a reaction is an instantaneous response to a specific trigger, such as taking damage or observing a spell being cast.16 Spells like *Shield* or *Counterspell* require the game loop to halt immediately, wait for the reacting entity's decision, and potentially alter the mathematical outcome of the triggering event.18

You must handle this by utilizing KStateMachine's ability to suspend transitions and wait for external input events.19 When an attack intent passes validation, do not immediately deduct hit points. Instead, transition the state machine into a DamageCalculationState.

Implement a global Coroutine-based Event Bus (using SharedFlow) that broadcasts the impending damage event to all combatants. Query the Room database for any entity within range that has an unspent reaction and a valid trigger (e.g., an enemy possessing the *Shield* spell). If a valid reactor is found, push an InterruptEvent to the state machine.

The KStateMachine must handle this by utilizing a nested, suspendable child state. Instruct the DamageCalculationState to enter an AwaitingInterruptResolution child state.15 Due to the Continuation Passing Style (CPS) of Kotlin Coroutines, this suspends the state machine's execution flow indefinitely without blocking the Main thread.20 Concurrently, emit a state update to Jetpack Compose, triggering a UI dialog for the player (or initiating an automated AI decision tree if the reactor is an NPC) asking whether they wish to expend their reaction.

Once the input is received, fire a ReactionResolvedEvent. If the reaction was *Shield*, execute a Room @Update to temporarily increase the reactor's Armor Class (AC), and then resume the DamageCalculationState. The system will now compare the original attack roll against the newly elevated AC, seamlessly integrating the interrupt into the mathematical truth.

### **The SRD 5.2.1 "One Spell Slot Per Turn" Limitation**

You must strictly enforce the most defining mechanical change of the 2024 SRD 5.2.1: the altered spellcasting action economy. The new rule explicitly states: "On a turn, you can expend only one spell slot to cast a spell".14 This completely replaces the 2014 Bonus Action spell rules and establishes a rigid, programmatic constraint that heavily impacts reactions.

Mechanically, this requires you to prevent a character from casting a leveled spell with the casting time of a Reaction on their own turn if they have already cast another leveled spell.22 For instance, if a player's wizard casts *Fireball* (a leveled spell) and an NPC mage responds with *Counterspell*, the player *cannot* use their own reaction to cast *Counterspell* in return, because they cannot expend two spell slots on the same turn.22 Similarly, if the wizard casts a leveled spell and then triggers an opportunity attack by moving, they are programmatically barred from casting *Shield*.22 Action Surge does not bypass this limitation.22

To implement this flawlessly, you must modify the Room database entity for TurnState. Add a highly specific boolean column: leveled\_spell\_cast\_this\_turn.

| Room TurnState Flag | SRD 5.2.1 Trigger Condition | Programmatic Consequence in KStateMachine |
| :---- | :---- | :---- |
| has\_used\_action | A Standard Action is utilized. | KStateMachine transition guard rejects subsequent StandardActionIntent. |
| has\_used\_bonus\_action | A Bonus Action is utilized. | KStateMachine transition guard rejects subsequent BonusActionIntent. |
| has\_used\_reaction | A Reaction is utilized. | Transition guard rejects subsequent ReactionIntent. This flag resets at the *start* of the entity's next turn.14 |
| leveled\_spell\_cast\_this\_turn | Any spell utilizing a spell slot is cast. | Bypasses the interrupt listener for any Reaction spell requiring a slot (e.g., *Shield*, *Counterspell*) for the remainder of the active turn.22 |

Implement a ReactionValidator class. When the global event bus broadcasts a reaction trigger, the ReactionValidator must check the leveled\_spell\_cast\_this\_turn flag. If this flag is true, the validator must automatically filter out any reaction abilities that consume a spell slot. The InterruptEvent must never be fired, and the Jetpack Compose UI must never prompt the player for a reaction. This ensures strict, invisible adherence to the 2024 SRD spellcasting limitations without requiring manual DM intervention.

Furthermore, reactions that do not consume a spell slot, or reactions triggered when the actor is concentrating on a spell with a long casting time (e.g., 1 minute), remain valid so long as the reaction itself does not require concentration.18 Ensure the ReactionValidator differentiates between leveled spells and non-leveled reactions (like an Opportunity Attack with a melee weapon).

### **Resolving Legendary and Off-Turn Actions**

While the 2024 D\&D ecosystem has revised how monsters utilize off-turn actions, the mechanical necessity of intervening at the end of another creature's turn remains critical. You must handle this utilizing KStateMachine's onExit callbacks. Attach an onExit block to the EntityTurn state. Before the machine formally transitions to the next entity in the initiative array, suspend the execution and query the Room database for any hostile entities possessing unspent off-turn actions or legendary actions. If such entities exist, instantiate a temporary OffTurnActionState, resolve the action through the standard Phase A/Phase B cycle, and only advance the global initiative index once the OffTurnActionState resolves.

## **Area of Effect (AoE) Spatial Queries in Room SQLite**

When the Phase A Intent Deduction outputs a CastSpellIntent for an Area of Effect ability (e.g., "I cast Fireball at the center of the room"), the system must mathematically identify all entities within the specified radius. Because the Android Room persistence library abstracts SQLite, and SQLite lacks native PostGIS spatial extensions, you must execute Euclidean distance calculations directly within your SQL queries.23

You must design a highly normalized Room database schema. Create an EntityNode table that represents all characters, monsters, and interactive environmental hazards. Define x and y columns for each node, representing their absolute coordinates on a 5-foot grid square system. To optimize performance, apply a Room @Index to both the x and y columns to prevent full table scans during spatial queries.

Implement the Area of Effect spatial query inside your Room DAO using the mathematical formula for a circle: ![][image1], where ![][image2] is the spell's target origin and ![][image3] is the radius.23 You must write the exact @Query annotation as follows:kotlin @Query(""" SELECT \* FROM EntityNode WHERE status\!= 'DEAD' AND ((x \- :originX) \* (x \- :originX) \+ (y \- :originY) \* (y \- :originY)) \<= (:radiusSq) """) suspend fun getEntitiesWithinRadius(originX: Float, originY: Float, radiusSq: Float): List

By filtering the entities at the database level, you prevent the Kotlin runtime from pulling thousands of irrelevant nodes into memory. Because D\&D 5.2.1 occasionally relies on grid systems where diagonals are treated via Chebyshev distance (maximum absolute difference of coordinates), ensure the DAO interface allows for dynamic query swapping depending on the specific grid rules configured in the application settings. For standard circular radii, the squared Euclidean distance query provided is highly performant.

\#\#\# Multi-Target Adjudication and Bulk Room Updates

Once the DAO returns the list of affected \`EntityNode\` objects, the KStateMachine must transition into a \`MultiTargetSaveState\`. Do not execute individual database transactions for each entity's saving throw, as this will severely bottleneck SQLite and cause UI stutter. Instead, iterate over the returned entities in memory.

Generate the appropriate saving throws (e.g., Dexterity) against the caster's Spell Save DC. Per SRD 5.2.1 mechanics, successful saves typically result in half damage. Calculate the final damage values for each entity, applying \`Math.floor\` or \`Math.ceil\` strictly according to 5e rounding rules (always round down). 

Once the memory map of updated hit points is complete, utilize a bulk \`@Update\` convenience method in the Room DAO.\[25\] You must wrap this bulk update function in a Room \`@Transaction\` annotation. This guarantees database atomicity; if one node update fails or a constraint is violated, the entire AoE damage application rolls back simultaneously, preventing corrupt, partially resolved game states.

\#\# Deterministic State Rollbacks via Single Row Snapshots

Because Phase A relies on generative AI for semantic extraction, LLM hallucinations or misinterpretations are statistically inevitable. If a player states, "I shoot the goblin," but the LLM incorrectly classifies the target as the friendly cleric, this error will propagate through Phase B and permanently corrupt the mathematical state. You must construct an immediate rollback mechanism capable of rewinding the application state by exactly one turn without polluting the LLM's context window with complex correction prompts.

Implement the Memento design pattern directly within the Room database.\[26, 27\] You must create a dedicated Room table named \`TurnStateSnapshot\`.

Do not attempt a full relational backup where every row is cloned into secondary shadow tables, as this incurs unacceptable storage and performance costs.\[27\] Instead, implement a JSON Serialization snapshot strategy.\[26, 28\] At the exact moment the KStateMachine enters the \`TurnStartState\` (immediately prior to executing Phase A), a Dagger Hilt-injected \`SnapshotRepository\` must trigger a complete serialization of the current encounter state.

Read the \`EntityNode\`, \`Inventory\`, and \`EncounterData\` tables. Utilize \`kotlinx.serialization\` and a Room \`@TypeConverter\` to compress these relational tables into a single, massive JSON string.\[28\] Insert this string into the \`TurnStateSnapshot\` table. To prevent database bloat, restrict this table to a single row (ID \= 1), utilizing an \`@Insert(onConflict \= OnConflictStrategy.REPLACE)\` upsert strategy to continually overwrite the previous turn's snapshot.

\#\#\# Executing the Rollback Sequence

If the user identifies an AI hallucination and clicks "Undo" in the Jetpack Compose UI, dispatch a \`RollbackEvent\` to the KStateMachine. The state machine must immediately abort its current transition and discard any pending coroutines associated with the active turn. 

The \`SnapshotRepository\` then executes a \`@Transaction\` that wipes the active \`EntityNode\` and \`Inventory\` tables, immediately replacing them with the deserialized entities extracted from the JSON string in the \`TurnStateSnapshot\` table. Because the Jetpack Compose UI is architected to observe the Room database via Kotlin \`Flow\` or \`LiveData\`, the UI will reactively and instantly update to the previous, uncorrupted state.

Crucially, you must write logic that intercepts the prompt payload sent to the Phase C generative model. When a rollback occurs, the Ktor client must silently instruct the LLM to drop the previous turn from its context window history entirely. By erasing the hallucination from the AI's memory and rewinding the SQLite database, you guarantee that the AI's contextual awareness remains perfectly synchronized with the true mathematical state of the Kotlin engine.

\#\# Granular Resource Tracking and Pre-Transition Guards

The SRD 5.2.1 ruleset demands strict enforcement of granular inventory resources: arrows, spell slots, class features, and rations. A common failure point in hybrid AI RPG architectures is chronological desynchronization. If Phase A generates a \`CastSpellIntent\` for \*Magic Missile\*, but the character's Room inventory shows zero first-level spell slots, the system must intercept this intent before Phase C narrates a successful casting. 

You must accomplish this utilizing KStateMachine's transition validation guards.\[21\] Implement a series of \`Guards\` (condition callbacks evaluated before a transition executes) attached directly to the \`IntentEvent\`.

When the \`IntentEvent\` attempts to transition the machine from \`IntentDeductionState\` to \`ActionResolutionState\`, the transition logic must invoke a Dagger Hilt-injected \`ResourceValidator\`. The \`ResourceValidator\` queries the Room database for the actor's inventory and feature tables. If the JSON intent specifies \`resource\_cost: "spell\_slot\_1"\`, the validator checks the \`SpellSlots\` entity for that specific character.

If the resource count evaluates to zero, the guard must return \`false\`. The KStateMachine will rigidly refuse the transition to \`ActionResolutionState\`.\[21\] However, the machine must not crash or throw a fatal exception. Instead, define an \`unless\` condition or a conditional branch that redirects the state machine to an \`InsufficientResourceState\`. 

When the machine enters the \`InsufficientResourceState\`, construct an internal error payload: \`{"error": "insufficient\_resource", "type": "spell\_slot\_1"}\`. Pass this payload to Phase C. Instruct the AI DM to inform the player immersively that the action failed due to exhaustion or lack of materials (e.g., "You reach for the weave to manifest Magic Missile, but your mind is tapped of magical energy. You must choose another action."). This local interception guarantees that inventory mathematics remain absolute; the LLM is powerless to override an empty quiver or an exhausted spell slot because your Kotlin layer acts as an impenetrable, deterministic firewall.

\#\# Jetpack Compose and Dependency Injection Architecture

To ensure the entire system remains modular, testable, and reactive, you must strictly implement Dependency Injection using Dagger Hilt. Write the \`@Module\` and \`@InstallIn(SingletonComponent::class)\` objects required to provide the Room Database, the KStateMachine instance, the Ktor HTTP Client, and the various validators (\`ActionValidator\`, \`ReactionValidator\`, \`ResourceValidator\`). 

The KStateMachine instance must be exposed to the Jetpack Compose UI via a \`ViewModel\` annotated with \`@HiltViewModel\`. Expose the current state of the machine as a \`StateFlow\<IState\>\`, and expose transient events (such as the request to use a Reaction) as a \`SharedFlow\<ReactionRequest\>\`. The Jetpack Compose UI must observe these flows using \`collectAsStateWithLifecycle()\`, dynamically re-rendering the combat interface, hit points, and dialog overlays in real-time as the pure Kotlin engine mutates the underlying Room database. 

By strictly following these directives, you will generate an Android application architecture that perfectly balances the creative narrative freedom of Large Language Models with the uncompromising mathematical determinism of the Dungeons & Dragons SRD 5.2.1 ruleset. Proceed immediately with generating the complete codebase based on these specifications.

#### **Works cited**

1. D\&D \- System Reference Document v5.2 \- Tribality, accessed May 2, 2026, [https://www.tribality.com/2025/04/23/dd-system-reference-document-v5-2/](https://www.tribality.com/2025/04/23/dd-system-reference-document-v5-2/)  
2. Everything To Know About D\&D 2024's New SRD 5.2 Creative Commons License, accessed May 2, 2026, [https://screenrant.com/dnd-2024-srd-52-creative-commons-license-explainer/](https://screenrant.com/dnd-2024-srd-52-creative-commons-license-explainer/)  
3. SRD v5.2.1 \- System Reference Document \- D\&D Beyond, accessed May 2, 2026, [https://www.dndbeyond.com/srd](https://www.dndbeyond.com/srd)  
4. D\&D SRD 5.2 – What You Need to Know \- Roll20, accessed May 2, 2026, [https://pages.roll20.net/dnd-srd](https://pages.roll20.net/dnd-srd)  
5. What's New in 2026: D\&D Rules, Unearthed Arcana & SRD 5.2 Explained \- Runic Dice, accessed May 2, 2026, [https://www.runicdice.com/blogs/news/what-s-new-in-2026-d-d-rules-unearthed-arcana-srd-5-2-explained](https://www.runicdice.com/blogs/news/what-s-new-in-2026-d-d-rules-unearthed-arcana-srd-5-2-explained)  
6. New DnD SRD (Systems Reference Document) 5.2 Now Available, accessed May 2, 2026, [https://dungeonsanddragonsfan.com/dnd-srd-new-rules/](https://dungeonsanddragonsfan.com/dnd-srd-new-rules/)  
7. Introducing Intents \- Zep, accessed May 2, 2026, [https://blog.getzep.com/introducing-intents/](https://blog.getzep.com/introducing-intents/)  
8. AI-assisted JSON Schema Creation and Mapping Deutsche Forschungsgemeinschaft (DFG) under project numbers 528693298 (preECO), 358283783 (SFB1333), and 390740016 (EXC2075) \- arXiv, accessed May 2, 2026, [https://arxiv.org/html/2508.05192v2](https://arxiv.org/html/2508.05192v2)  
9. KStateMachine \- GitHub, accessed May 2, 2026, [https://github.com/KStateMachine](https://github.com/KStateMachine)  
10. GitHub \- KStateMachine/kstatemachine: Powerful Kotlin Multiplatform library with clean DSL syntax for creating complex state machines and statecharts driven by Kotlin Coroutines., accessed May 2, 2026, [https://github.com/KStateMachine/kstatemachine](https://github.com/KStateMachine/kstatemachine)  
11. State-Machine in Android/Kotlin \- Medium, accessed May 2, 2026, [https://medium.com/@karthik.dusk/state-machine-in-android-kotlin-4f04e4121062](https://medium.com/@karthik.dusk/state-machine-in-android-kotlin-4f04e4121062)  
12. State machine | KStateMachine \- GitHub Pages, accessed May 2, 2026, [https://kstatemachine.github.io/kstatemachine/pages/statemachine.html](https://kstatemachine.github.io/kstatemachine/pages/statemachine.html)  
13. Overview | KStateMachine, accessed May 2, 2026, [https://kstatemachine.github.io/kstatemachine/](https://kstatemachine.github.io/kstatemachine/)  
14. Leveled Spells and Reactions : r/DMAcademy \- Reddit, accessed May 2, 2026, [https://www.reddit.com/r/DMAcademy/comments/1hngkan/leveled\_spells\_and\_reactions/](https://www.reddit.com/r/DMAcademy/comments/1hngkan/leveled_spells_and_reactions/)  
15. Typesafe transitions | KStateMachine \- GitHub Pages, accessed May 2, 2026, [https://kstatemachine.github.io/kstatemachine/pages/transitions/typesafe\_transitions.html](https://kstatemachine.github.io/kstatemachine/pages/transitions/typesafe_transitions.html)  
16. Wizards of the Coast releases SRD version 5.2 for Dungeons and Dragons \- Blizzard Watch, accessed May 2, 2026, [https://blizzardwatch.com/2025/04/28/wizards-coast-releases-srd-version-5-2-dungeons-dragons/](https://blizzardwatch.com/2025/04/28/wizards-coast-releases-srd-version-5-2-dungeons-dragons/)  
17. All reactions in D\&D 2024 : r/3d6 \- Reddit, accessed May 2, 2026, [https://www.reddit.com/r/3d6/comments/1qu3h3k/all\_reactions\_in\_dd\_2024/](https://www.reddit.com/r/3d6/comments/1qu3h3k/all_reactions_in_dd_2024/)  
18. Can I cast reaction spells like Shield or Counterspell when I'm in the middle of casting a spell with a long casting time and don't stop casting it? \- RPG Stack Exchange, accessed May 2, 2026, [https://rpg.stackexchange.com/questions/150359/can-i-cast-reaction-spells-like-shield-or-counterspell-when-im-in-the-middle-of](https://rpg.stackexchange.com/questions/150359/can-i-cast-reaction-spells-like-shield-or-counterspell-when-im-in-the-middle-of)  
19. STM32 External Interrupt Example LAB – DeepBlue \- DeepBlueMbedded, accessed May 2, 2026, [https://deepbluembedded.com/stm32-external-interrupt-example-lab/](https://deepbluembedded.com/stm32-external-interrupt-example-lab/)  
20. The Machinery Behind the Magic: How Kotlin Turns suspend into State Machines, accessed May 2, 2026, [https://doveletter.dev/preview/articles/coroutines-compiler-machinery](https://doveletter.dev/preview/articles/coroutines-compiler-machinery)  
21. Transitions and events — Python State Machine 1.0.3 documentation, accessed May 2, 2026, [https://python-statemachine.readthedocs.io/en/v1.0.3/transitions.html](https://python-statemachine.readthedocs.io/en/v1.0.3/transitions.html)  
22. 2024 D\&D Rule Change: One Spell Slot Per Turn \- Illusory Script, accessed May 2, 2026, [https://illusoryscript.com/dnd-2024-one-spell-slot-per-turn/](https://illusoryscript.com/dnd-2024-one-spell-slot-per-turn/)  
23. Calculate distance between two points directly in SQLite \- Stack Overflow, accessed May 2, 2026, [https://stackoverflow.com/questions/10126404/calculate-distance-between-two-points-directly-in-sqlite](https://stackoverflow.com/questions/10126404/calculate-distance-between-two-points-directly-in-sqlite)  
24. SQLite query for calculating distance | Page 2 | B4X Programming Forum, accessed May 2, 2026, [https://www.b4x.com/android/forum/threads/sqlite-query-for-calculating-distance.34443/page-2](https://www.b4x.com/android/forum/threads/sqlite-query-for-calculating-distance.34443/page-2)

[image1]: <data:image/png;base64,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>

[image2]: <data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADkAAAAeCAYAAACWsbYLAAADaUlEQVR4Xu2YS6hNURjHP3mHPCMhxUAekSQDJKUwYOARxciERIkBGSkZKCOpKymvpGRKmZ1SiPIYiJQ65JHEjAnh+921vu466+y19777nFPXdf71y7W+s/de61vfY+0t0lVXXf0LGqKs98yObANJY5Td/l8oLRZ4ROnxDG8094qxacp0ZVRka5fGirv/VGVoZAu1VbntGR/ZkuKix+JuDrH2Kn88P5TljeaWhMOACdsz7injwh9FYlNOei5I9qY0adAvcqbyXNkSGyJNVp4pL5Qpka1dWqP8Us7GhgyROsDcd0S2Jh1VHkpxbM9XvirXxXmyEzosbid3xYYcnVAeiduETLEj7AwLLRIPZgKEbic0TLklzpE4tKwWibtmc2wwrVO+K6tiQ4YIoXbnY6gZSl1cbZjYaMoVuUsOX5JEhB1X3ovLyzzxUB7Ors8Tdx03vqws6PtZS8LRP6UxH+cq58U9J2+OFyXhHFZNftXE9ac8WT6+Ve4qq8U99KryRVnoaUWkjOUjc9umXFM2KU+8PSVsmWHOwmriSndRc7d8ZPcmBeM2MSYCVRS2ECaKs7aLixZC0VoLRSklOgNptyw2WAheiQ0ZIoQIpbXROBP5LS63oYrIRcvHp+KqJacvTjzsKA5mLK/642BzdoM4PhF+RYsM8zHsj+b9uvRNtIrIRctHJvpK3Pm5sMEHSi6y7E6m+qON3xBX/qGKCHkL+4PKTuWd8lrKvygkF2k5SW/KmyDxntUfD/hxJmWLXKGMDn9UIOuNcX+0Z1oestimohIomZP2gJrkV9es/mgOqosL0zkedtXutVjctSv9/7PEseyNJ2wBtjPm2DPKBv93loiET+Lm0KRTkt8nqW5U1DgfLZ9xErlzzMN7HgoP3PG1oXAcDoSwP4bhN0tcK8krPMk+iYpOPITJR2k+TRAF55QPyh3ltCcsFofEVd5vypJgPNQecYuB8FjGOfS+8kCckylEKdlGJA/1/8UiyYmXkj5NsDCaf1Y5x8ZkJsSGQDijR9JFg/tyf4jPnfRJ3m3z6gXigP5Z8nO2t9GWedWqIpxIFHTi3qZSr4plX5qraJ+yPx5sk3AglJ570eePKlqq3JQCD1cUod2vzx+Ii4q+1vVX3GNEPNgmVfpah1goVQyoqgNVI5WNUuG766DTX7Qpz/TqVJGJAAAAAElFTkSuQmCC>

[image3]: <data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAwAAAAdCAYAAACe/43ZAAAAsklEQVR4XmNgGAWjAC+QAOLZQLwCiNWAmBOK04B4BxBnQflgAGIsBmJXIN4IxI+A+AQUpwKxPRA/B+IImAZLIJ4DxKxAvBSI/wNxDBTzM0A0gsSiydaQDMRBQCwCxFeB+DQQC0IxCxAnAXElEHPDNMCAMRB/ZYDYRhQAWYliNT7ACMTzGSA2gGwiCEDuBbkd5n6CQB+IPzFAbAHZRhD4MkDcD48cQgAUB6BgZUaXGAXkAAC/4yHZQcNVvQAAAABJRU5ErkJggg==>