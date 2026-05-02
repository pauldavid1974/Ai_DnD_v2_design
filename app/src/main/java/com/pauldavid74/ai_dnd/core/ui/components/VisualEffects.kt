package com.pauldavid74.ai_dnd.core.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.pauldavid74.ai_dnd.core.ui.theme.Wine
import com.pauldavid74.ai_dnd.feature.game.KineticEffect
import kotlin.random.Random

// ── SemanticVignette ─────────────────────────────────────────────────────────
// Fullscreen radial vignette driven by the character's current HP percentage.
// Place this as the LAST child of the root Box in GameScreen so it renders
// above all content but below the DiceRollChip overlay.
//
// Usage:
//   val character = state.character
//   if (character != null) {
//       SemanticVignette(currentHp = character.currentHp, maxHp = character.maxHp)
//   }

@Composable
fun SemanticVignette(currentHp: Int, maxHp: Int, modifier: Modifier = Modifier) {
    if (maxHp == 0) return
    val pct = currentHp.toFloat() / maxHp.toFloat()

    val targetColor = when {
        pct <= 0.10f -> Wine                    // near_death  — deep crimson
        pct <= 0.49f -> Color(0xFF8B1A1A)       // bloodied    — crimson
        pct <= 0.79f -> Color(0xFFD4AF37)       // wounded     — amber
        else         -> Color.Transparent
    }

    val targetAlpha = when {
        pct <= 0.10f -> 0.75f
        pct <= 0.49f -> 0.40f
        pct <= 0.79f -> 0.18f
        else         -> 0f
    }

    // Near-death: pulse. Otherwise: static tween.
    val animAlpha by if (pct <= 0.10f) {
        rememberInfiniteTransition(label = "vignette_pulse").animateFloat(
            initialValue = 0.35f,
            targetValue = targetAlpha,
            label = "vignette",
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        )
    } else {
        animateFloatAsState(targetValue = targetAlpha, animationSpec = tween(500), label = "vignette")
    }

    if (animAlpha > 0.01f) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            targetColor.copy(alpha = animAlpha),
                        ),
                        radius = 2800f,
                    ),
                ),
        )
    }
}

// ── ShakeModifier ─────────────────────────────────────────────────────────────
// Attach to any Modifier to shake the composable when a triggering KineticEffect
// is received from the ViewModel.
//
// Usage:
//   Modifier.shakeOnEffect(state.kineticEffect)
//
// The effect is consumed in GameScreen via viewModel.onEffectConsumed() — this
// modifier only reads the value and reacts; it does not consume it.

fun Modifier.shakeOnEffect(effect: KineticEffect?): Modifier = composed {
    val offsetX = remember { Animatable(0f) }

    LaunchedEffect(effect) {
        if (effect == KineticEffect.HeavyThud || effect == KineticEffect.StatusWobble) {
            val amplitudes = if (effect == KineticEffect.HeavyThud)
                listOf(18f, -14f, 10f, -7f, 4f, -2f, 0f)
            else
                listOf(6f, -10f, 8f, -6f, 4f, -3f, 2f, -1f, 0f)

            for (amp in amplitudes) {
                offsetX.animateTo(amp, animationSpec = tween(38, easing = LinearEasing))
            }
        }
    }

    this.graphicsLayer { translationX = offsetX.value }
}

// ── GlitchOverlay ─────────────────────────────────────────────────────────────
// Rendered while GameUiStatus == Chronicling. Draws animated scanlines over
// the full screen to indicate the background Chronicler agent is running.

@Composable
fun GlitchOverlay(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "glitch")
    val alpha by transition.animateFloat(
        initialValue = 0f,
        targetValue = 0.25f,
        label = "glitch_alpha",
        animationSpec = infiniteRepeatable(
            animation = tween(70, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
    )
    // Seed changes every frame to randomise line positions — intentional.
    val lineSeed by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        label = "glitch_seed",
        animationSpec = infiniteRepeatable(tween(120, easing = LinearEasing)),
    )

    Box(modifier = modifier.fillMaxSize()) {
        // Dark flicker layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = alpha)),
        )
        // Cyan scanlines
        Canvas(modifier = Modifier.fillMaxSize()) {
            val rng = Random(lineSeed.toInt())
            repeat(12) {
                val y = rng.nextFloat() * size.height
                drawLine(
                    color = Color.Cyan.copy(alpha = 0.30f),
                    start = androidx.compose.ui.geometry.Offset(0f, y),
                    end = androidx.compose.ui.geometry.Offset(size.width, y),
                    strokeWidth = 1f,
                )
            }
        }
    }
}
