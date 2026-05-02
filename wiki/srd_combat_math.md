# SRD 5.2.1 Combat Mechanics & Math {#srd-5.2.1-combat-mechanics-math}

**CRITICAL DIRECTIVE:** This file contains the exact deterministic mathematics that the Kotlin engine MUST execute locally. Do not ask the LLM to calculate any of these values. Implement these rules as pure Kotlin functions inside the Domain layer (core:engine).

## 1. Core Dice Rolling {#core-dice-rolling}

All random generation must use a secure Kotlin pseudo-random number generator (e.g., kotlin.random.Random).

- d20: Random.nextInt(1, 21)

- d6: Random.nextInt(1, 7)

### Advantage & Disadvantage {#advantage-disadvantage}

- **Advantage:** Roll 2d20, keep the highest value. maxOf(roll1, roll2)

- **Disadvantage:** Roll 2d20, keep the lowest value. minOf(roll1, roll2)

## 2. Ability Modifiers {#ability-modifiers}

The mathematical formula for converting an Ability Score (1-30) into an Ability Modifier is:

Modifier = floor((Score - 10) / 2)

*Example:* A Strength score of 15 yields a modifier of +2. A score of 8 yields -1.

## 3. Initiative {#initiative}

Determines the turn order at the start of combat.

- **Formula:** 1d20 + Dexterity Modifier

- **Tie-breakers:** In the event of a tie, the entity with the highest Dexterity score goes first. If still tied, the player wins the tie against NPCs.

## 4. Attack Rolls (Melee & Ranged) {#attack-rolls-melee-ranged}

To hit a target, the attack roll must equal or exceed the target\'s Armor Class (AC).

- **Melee Weapon Attack:** 1d20 + Strength Modifier + Proficiency Bonus (if proficient with weapon)

  - *Note:* Finesse weapons can use Dexterity instead of Strength.

- **Ranged Weapon Attack:** 1d20 + Dexterity Modifier + Proficiency Bonus (if proficient)

- **Spell Attack Roll:** 1d20 + Spellcasting Ability Modifier + Proficiency Bonus

## 5. Critical Hits & Misses {#critical-hits-misses}

These rules apply ONLY to Attack Rolls (not ability checks or saving throws).

- **Natural 20 (Critical Hit):** An unadjusted d20 roll of 20 always hits, regardless of the target\'s AC.

  - *Damage Math:* Roll all of the attack\'s damage dice twice and add them together. Then add any relevant modifiers as normal. Do not double the flat modifier.

- **Natural 1 (Critical Miss):** An unadjusted d20 roll of 1 always misses, regardless of modifiers.

## 6. Saving Throws {#saving-throws}

Used to resist magical or environmental effects.

- **Formula:** 1d20 + Ability Modifier + Proficiency Bonus (if proficient in that save)

- **Resolution:** Must equal or exceed the Difficulty Class (DC) of the effect.

- **Spell Save DC Formula:** 8 + Proficiency Bonus + Spellcasting Ability Modifier

## Agent Implementation Note:

When writing the CombatAdjudicator class, ensure that HP deduction uses a clamped function to prevent negative hit points: target.hp = maxOf(0, target.hp - finalDamage).
