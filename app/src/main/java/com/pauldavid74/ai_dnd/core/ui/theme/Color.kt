package com.pauldavid74.ai_dnd.core.ui.theme

import androidx.compose.ui.graphics.Color

// ── V1 Design System Palette ─────────────────────────────────────────────────
// "Reading a well-bound book in warm lamplight, not working in a spreadsheet."
// Drop-in replacement for V2's Color.kt. All screen-level Color.Black /
// Color.White / Color.DarkGray hard-codes should be removed and replaced with
// MaterialTheme.colorScheme.* references after applying this file.

val Parchment    = Color(0xFFE8DDC7)   // Primary text on dark surfaces
val Ink          = Color(0xFF1C1814)   // Primary dark surface (background)
val Ember        = Color(0xFFC8662A)   // Primary accent
val Moss         = Color(0xFF5A7A4C)   // Success / healing
val Wine         = Color(0xFF7A2A2A)   // Damage / critical failure
val Slate        = Color(0xFF2A2824)   // Cards / panels  (surface)
val MutedInk     = Color(0xFF3A342C)   // Dividers / inactive (surfaceVariant)
val FadedGold    = Color(0xFFD4AF37)   // Dice readouts / XP / tertiary accent
val BloodCrimson = Color(0xFF8B0000)   // Deep damage / error container

// ── Material 3 Dark Theme Role Bindings ───────────────────────────────────────
// These map directly into AiDnDTheme → darkColorScheme(). Theme.kt is unchanged.

val md_theme_dark_primary              = Ember
val md_theme_dark_onPrimary            = Parchment
val md_theme_dark_primaryContainer     = Color(0xFF4A2010)
val md_theme_dark_onPrimaryContainer   = Parchment
val md_theme_dark_secondary            = Moss
val md_theme_dark_onSecondary          = Parchment
val md_theme_dark_secondaryContainer   = Color(0xFF1F3018)
val md_theme_dark_onSecondaryContainer = Parchment
val md_theme_dark_tertiary             = FadedGold
val md_theme_dark_onTertiary           = Ink
val md_theme_dark_tertiaryContainer    = Color(0xFF3A3000)
val md_theme_dark_onTertiaryContainer  = Parchment
val md_theme_dark_error                = Wine
val md_theme_dark_onError              = Parchment
val md_theme_dark_errorContainer       = BloodCrimson
val md_theme_dark_onErrorContainer     = Parchment
val md_theme_dark_background           = Ink
val md_theme_dark_onBackground         = Parchment
val md_theme_dark_surface              = Slate
val md_theme_dark_onSurface            = Parchment
val md_theme_dark_surfaceVariant       = MutedInk
val md_theme_dark_onSurfaceVariant     = Color(0xFFBBAA99)
val md_theme_dark_outline              = MutedInk
val md_theme_dark_outlineVariant       = Color(0xFF4A3F35)
val md_theme_dark_inverseSurface       = Parchment
val md_theme_dark_inverseOnSurface     = Ink
val md_theme_dark_inversePrimary       = Color(0xFF8B3A10)
val md_theme_dark_shadow               = Color(0xFF000000)
val md_theme_dark_surfaceTint          = Ember
val md_theme_dark_scrim                = Color(0xFF000000)
