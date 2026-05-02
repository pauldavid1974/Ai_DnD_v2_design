package com.pauldavid74.ai_dnd.feature.pregame.character

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pauldavid74.ai_dnd.core.ui.theme.Ember
import com.pauldavid74.ai_dnd.core.ui.theme.MutedInk

// ── CharacterCreationScreen ───────────────────────────────────────────────────
// Adds a "Quick Start" tab (12 SRD archetypes, one tap) alongside the existing
// stat-builder form (now labelled "Custom"). Both tabs produce a CharacterEntity
// via the same CharacterCreationViewModel.

private enum class CreationTab { QUICK_START, CUSTOM }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterCreationScreen(
    viewModel: CharacterCreationViewModel = hiltViewModel(),
    onSettingsClick: () -> Unit,
    onComplete: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    var activeTab by remember { mutableStateOf(CreationTab.QUICK_START) }

    LaunchedEffect(state.isComplete) {
        if (state.isComplete && state.createdCharacterId != null) {
            onComplete(state.createdCharacterId!!)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Create Your Hero",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            // ── Tab row ────────────────────────────────────────────────────────
            TabRow(
                selectedTabIndex = activeTab.ordinal,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab.ordinal]),
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
            ) {
                Tab(
                    selected = activeTab == CreationTab.QUICK_START,
                    onClick = { activeTab = CreationTab.QUICK_START },
                    text = {
                        Text(
                            "Quick Start",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    },
                )
                Tab(
                    selected = activeTab == CreationTab.CUSTOM,
                    onClick = { activeTab = CreationTab.CUSTOM },
                    text = {
                        Text(
                            "Custom",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    },
                )
            }

            // ── Tab content ────────────────────────────────────────────────────
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tab_content",
            ) { tab ->
                when (tab) {
                    CreationTab.QUICK_START -> QuickStartTab(
                        state = state,
                        onArchetypeSelected = { archetype ->
                            viewModel.onNameChanged(archetype.name)
                            viewModel.onClassChanged(archetype.characterClass)
                            archetype.applyStats(viewModel)
                        },
                        onNameChanged = viewModel::onNameChanged,
                        onConfirm = viewModel::saveCharacter,
                    )
                    CreationTab.CUSTOM -> CustomTab(
                        state = state,
                        viewModel = viewModel,
                    )
                }
            }
        }
    }
}

// ── Quick Start Tab ───────────────────────────────────────────────────────────

@Composable
private fun QuickStartTab(
    state: CharacterCreationState,
    onArchetypeSelected: (Archetype) -> Unit,
    onNameChanged: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    var selectedArchetype by remember { mutableStateOf<Archetype?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Pick a ready-made hero and name them. One tap and you're in.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontStyle = FontStyle.Italic,
            ),
        )

        // Archetype grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.heightIn(max = 640.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(ARCHETYPES) { archetype ->
                ArchetypeCard(
                    archetype = archetype,
                    selected = selectedArchetype == archetype,
                    onClick = {
                        selectedArchetype = archetype
                        onArchetypeSelected(archetype)
                    },
                )
            }
        }

        // Name field (appears after archetype selected)
        AnimatedVisibility(visible = selectedArchetype != null) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Text(
                    text = "Name your ${selectedArchetype?.characterClass ?: "hero"}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                OutlinedTextField(
                    value = state.name,
                    onValueChange = onNameChanged,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            selectedArchetype?.defaultName ?: "Enter a name",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontStyle = FontStyle.Italic,
                            ),
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        cursorColor = MaterialTheme.colorScheme.primary,
                    ),
                    singleLine = true,
                )
                Button(
                    onClick = onConfirm,
                    enabled = !state.isSaving && state.name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            "Begin the Adventure",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }

        if (state.error != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(10.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ArchetypeCard(
    archetype: Archetype,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outline

    Surface(
        onClick = onClick,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
        tonalElevation = if (selected) 4.dp else 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = archetype.icon,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Start,
            )
            Text(
                text = archetype.displayName,
                style = MaterialTheme.typography.titleSmall.copy(
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Text(
                text = archetype.characterClass,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
            Text(
                text = archetype.role,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = FontStyle.Italic,
                ),
            )
        }
    }
}

// ── Custom Tab ────────────────────────────────────────────────────────────────
// The original V2 stat-builder form, re-styled.

@Composable
private fun CustomTab(
    state: CharacterCreationState,
    viewModel: CharacterCreationViewModel,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        OutlinedTextField(
            value = state.name,
            onValueChange = viewModel::onNameChanged,
            label = { Text("Character Name") },
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
            ),
            isError = state.name.isBlank() && state.error != null,
        )

        val classes = listOf(
            "Barbarian", "Bard", "Cleric", "Druid", "Fighter",
            "Monk", "Paladin", "Ranger", "Rogue", "Sorcerer", "Warlock", "Wizard",
        )
        Text(
            "Class",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        ClassDropdown(
            selectedClass = state.characterClass,
            options = classes,
            onSelect = viewModel::onClassChanged,
            isError = state.characterClass.isBlank() && state.error != null,
        )

        Text(
            "Generation Method",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GenerationMethod.entries.forEach { method ->
                FilterChip(
                    selected = state.generationMethod == method,
                    onClick = { viewModel.onMethodChanged(method) },
                    label = { Text(method.name.replace("_", " ")) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
        }

        if (state.generationMethod == GenerationMethod.POINT_BUY) {
            Text(
                text = "Points Remaining: ${state.pointsRemaining}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.pointsRemaining >= 0) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.error,
            )
        }

        listOf("Strength", "Dexterity", "Constitution", "Intelligence", "Wisdom", "Charisma")
            .forEach { stat ->
                StatRow(
                    name = stat,
                    value = getStatValue(state, stat),
                    onValueChange = { viewModel.updateStat(stat, it) },
                    method = state.generationMethod,
                )
            }

        if (state.error != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(10.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Button(
            onClick = viewModel::saveCharacter,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            enabled = !state.isSaving && state.isValid,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Text("Create Character", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

// ── Archetype Data ────────────────────────────────────────────────────────────

data class Archetype(
    val displayName: String,
    val characterClass: String,
    val species: String,
    val role: String,
    val icon: String,
    val defaultName: String,
    // SRD standard array [15,14,13,12,10,8] allocated per class focus
    val str: Int, val dex: Int, val con: Int,
    val int: Int, val wis: Int, val cha: Int,
    val hitDie: Int,
) {
    fun applyStats(viewModel: CharacterCreationViewModel) {
        viewModel.updateStat("Strength", str)
        viewModel.updateStat("Dexterity", dex)
        viewModel.updateStat("Constitution", con)
        viewModel.updateStat("Intelligence", int)
        viewModel.updateStat("Wisdom", wis)
        viewModel.updateStat("Charisma", cha)
    }
}

val ARCHETYPES = listOf(
    Archetype("Steel Knight",    "Fighter",   "Human",      "Frontline tank",       "⚔",  "Aldric",   15, 13, 14, 8,  12, 10, 10),
    Archetype("Shadow Blade",    "Rogue",     "Elf",        "Stealth skirmisher",   "🗡",  "Sable",    10, 15, 14, 13, 12, 8,  8),
    Archetype("Spell Scholar",   "Wizard",    "Gnome",      "Ranged nuker",         "📖", "Elara",    8,  14, 13, 15, 12, 10, 6),
    Archetype("Dawn Shepherd",   "Cleric",    "Halfling",   "Healer / support",     "☀",  "Mira",     13, 10, 14, 12, 15, 8,  8),
    Archetype("Wild Tracker",    "Ranger",    "Elf",        "Ranged skirmisher",    "🏹", "Fenris",   13, 15, 12, 10, 14, 8,  10),
    Archetype("Storm Berserker", "Barbarian", "Goliath",    "Pure bruiser",         "🌩", "Grak",     15, 13, 14, 8,  12, 10, 12),
    Archetype("Silver-Tongue",   "Bard",      "Human",      "Face / utility",       "🎭", "Vesper",   8,  14, 12, 13, 10, 15, 8),
    Archetype("Dragon-Blooded",  "Sorcerer",  "Dragonborn", "Burst caster",         "🐉", "Skarrex",  8,  14, 13, 12, 10, 15, 6),
    Archetype("Fiend-Pact",      "Warlock",   "Tiefling",   "Mid-range caster",     "🔥", "Mordecai", 8,  14, 13, 12, 10, 15, 8),
    Archetype("Moon Druid",      "Druid",     "Elf",        "Versatile caster",     "🌙", "Sylva",    10, 13, 14, 12, 15, 8,  8),
    Archetype("Open Hand",       "Monk",      "Human",      "Mobile striker",       "🥋", "Tenzin",   13, 15, 14, 10, 12, 8,  8),
    Archetype("Oath Knight",     "Paladin",   "Dwarf",      "Tank / smiter",        "🛡",  "Vayne",    15, 10, 13, 8,  12, 14, 10),
)

// Reuse helpers from original CharacterCreationScreen.kt unchanged below:
// ClassDropdown, StatRow, StatDropdown, getStatValue
// (they compile from the same package — no duplication needed)
