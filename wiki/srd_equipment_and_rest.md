# SRD 5.2.1 Equipment & Rest Mechanics {#srd-5.2.1-equipment-rest-mechanics}

**CRITICAL DIRECTIVE:** The local Kotlin rules engine (core:engine) must handle all mathematical tracking of inventory weight, Armor Class (AC) calculations, and rest recovery. The LLM does NOT track inventory limits or calculate healed hit points.

## 1. Armor Class (AC) Calculations {#armor-class-ac-calculations}

The engine must dynamically calculate a character\'s AC based on their currently equipped armor, shield, and Dexterity modifier.

- **Unarmored:** 10 + Dexterity Modifier

- **Light Armor:** Armor Base AC + Dexterity Modifier

- **Medium Armor:** Armor Base AC + Dexterity Modifier (Maximum of +2)

- **Heavy Armor:** Armor Base AC *(Dexterity modifier is ignored, whether positive or negative. Note: Heavy armor may have a minimum Strength requirement to avoid a speed penalty).*

- **Shields:** Equipping a shield adds a flat +2 to the calculated AC. A character can only benefit from one shield at a time.

## 2. Carrying Capacity & Encumbrance {#carrying-capacity-encumbrance}

Inventory weight is tracked locally to prevent the LLM from allowing players to carry physically impossible loads.

- **Carrying Capacity:** Strength Score \* 15 (in pounds).

- **Push, Drag, or Lift:** Strength Score \* 30 (in pounds). While pushing or dragging weight in excess of the carrying capacity, speed drops to 5 feet.

- **Size Multipliers:** If the creature is Large, double the capacity. If Tiny, halve it. (Player characters are standardly Medium or Small, which use the base calculation).

## 3. Rest Mechanics (Healing & Recovery) {#rest-mechanics-healing-recovery}

The application must feature explicit UI triggers for Short and Long Rests, which execute deterministic database updates before the AI narrates the passage of time.

### Short Rest (Minimum 1 hour)

- **Mechanic:** A character can spend one or more of their available Hit Dice to recover HP.

- **Math:** For each Hit Die spent, roll that die and add the character\'s Constitution modifier.

- **Formula:** Hit Points Healed = Roll(HitDie) + Constitution Modifier (Minimum of 0).

- **State Update:** Add healed HP to the current HP pool (clamped to max HP). Deduct the spent Hit Dice from the character\'s available pool.

### Long Rest (8 hours)

- **HP Recovery:** The character regains all lost Hit Points.

- **Hit Dice Recovery:** The character regains spent Hit Dice, up to a number of dice equal to half of the character\'s total number of them (minimum of 1 die).

- **State Update:** Reset all spell slots, class feature charges, and daily abilities to their maximum values in the Room database.

## Agent Implementation Note

In the core:engine module, create an InventoryManager and a RestManager class. The RestManager should expose suspended functions like applyLongRest(characterId: String) that perform transactional updates on the CharacterEntity to ensure HP, spell slots, and Hit Dice are all reset atomically within the Room SQLite database.
