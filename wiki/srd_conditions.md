# SRD 5.2.1 Status Conditions {#srd-5.2.1-status-conditions}

**CRITICAL DIRECTIVE:** The local Kotlin rules engine must apply these exact mechanical modifiers when a character or NPC is afflicted with a condition. The LLM does NOT calculate these penalties. Do not prompt the LLM to determine if an attack has Disadvantage; the Kotlin engine must enforce it automatically based on these rules.

## Condition Mechanics

### Blinded

- **Perception:** Automatically fails any ability check that requires sight.

- **Combat:** Attack rolls against the creature have Advantage. The creature\'s attack rolls have Disadvantage.

### Charmed

- **Action Restriction:** The creature cannot attack the charmer or target the charmer with harmful abilities or magical effects.

- **Social:** The charmer has Advantage on any ability check to interact socially with the creature.

### Frightened

- **Ability Checks & Attacks:** The creature has Disadvantage on Ability Checks and Attack rolls while the source of its fear is within line of sight.

- **Movement:** The creature cannot willingly move closer to the source of its fear.

### Grappled

- **Movement:** Speed becomes 0, and it cannot benefit from any bonus to its speed.

- **Termination:** The condition ends if the grappler is incapacitated or if an effect removes the grappled creature from the reach of the grappler.

### Incapacitated

- **Actions:** An incapacitated creature cannot take Actions or Reactions.

### Invisible

- **Concealment:** The creature is impossible to see without the aid of magic or a special sense. For the purpose of hiding, the creature is heavily obscured.

- **Combat:** Attack rolls against the creature have Disadvantage. The creature\'s attack rolls have Advantage.

### Paralyzed

- **Actions:** The creature is Incapacitated (cannot take Actions or Reactions) and cannot move or speak.

- **Saving Throws:** Automatically fails Strength and Dexterity Saving Throws.

- **Combat:** Attack rolls against the creature have Advantage. Any Attack that hits the creature is a Critical Hit if the attacker is within 5 feet of the creature.

### Poisoned

- **Combat & Checks:** The creature has Disadvantage on Attack rolls and Ability Checks.

### Prone

- **Movement:** The creature\'s only movement option is to crawl (costs 2 feet of movement for every 1 foot) until it stands up. Standing up costs half its movement speed.

- **Combat (Defending):** Attack rolls against the creature have Advantage if the attacker is within 5 feet. Otherwise, the Attack roll has Disadvantage.

- **Combat (Attacking):** The creature has Disadvantage on Attack rolls.

### Restrained

- **Movement:** Speed becomes 0, and it cannot benefit from any bonus to its speed.

- **Combat:** Attack rolls against the creature have Advantage. The creature\'s attack rolls have Disadvantage.

- **Saving Throws:** The creature has Disadvantage on Dexterity Saving Throws.

### Stunned

- **Actions:** The creature is Incapacitated, cannot move, and can speak only falteringly.

- **Saving Throws:** Automatically fails Strength and Dexterity Saving Throws.

- **Combat:** Attack rolls against the creature have Advantage.

### Unconscious

- **State:** The creature is Incapacitated, cannot move or speak, and is unaware of its surroundings.

- **Held Items:** The creature drops whatever it is holding and falls Prone.

- **Saving Throws:** Automatically fails Strength and Dexterity Saving Throws.

- **Combat:** Attack rolls against the creature have Advantage. Any Attack that hits the creature is a Critical Hit if the attacker is within 5 feet.

## Agent Implementation Note

In the core:engine domain, represent these conditions as an enum class or a sealed hierarchy, and store them as a Set\<Condition\> on the Entity data class. Before calculating any dice roll (Attack, Save, or Check), the CombatAdjudicator must iterate through the active set and apply the relevant boolean flags (e.g., hasAdvantage, autoFails) to the dice roll operation.
