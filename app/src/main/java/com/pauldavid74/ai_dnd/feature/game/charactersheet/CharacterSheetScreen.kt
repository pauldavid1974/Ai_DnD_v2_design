package com.pauldavid74.ai_dnd.feature.game.charactersheet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import com.pauldavid74.ai_dnd.core.rules.CharacterSheetManager
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

import androidx.compose.material.icons.filled.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

private enum class SheetTab(val label: String) {
    MAIN("Main"),
    INVENTORY("Inventory"),
    SPELLS("Spells"),
    JOURNAL("Journal"),
    DICE("Dice"),
    FEATURES("Features"),
    PARTY("Party")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterSheetScreen(
    characterId: Long,
    viewModel: GameViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    var activeTab by remember { mutableStateOf(SheetTab.MAIN) }
    val sheetState = rememberModalBottomSheetState()

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
            ) {
                ScrollableTabRow(
                    selectedTabIndex = activeTab.ordinal,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary,
                    edgePadding = 16.dp,
                    divider = {},
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab.ordinal]),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                ) {
                    SheetTab.entries.forEach { tab ->
                        Tab(
                            selected = activeTab == tab,
                            onClick = { activeTab = tab },
                            text = { Text(tab.label, style = MaterialTheme.typography.labelLarge) }
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (activeTab) {
                        SheetTab.MAIN -> MainTab(character)
                        SheetTab.INVENTORY -> InventoryTab(character, onDetailClick = { viewModel.showDetail(it, "ITEM") })
                        SheetTab.SPELLS -> SpellbookTab(character, onDetailClick = { viewModel.showDetail(it, "SPELL") })
                        SheetTab.JOURNAL -> JournalTab(character)
                        SheetTab.DICE -> DiceTab(character)
                        SheetTab.FEATURES -> FeaturesTab(character, onDetailClick = { viewModel.showDetail(it, "FEATURE") })
                        SheetTab.PARTY -> PartyTab(character)
                    }
                }
            }
        }

        // ── Detail Bottom Sheet ───────────────────────────────────────────────
        if (state.selectedDetail != null || state.isDetailLoading) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.dismissDetail() },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outline) }
            ) {
                DetailContent(
                    detail = state.selectedDetail,
                    isLoading = state.isDetailLoading,
                    onDismiss = { viewModel.dismissDetail() }
                )
            }
        }
    }
}

@Composable
private fun DetailContent(
    detail: com.pauldavid74.ai_dnd.feature.game.DetailInfo?,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isLoading) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Text("Retrieving SRD lore...", style = MaterialTheme.typography.bodyMedium)
            }
        } else if (detail != null) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = detail.type,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = detail.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text(
                text = detail.content,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified // Default
            )
            
            Spacer(Modifier.height(8.dp))
            
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Close")
            }
        }
    }
}

@Composable
private fun MainTab(character: com.pauldavid74.ai_dnd.core.database.entity.CharacterEntity) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
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
                        text = "${character.species} • ${character.background} • ${character.alignment}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (character.originFeat.isNotBlank()) {
                        Text(
                            text = "Origin Feat: ${character.originFeat}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    Text(
                        text = "Proficiency  +${character.proficiencyBonus}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${character.experiencePoints} XP",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }

                // SRD 5.2.1 Core Derived Stats
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    DerivedStatChip("Initiative", "+${CharacterSheetManager.calculateInitiative(character)}")
                    DerivedStatChip("Speed", "${CharacterSheetManager.calculateSpeed(character)}ft")
                    DerivedStatChip("Passive Perc.", CharacterSheetManager.calculatePassivePerception(character).toString())
                }
            }
        }

        // ── Interactive Vitals (HP & Death Saves) ────────────────────
        SheetLabel("Vitals")
        SheetSection {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // HP block
                Column(modifier = Modifier.weight(1f)) {
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
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = "${character.currentHp} / ${character.maxHp}",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                color = hpColor,
                                fontWeight = FontWeight.Bold,
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
                        modifier = Modifier.padding(top = 8.dp).fillMaxWidth(0.8f).height(8.dp),
                        color = hpColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }

                // Death Saves
                Column(horizontalAlignment = Alignment.End) {
                    DeathSaveRow("Success", character.deathSaveSuccesses, 3, Moss)
                    DeathSaveRow("Failure", character.deathSaveFailures, 3, Wine)
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

        // ── Skills ───────────────────────────────────────────────────
        SheetLabel("Skills")
        SheetSection {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                CharacterSheetManager.getSkillModifiers(character).forEach { (skill, mod) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = skill,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (mod >= 0) "+$mod" else "$mod",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun InventoryTab(
    character: com.pauldavid74.ai_dnd.core.database.entity.CharacterEntity,
    onDetailClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SheetLabel("Equipment")
        SheetSection {
            if (character.inventory.isEmpty()) {
                Text("Your bag is empty.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                character.inventory.forEach { item ->
                    Text(
                        text = "• $item", 
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDetailClick(item) }
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }
        SheetLabel("Attunement (0/3)")
        SheetSection {
            Text("No items attuned.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SpellbookTab(
    character: com.pauldavid74.ai_dnd.core.database.entity.CharacterEntity,
    onDetailClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SheetLabel("Spells")
        SheetSection {
            if (character.spells.isEmpty()) {
                Text("You know no spells.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                character.spells.forEach { spell ->
                    Text(
                        text = "✦ $spell", 
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDetailClick(spell) }
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun JournalTab(character: com.pauldavid74.ai_dnd.core.database.entity.CharacterEntity, viewModel: GameViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SheetLabel("Campaign Logs")
        if (state.sessionMemories.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Text("The Chronicler is silent. No memories yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            state.sessionMemories.forEach { memory ->
                SheetSection {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = memory.type.replace("_", " "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = memory.key,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Text(
                            text = memory.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiceTab(character: com.pauldavid74.ai_dnd.core.database.entity.CharacterEntity, viewModel: GameViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SheetLabel("Quick Roll")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(4, 6, 8, 10, 12, 20, 100).forEach { sides ->
                DieButton(sides = sides, onRoll = { viewModel.rollDice(1, sides) }, modifier = Modifier.weight(1f))
            }
        }
        
        SheetLabel("History")
        SheetSection {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (state.rollHistory.isEmpty()) {
                    Text("No rolls yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    state.rollHistory.forEach { roll ->
                        Text(text = roll, style = MaterialTheme.typography.bodyMedium)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DieButton(sides: Int, onRoll: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onRoll,
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(4.dp),
        modifier = modifier.aspectRatio(1f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "d$sides",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun FeaturesTab(
    character: com.pauldavid74.ai_dnd.core.database.entity.CharacterEntity,
    onDetailClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SheetLabel("Class Features")
        SheetSection {
            if (character.classFeatures.isEmpty()) {
                Text("No class features found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                character.classFeatures.forEach { feature ->
                    Text(
                        text = "• $feature", 
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDetailClick(feature) }
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }
        SheetLabel("Weapon Masteries")
        SheetSection {
            if (character.weaponMasteries.isEmpty()) {
                Text("No weapon masteries.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                character.weaponMasteries.forEach { mastery ->
                    Text(
                        text = "⚔ $mastery", 
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDetailClick(mastery) }
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PartyTab(character: com.pauldavid74.ai_dnd.core.database.entity.CharacterEntity) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SheetLabel("Party Status")
        SheetSection {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("DM", color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
                Column {
                    Text("The Dungeon Master", style = MaterialTheme.typography.titleSmall)
                    Text("Adjudicating your fate...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Text(
                "Multiplayer party support coming in v2.1",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
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

@Composable
private fun DerivedStatChip(label: String, value: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(4.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun DeathSaveRow(label: String, count: Int, max: Int, activeColor: androidx.compose.ui.graphics.Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(50.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(max) { i ->
                Icon(
                    imageVector = if (i < count) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (i < count) activeColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            }
        }
    }
}
