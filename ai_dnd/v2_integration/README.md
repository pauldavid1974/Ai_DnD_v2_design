# V1 UI → V2 App Integration

Replaces every screen's hard-coded `Color.Black / Color.White / Color.DarkGray`
with the V1 warm-parchment design language — keeping 100% of V2's engine,
ViewModel, KStateMachine, Chronicler, and domain layer untouched.

---

## Files and where they go

| This file | Replaces / adds |
|---|---|
| `theme/Color.kt` | `core/ui/theme/Color.kt` |
| `theme/Type.kt` | `core/ui/theme/Type.kt` |
| `components/GameComponents.kt` | NEW — add to `core/ui/components/` |
| `components/VisualEffects.kt` | NEW — add to `core/ui/components/` |
| `screens/GameScreen.kt` | `feature/game/GameScreen.kt` |
| `screens/HomeScreen.kt` | `feature/home/HomeScreen.kt` |
| `screens/CharacterCreationScreen.kt` | `feature/pregame/character/CharacterCreationScreen.kt` |
| `screens/CharacterSheetScreen.kt` | `feature/game/charactersheet/CharacterSheetScreen.kt` |

`Theme.kt` **does not change** — it already wires the `md_theme_dark_*`
variables properly. Updating `Color.kt` is all that's needed to fix the palette.

---

## Step-by-step

### 1. Copy files
Copy each file above into its destination path inside the V2 project.
The two new component files go into a new directory:
```
app/src/main/java/com/pauldavid74/ai_dnd/core/ui/components/
```
Create that directory if it doesn't exist.

### 2. Update imports in GameScreen.kt
The new `GameScreen.kt` imports from `core.ui.components`:
```kotlin
import com.pauldavid74.ai_dnd.core.ui.components.*
```
Android Studio will flag any import it can't resolve — use Alt+Enter to
re-import or check the package name matches your build exactly.

### 3. Fix the Send icon
The new `GameInputBar` uses `Icons.Default.Menu` as a placeholder for the
send button icon (the AutoMirrored.Send icon reference was removed to avoid
an extra import). Swap it for the correct icon once the file is in place:
```kotlin
// In GameInputBar, change:
imageVector = Icons.Default.Menu,
// to:
imageVector = Icons.AutoMirrored.Filled.Send,
// (add import androidx.compose.material.icons.automirrored.filled.Send)
```

### 4. (Optional) Custom fonts
`Type.kt` uses `FontFamily.Serif / SansSerif / Monospace` as system
stand-ins. To use the PRD-specified typefaces:

**a.** Add to `app/build.gradle.kts`:
```kotlin
implementation("androidx.compose.ui:ui-text-google-fonts:1.7.0")
```

**b.** Create `app/src/main/res/values/font_certs.xml` — copy the GMS font
   certificate array from the official [Compose Google Fonts sample](https://github.com/android/compose-samples/blob/main/Crane/app/src/main/res/values/font_certs.xml).

**c.** In `Type.kt` replace each `FontFamily.*` constant with:
```kotlin
val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage   = "com.google.android.gms",
    certificates      = R.array.com_google_android_gms_fonts_certs,
)
val NarrationDisplayFamily = FontFamily(
    Font(GoogleFont("Cormorant Garamond"), provider, weight = FontWeight.Bold),
    Font(GoogleFont("Cormorant Garamond"), provider, weight = FontWeight.Normal),
)
val NarrationBodyFamily = FontFamily(
    Font(GoogleFont("Lora"), provider),
)
val UiChromeFamily = FontFamily(
    Font(GoogleFont("Inter"), provider),
)
val MonospaceFamily = FontFamily(
    Font(GoogleFont("JetBrains Mono"), provider),
)
```

### 5. CharacterCreationScreen — ViewModel wiring
The Quick Start tab calls `archetype.applyStats(viewModel)` which calls
`viewModel.updateStat(name, value)` for each ability score. Confirm that
`CharacterCreationViewModel.updateStat()` accepts a `String` stat name and
an `Int` value — that's the existing V2 signature, so no ViewModel changes
are needed.

The `ARCHETYPES` list and `Archetype` data class live at the **bottom** of
`CharacterCreationScreen.kt`. `ClassDropdown`, `StatRow`, `StatDropdown`,
and `getStatValue` are reused from the original file without duplication
(they're in the same package / file).

---

## What is NOT changed

| Layer | Status |
|---|---|
| `GameViewModel.kt` | ✅ Unchanged |
| `KStateMachineConfig.kt` | ✅ Unchanged |
| `GameState.kt` | ✅ Unchanged |
| All `core/rules/*` | ✅ Unchanged |
| All `core/domain/*` | ✅ Unchanged |
| All `core/database/*` | ✅ Unchanged |
| All `core/network/*` | ✅ Unchanged |
| `core/security/KeyManager.kt` | ✅ Unchanged |
| `core/ui/VisualLaw.kt` | ✅ Unchanged |
| `core/ui/HapticManager.kt` | ✅ Unchanged |
| `core/ui/theme/Theme.kt` | ✅ Unchanged |
| `SettingsScreen.kt` | ✅ Unchanged (styling improves automatically via theme) |
| `LevelUpScreen.kt` | ✅ Unchanged (styling improves automatically via theme) |

---

## Visual changes summary

| Before (V2) | After |
|---|---|
| Pure `Color.Black` backgrounds | Warm Ink `#1C1814` via theme |
| `Color.White` text everywhere | Parchment `#E8DDC7` via theme |
| Plain `AssistChip` with no icons | `FantasyChip` (pill, Ember border, action icons) |
| AI messages in dark-grey bubble | `NarrationBlock` — serif, no bubble, typewriter on newest only |
| `SemanticVignette` defined but never called | Wired to live HP in `GameScreen` |
| No HP display in top bar | `HpPip` with colour-coded heart + fraction |
| No FSM state label in top bar | `StatusIndicator` italic label during two-call cycle |
| Plain card list on Home | HP bar per hero, warm-styled cards |
| Character creation: stat form only | Quick Start tab with 12 archetype cards |
| Character sheet: plain list | Styled ability cards, HP bar, section labels |
| `GlitchOverlay` not layered correctly | Properly positioned as topmost layer in root `Box` |
