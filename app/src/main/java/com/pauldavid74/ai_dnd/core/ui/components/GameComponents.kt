package com.pauldavid74.ai_dnd.core.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
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
    val alpha = remember { Animatable(if (animate) 0f else 1f) }

    LaunchedEffect(animate) {
        if (animate) {
            alpha.animateTo(1f, animationSpec = tween(600))
        }
    }

    val paragraphs = remember(text) { text.split("\n\n").filter { it.isNotBlank() } }
    val letterSpacing = with(androidx.compose.ui.platform.LocalDensity.current) { 0.5.dp.toSp() }

    Column(
        modifier = modifier
            .padding(horizontal = 4.dp, vertical = 12.dp)
            .graphicsLayer(alpha = alpha.value),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        paragraphs.forEachIndexed { index, paragraph ->
            val isLast = index == paragraphs.size - 1
            Text(
                text = paragraph + if (isStreaming && isLast) "▌" else "",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                    fontStyle = FontStyle.Normal,
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.25f,
                    letterSpacing = letterSpacing
                ),
            )
        }
    }
}

// ── PlayerInputBlock ─────────────────────────────────────────────────────────
// Right-aligned bubble for player messages.

@Composable
fun PlayerInputBlock(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 4.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 2.dp
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Medium
                ),
            )
        }
    }
}

// ── SystemBlock ───────────────────────────────────────────────────────────────
// Centered monospace line for system events (conditions, general info, etc.)

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

// ── DiceRollBlock ─────────────────────────────────────────────────────────────
// A special "pill box" for dice rolls within the chat flow.

@Composable
fun DiceRollBlock(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Casino,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontWeight = FontWeight.Bold
                    ),
                )
            }
        }
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

// ── ActionBottomDrawer ────────────────────────────────────────────────────────
// Matches the reference: A header that expands into a list of actions.

@Composable
fun ActionBottomDrawer(
    choices: List<UiChoice>,
    enabled: Boolean,
    onChoice: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        // Drawer Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled && choices.isNotEmpty()) { expanded = !expanded }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Close, // Using Close/X as cross-swords placeholder
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Actions",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = CircleShape
            ) {
                Text(
                    text = "${choices.size}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Expanded Action List
        AnimatedVisibility(
            visible = expanded && choices.isNotEmpty(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                choices.forEach { choice ->
                    ActionItemRow(
                        choice = choice,
                        onClick = {
                            onChoice(choice.label)
                            expanded = false
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 48.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                }
                // Hardcoded "Something else..." if desired, or handled by AI
            }
        }
    }
}

@Composable
private fun ActionItemRow(
    choice: UiChoice,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconForChoice(choice),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.width(16.dp))
        Text(
            text = choice.label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

private fun iconForChoice(choice: UiChoice): ImageVector {
    val combined = (choice.label + " " + choice.actionType).lowercase()
    return when {
        combined.containsAny("attack", "fight", "strike", "slash", "shoot", "hit", "kill") -> Icons.Default.Gavel
        combined.containsAny("search", "look", "investig", "percep", "inspect", "scan", "find", "examine") -> Icons.Default.Search
        combined.containsAny("talk", "persuad", "deceiv", "social", "speak", "ask", "charm", "intimidate", "greet") -> Icons.Default.QuestionAnswer
        combined.containsAny("spell", "cast", "magic", "ritual", "arcane", "bless") -> Icons.Default.AutoAwesome
        combined.containsAny("run", "dash", "flee", "escape", "retreat", "travel", "go to", "walk") -> Icons.Default.DirectionsRun
        combined.containsAny("sneak", "hide", "stealth", "pickpocket") -> Icons.Default.VisibilityOff
        combined.containsAny("rest", "sleep", "camp", "heal", "potion") -> Icons.Default.Favorite
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
        GameUiStatus.DeducingIntent    -> "The Dungeon Master reads your intent…"
        GameUiStatus.AdjudicatingMath  -> "The dice fall…"
        GameUiStatus.GeneratingOutcome -> "The Dungeon Master speaks…"
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
