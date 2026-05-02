package com.pauldavid74.ai_dnd.feature.game.charactersheet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pauldavid74.ai_dnd.core.ui.theme.*
import com.pauldavid74.ai_dnd.feature.game.GameViewModel

// ── CharacterSheetScreen ──────────────────────────────────────────────────────
// Warm dark-fantasy styled sheet. Replaces the plain white list in V2.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterSheetScreen(
    characterId: Long,
    viewModel: GameViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(characterId) { viewModel.loadCampaign(characterId) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.character?.name ?: "Character Sheet",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        val character = state.character

        if (character == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Spacer(Modifier.height(4.dp))

                // ── Identity card ─────────────────────────────────────────────
                SheetSection {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = "Level ${character.level} ${character.characterClass}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Proficiency  +${character.proficiencyBonus}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "${character.experiencePoints} XP",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        }
                        // HP block
                        Column(horizontalAlignment = Alignment.End) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                val hpPct = character.currentHp.toFloat() / character.maxHp.toFloat()
                                val hpColor = when {
                                    hpPct >= 0.80f -> Moss
                                    hpPct >= 0.50f -> FadedGold
                                    hpPct >= 0.11f -> Ember
                                    else           -> Wine
                                }
                                Icon(
                                    Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = hpColor,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = "${character.currentHp} / ${character.maxHp}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = hpColor,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                )
                            }
                            if (character.temporaryHp > 0) {
                                Text(
                                    text = "+${character.temporaryHp} temp",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.secondary,
                                    ),
                                )
                            }
                            // HP bar
                            val hpPct = (character.currentHp.toFloat() / character.maxHp.toFloat()).coerceIn(0f, 1f)
                            val hpColor = when {
                                hpPct >= 0.80f -> Moss
                                hpPct >= 0.50f -> FadedGold
                                hpPct >= 0.11f -> Ember
                                else           -> Wine
                            }
                            LinearProgressIndicator(
                                progress = { hpPct },
                                modifier = Modifier.padding(top = 6.dp).width(100.dp).height(5.dp),
                                color = hpColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        }
                    }
                }

                // ── Ability Scores ────────────────────────────────────────────
                SheetLabel("Ability Scores")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AbilityCard("STR", character.strength, character.strengthModifier, Modifier.weight(1f))
                    AbilityCard("DEX", character.dexterity, character.dexterityModifier, Modifier.weight(1f))
                    AbilityCard("CON", character.constitution, character.constitutionModifier, Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AbilityCard("INT", character.intelligence, character.intelligenceModifier, Modifier.weight(1f))
                    AbilityCard("WIS", character.wisdom, character.wisdomModifier, Modifier.weight(1f))
                    AbilityCard("CHA", character.charisma, character.charismaModifier, Modifier.weight(1f))
                }

                // ── Class Features ────────────────────────────────────────────
                if (character.classFeatures.isNotEmpty()) {
                    SheetLabel("Class Features")
                    SheetSection {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            character.classFeatures.forEachIndexed { i, feature ->
                                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                Text(
                                    text = feature,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }

                // ── Spells ────────────────────────────────────────────────────
                if (character.spells.isNotEmpty()) {
                    SheetLabel("Spells")
                    SheetSection {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            character.spells.forEach { spell ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = spell,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = "✦",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.tertiary,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Inventory ─────────────────────────────────────────────────
                if (character.inventory.isNotEmpty()) {
                    SheetLabel("Inventory")
                    SheetSection {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            character.inventory.forEach { item ->
                                Text(
                                    text = "• $item",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ── Shared sheet composables ──────────────────────────────────────────────────

@Composable
private fun SheetSection(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp), content = content)
    }
}

@Composable
private fun SheetLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
private fun AbilityCard(label: String, score: Int, mod: Int, modifier: Modifier = Modifier) {
    val modText = if (mod >= 0) "+$mod" else "$mod"
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
            Text(
                text = score.toString(),
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                ),
                textAlign = TextAlign.Center,
            )
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(4.dp),
            ) {
                Text(
                    text = modText,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Medium,
                    ),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
