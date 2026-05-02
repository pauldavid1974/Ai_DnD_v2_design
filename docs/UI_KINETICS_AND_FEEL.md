# Game Feel, Kinetics, & Abstracted States {#game-feel-kinetics-abstracted-states}

**CRITICAL DIRECTIVE:** The success of this application relies entirely on masking network latency through sensory feedback. The UI must \"feel\" like a tabletop game, not a chatbot. Furthermore, the UI must act as a sensory bridge, translating raw math into colors and haptics.

## 1. State Machine Constraints (KStateMachine) {#state-machine-constraints-kstatemachine}

The UI MUST NOT rely on simple boolean flags (e.g., isLoading = true). All UI states must be governed by a strict, declarative Finite State Machine implemented via KStateMachine in the GameViewModel.

### Core UI States:

- AwaitingInput: Text field is active.

- Adjudicating.DeducingIntent: Network call 1 in flight. Show \"Thinking\...\" indicator.

- Adjudicating.RollingDice: Local engine is resolving math. Show brief dice math UI overlay.

- Adjudicating.GeneratingOutcome: Network call 2 in flight.

- StreamingNarration: Receiving SSE response. Lock text field. Typewriter effect active.

## 2. Semantic HP Mapping & Color Kinetics {#semantic-hp-mapping-color-kinetics}

To enforce the Neuro-Symbolic Boundary, the UI must translate exact integer Hit Points into semantic buckets before sending them to the LLM, and simultaneously use those buckets to shift the Compose UI color palette.

### The Mapping Table

| **Raw HP %** | **Semantic Bucket String** | **Compose Color Matrix Vignette**                  |
|--------------|----------------------------|----------------------------------------------------|
| 100% - 80%   | \"healthy\"                | Neutral / Invisible                                |
| 79% - 50%    | \"wounded\"                | Subtle amber tint at screen edges                  |
| 49% - 11%    | \"bloodied\"               | Deep crimson vignette                              |
| 10% - 1%     | \"near_death\"             | Pulsing crimson with slight grayscale desaturation |

- **Rule:** The LLM API receives the string \"bloodied\", NEVER 12/35 HP. The UI reads the same \"bloodied\" state from the ViewModel and triggers the Compose animation for the crimson vignette.

## 3. Haptic Primitives (Android Vibrator / HapticFeedback) {#haptic-primitives-android-vibrator-hapticfeedback}

The sensory experience relies on precise hardware actuation synchronized with the Two-Call Cycle and the typed JSON responses.

- **Resist (Light Tick):** Triggered continuously when dragging UI elements (like equipping an item) or when typing in the input field.

- **Expand (Rising Crescendo):** Triggered when a player successfully lands a hit or passes a saving throw.

- **Bounce (Heavy Thud):** Triggered at the exact millisecond the local Kotlin dice math concludes (Step 2 of the loop).

- **Wobble (Disorienting Spin):** Triggered when the player is inflicted with a negative status condition (e.g., Poisoned, Frightened).

## 4. The Typewriter Effect (LaunchedEffect) {#the-typewriter-effect-launchedeffect}

When receiving the final_narration string via SSE, the text must not appear instantly.

- Use a Compose LaunchedEffect paired with a delay(ms) coroutine to append characters to the displayed string one by one.

- The delay should be slightly randomized (e.g., between 15ms and 35ms) to simulate natural pacing.
