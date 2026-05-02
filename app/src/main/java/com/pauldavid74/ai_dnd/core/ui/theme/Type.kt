package com.pauldavid74.ai_dnd.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Font Families ────────────────────────────────────────────────────────────
// Currently uses system fonts as functional stand-ins.
// To upgrade to Google Fonts (Cormorant Garamond / Lora / Inter / JetBrains Mono):
//   1. Add to app/build.gradle.kts:
//        implementation("androidx.compose.ui:ui-text-google-fonts:1.7.0")
//   2. Create res/values/font_certs.xml (copy from the Google Fonts sample app or
//      https://github.com/android/compose-samples — search "com_google_android_gms_fonts_certs")
//   3. Replace each FontFamily below with e.g.:
//        val provider = GoogleFont.Provider("com.google.android.gms.fonts", "com.google.android.gms", R.array.com_google_android_gms_fonts_certs)
//        val NarrationDisplayFamily = FontFamily(Font(GoogleFont("Cormorant Garamond"), provider, weight = FontWeight.Bold))

/** Headers, scene titles — target: Cormorant Garamond */
val NarrationDisplayFamily: FontFamily = FontFamily.Serif

/** Body narration text — target: Lora */
val NarrationBodyFamily: FontFamily = FontFamily.Serif

/** UI chrome (buttons, chips, nav) — target: Inter */
val UiChromeFamily: FontFamily = FontFamily.SansSerif

/** Dice readouts, raw stats — target: JetBrains Mono */
val MonospaceFamily: FontFamily = FontFamily.Monospace

// ── Typography Scale ─────────────────────────────────────────────────────────
val Typography = Typography(
    // Scene titles / chapter headings
    displayLarge = TextStyle(
        fontFamily = NarrationDisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = NarrationDisplayFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = NarrationDisplayFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = NarrationDisplayFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = NarrationDisplayFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    ),
    // Top bar character name, card titles
    titleLarge = TextStyle(
        fontFamily = NarrationDisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = UiChromeFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = UiChromeFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    // Narration body text (17sp, 1.5 line-height — PRD §13.2)
    bodyLarge = TextStyle(
        fontFamily = NarrationBodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.3.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = NarrationBodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = UiChromeFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    // Chip labels, button text, UI chrome
    labelLarge = TextStyle(
        fontFamily = UiChromeFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = UiChromeFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    // Dice readouts, modifier breakdowns — monospace
    labelSmall = TextStyle(
        fontFamily = MonospaceFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
    ),
)
