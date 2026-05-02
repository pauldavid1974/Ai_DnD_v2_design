# SRD 5.2.1 Character Creation Math {#srd-5.2.1-character-creation-math}

**CRITICAL DIRECTIVE:** The local Kotlin engine (core:engine module) must handle all mathematical aspects of character creation deterministically. The LLM is strictly prohibited from generating ability scores, assigning Hit Points, or calculating Proficiency Bonuses. The LLM\'s only role in character creation is generating the backstory and visual description *after* the math is finalized.

## 1. Ability Score Generation {#ability-score-generation}

The engine must support the following two deterministic methods for determining the base six ability scores (Strength, Dexterity, Constitution, Intelligence, Wisdom, Charisma).

### Method A: Standard Array

The user assigns the following exact integers to their six stats:

\[15, 14, 13, 12, 10, 8\]

### Method B: Point Buy

The user has a pool of **27 Points** to spend. All scores start at 8. The maximum base score before species modifiers is 15. The Kotlin engine must enforce these exact point costs:

| **Score** | **Point Cost** |
|-----------|----------------|
| 8         | 0              |
| 9         | 1              |
| 10        | 2              |
| 11        | 3              |
| 12        | 4              |
| 13        | 5              |
| 14        | 7              |
| 15        | 9              |

## 2. Proficiency Bonus Scaling {#proficiency-bonus-scaling}

The Proficiency Bonus (PB) is determined strictly by the character\'s total level, regardless of multiclassing. The local engine must use this scaling to calculate final skill check modifiers.

- **Formula:** PB = 2 + ((CharacterLevel - 1) / 4) *(using standard integer division)*

- **Levels 1--4:** +2

- **Levels 5--8:** +3

- **Levels 9--12:** +4

- **Levels 13--16:** +5

- **Levels 17--20:** +6

## 3. Hit Points (HP) & Hit Dice {#hit-points-hp-hit-dice}

Hit points are calculated locally using the exact Hit Die assigned to the character\'s class.

### Class Hit Dice Mapping

- **d12:** Barbarian

- **d10:** Fighter, Paladin, Ranger

- **d8:** Bard, Cleric, Druid, Monk, Rogue, Warlock

- **d6:** Sorcerer, Wizard

### Level 1 HP Calculation

At 1st level, a character\'s maximum Hit Points equal the maximum value of their Hit Die plus their Constitution modifier.

- **Formula:** MaxHitDieValue + ConstitutionModifier

- *Example:* A Level 1 Fighter (d10) with a Constitution score of 14 (+2) has exactly 12 HP.

### Leveling Up HP (Fixed Average)

When a character levels up, the app should default to the fixed average value to preserve deterministic state (avoiding bad random rolls that cripple players).

- **Formula:** (MaxHitDieValue / 2) + 1 + ConstitutionModifier

- *Example:* The same Fighter hitting Level 2 gains (10 / 2) + 1 + 2 = 8 additional HP.

## Agent Implementation Note:

In the Room Database (core:database), CharacterEntity must store the raw ability scores (1-30). Do not store the modifiers. The modifiers must be dynamically computed via Kotlin getter properties (val strMod get() = (strength - 10) / 2) to ensure that temporary stat-draining conditions recalculate modifiers flawlessly.
