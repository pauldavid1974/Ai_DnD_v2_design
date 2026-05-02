# Orchestration Directive: SRD 5.2.1 Character Sheet Implementation

## 1. Objective
Implement a high-fidelity, interactive Character Sheet system for `AI_DnD` that strictly adheres to the **SRD 5.2.1 (2024 Core Rules)**. This includes the updated "Origin" system, "Species" terminology, and "Weapon Mastery" trackers.

## 2. Data Layer: Room Database Updates
Update `com.grimoire.data.local.entity.player.CharacterSheetEntity` to include the following fields:

### A. Identity & Origin
- [cite_start]`species`: String (replaces 'race') [cite: 1285]
- [cite_start]`background`: String (must link to a feat and ability score increases) [cite: 1286]
- [cite_start]`originFeat`: String (granted by background) [cite: 1280]
- `alignment`: String
- `experiencePoints`: Int

### B. Stats & Derived Values
- [cite_start]`baseAbilityScores`: Map<Ability, Int> (Str, Dex, Con, Int, Wis, Cha) [cite: 1263]
- [cite_start]`currentHp`: Int, `maxHp`: Int, `tempHp`: Int [cite: 1268]
- [cite_start]`deathSaveSuccesses`: Int, `deathSaveFailures`: Int [cite: 1269]
- [cite_start]`weaponMasteries`: List<String> (e.g., "Cleave", "Topple", "Vex") [cite: 1275, 1288]

## 3. Domain Layer: The "Bones" Engine Logic
[cite_start]Create or update a `CharacterSheetManager.kt` to handle all deterministic logic locally[cite: 1130, 1163]:

- [cite_start]**Modifiers:** Calculate as $(Score - 10) / 2$ rounded down[cite: 1264].
- [cite_start]**Proficiency Bonus:** Scaled automatically based on total level ($+2$ to $+6$)[cite: 1270].
- [cite_start]**Initiative:** Dexterity Modifier + (Proficiency Bonus *if character has Alert feat*)[cite: 1266, 1287].
- [cite_start]**Passive Senses:** $10 + \text{Modifier}$[cite: 1271].
- [cite_start]**Speed:** Standardized to 30ft for all species (unless modified by features)[cite: 1267, 1285].

## 4. UI Layer: Jetpack Compose (Dark Fantasy Theme)
[cite_start]Build the `CharacterSheetScreen.kt` using the established visual law (Ancient Scroll vibe: ember, parchment, and ink)[cite: 263, 1036].

### Component Requirements:
- [cite_start]**Header:** Character Name, Level, Class, and Species (ensure 'Species' is used, not 'Race')[cite: 1285].
- **Stat Grid:** Six core ability scores with their calculated modifiers prominently displayed.
- **Vitals Bar:** Interactive HP tracker and Death Saving Throw toggles.
- **Proficiency List:** Skill list with auto-calculated totals ($Ability Mod + Proficiency$).
- [cite_start]**Weapon Mastery Tab:** A dedicated section to track 5.2.1 Mastery properties[cite: 1288].
- [cite_start]**Inventory/Equipment:** List of items with Attunement slots (max 3)[cite: 1283].

## 5. Execution Steps for Agent
1. **Branching:** Create a new branch `feat/srd-521-character-sheet`.
2. **Ledger:** Update `TASKS.md` with the checklist for this feature.
3. **Database:** Update the `CharacterSheetEntity` and run a Room migration if necessary.
4. **Logic:** Implement the `CharacterSheetManager` for math-heavy derived stats.
5. **UI:** Build the Compose screens in the `:feature:play` or a new `:feature:character` module.
6. [cite_start]**Verification:** Verify that the "Species" and "Origin Feat" fields are present and correctly mapped to the SRD 5.2.1 definitions[cite: 1258, 1284].

## 6. Constraints
- [cite_start]**Terminology:** Use "Species"[cite: 1285].
- [cite_start]**No Hallucinations:** Do not allow the LLM to modify hit points directly; the UI must call the Kotlin engine to update state[cite: 1168, 1185].
- [cite_start]**BYOK Sync:** Ensure the character sheet data is ready to be injected into the `PromptFactory` for JIT context[cite: 1202, 1203].