package com.pauldavid74.ai_dnd.core.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.pauldavid74.ai_dnd.core.network.model.UiChoice
import com.pauldavid74.ai_dnd.core.ui.theme.*
import com.pauldavid74.ai_dnd.feature.game.GameUiStatus
import kotlinx.coroutines.delay
import kotlin.random.Random

// ── NarrationBlock ───────────────────────────────────────────────────────────
// Renders AI narrator text in the V1 style: serif body, no bubble background,
// subtle paragraph fade. Pass animate=true only for the freshest message.

@Composable
fun NarrationBlock(
    text: String,
    modifier: Modifier = Modifier,
    animate: Boolean = false,
    isStreaming: Boolean = false,
) {
    val displayText = if (animate && !isStreaming) {
        rememberTypewriterText(text)
    } else {
        text
    }

    Column(modifier = modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
        Text(
            text = displayText + if (isStreaming) "▌" else "",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onBackground,
                fontStyle = FontStyle.Normal,
            ),
        )
    }
}

@Composable
private fun rememberTypewriterText(fullText: String): String {
    var visible by remember(fullText) { mutableStateOf("") }
    LaunchedEffect(fullText) {
        if (fullText.length < visible.length) visible = ""
        for (i in visible.length until fullText.length) {
            visible += fullText[i]
            delay(Random.nextLong(15, 35))
        }
    }
    return visible
}

// ── PlayerInputBlock ─────────────────────────────────────────────────────────
// Right-aligned bubble for player messages.

@Composable
fun PlayerInputBlock(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
            )
        }
    }
}

// ── SystemBlock ───────────────────────────────────────────────────────────────
// Centered monospace line for system events (roll results, conditions, etc.)

@Composable
fun SystemBlock(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.tertiary,
            ),
        )
    }
}

// ── DiceRollChip ─────────────────────────────────────────────────────────────
// Full-screen overlay chip — pops up when a dice result is ready, then fades.
// Place inside a Box that fills the screen; the chip centres itself.

@Composable
fun DiceRollChip(result: String?, modifier: Modifier = Modifier) {
    var visible by remember(result) { mutableStateOf(result != null) }

    LaunchedEffect(result) {
        if (result != null) {
            visible = true
            delay(2400)
            visible = false
        }
    }

    AnimatedVisibility(
        visible = visible && result != null,
        enter = scaleIn(spring(dampingRatio = 0.5f)) + fadeIn(),
        exit = fadeOut(tween(300)) + scaleOut(tween(300)),
        modifier = modifier,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.tertiary),
            shadowElevation = 12.dp,
            tonalElevation = 8.dp,
        ) {
            Text(
                text = result ?: "",
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                ),
            )
        }
    }
}

// ── ChoiceChipBar ─────────────────────────────────────────────────────────────
// Horizontally scrollable row of action chips. Icons are inferred from label
// content. Extend iconForChoice() as needed when PromptFactory's actionType
// values are finalized.

@Composable
fun ChoiceChipBar(
    choices: List<UiChoice>,
    enabled: Boolean,
    onChoice: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (choices.isEmpty()) return

    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        choices.forEach { choice ->
            FantasyChip(
                label = choice.label,
                icon = iconForChoice(choice),
                enabled = enabled,
                onClick = { onChoice(choice.label) },
            )
        }
    }
}

@Composable
private fun FantasyChip(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (enabled) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outline

    Surface(
        onClick = onClick,
        enabled = enabled,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, borderColor),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (enabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

private fun iconForChoice(choice: UiChoice): ImageVector {
    val combined = (choice.label + " " + choice.actionType).lowercase()
    return when {
        combined.containsAny("attack", "fight", "strike", "slash", "shoot") -> Icons.Default.Warning
        combined.containsAny("search", "look", "investig", "percep", "inspect", "scan") -> Icons.Default.Search
        combined.containsAny("talk", "persuad", "deceiv", "social", "speak", "ask", "charm") -> Icons.Default.MailOutline
        combined.containsAny("spell", "cast", "magic", "ritual") -> Icons.Default.Star
        combined.containsAny("run", "dash", "flee", "escape", "retreat") -> Icons.Default.Refresh
        combined.containsAny("sneak", "hide", "stealth") -> Icons.Default.VisibilityOff
        else -> Icons.Default.Edit
    }
}

private fun String.containsAny(vararg terms: String) = terms.any { this.contains(it) }

// ── HpPip ─────────────────────────────────────────────────────────────────────
// Small coloured heart + fraction for the top bar.

@Composable
fun HpPip(currentHp: Int, maxHp: Int, modifier: Modifier = Modifier) {
    val pipColor = hpColor(currentHp, maxHp)
    val pulsing = currentHp.toFloat() / maxHp.toFloat() <= 0.10f
    val pulseAlpha by if (pulsing) {
        val inf = rememberInfiniteTransition(label = "hp_pulse")
        inf.animateFloat(
            initialValue = 0.6f, targetValue = 1f, label = "pulse",
            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Favorite,
            contentDescription = "HP",
            tint = pipColor.copy(alpha = pulseAlpha),
            modifier = Modifier.size(13.dp),
        )
        Text(
            text = "$currentHp/$maxHp",
            style = MaterialTheme.typography.labelSmall.copy(color = pipColor.copy(alpha = pulseAlpha)),
        )
    }
}

private fun hpColor(currentHp: Int, maxHp: Int): Color {
    if (maxHp == 0) return Moss
    val pct = currentHp.toFloat() / maxHp.toFloat()
    return when {
        pct >= 0.80f -> Moss
        pct >= 0.50f -> FadedGold
        pct >= 0.11f -> Ember
        else -> Wine
    }
}

// ── StatusIndicator ───────────────────────────────────────────────────────────
// Animated label shown below the character name during the two-call cycle.

@Composable
fun StatusIndicator(status: GameUiStatus, modifier: Modifier = Modifier) {
    val label = when (status) {
        GameUiStatus.DeducingIntent    -> "The narrator reads your intent…"
        GameUiStatus.AdjudicatingMath  -> "The dice fall…"
        GameUiStatus.GeneratingOutcome -> "The narrator speaks…"
        GameUiStatus.Chronicling       -> "The Chronicler stirs…"
        else                           -> null
    } ?: return

    val alpha by rememberInfiniteTransition(label = "status_pulse").animateFloat(
        initialValue = 0.5f, targetValue = 1f, label = "alpha",
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
    )

    Text(
        text = label,
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
            fontStyle = FontStyle.Italic,
        ),
    )
}
