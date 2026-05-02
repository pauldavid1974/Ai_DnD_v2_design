package com.pauldavid74.ai_dnd.feature.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import com.pauldavid74.ai_dnd.core.database.entity.CharacterEntity
import com.pauldavid74.ai_dnd.core.ui.theme.*
import kotlinx.coroutines.launch

// ── HomeScreen ────────────────────────────────────────────────────────────────
// Warm dark fantasy home. Replaces V2's plain Material3 default styling.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNewCampaign: () -> Unit,
    onContinueCampaign: (Long) -> Unit,
    onSettingsClick: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "AI Dungeon Master",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Text(
                                text = "SRD 5.2.1",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.tertiary,
                                ),
                            )
                        }
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
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = onNewCampaign,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = {
                        Text("New Adventure", style = MaterialTheme.typography.labelLarge)
                    },
                )
            },
        ) { padding ->
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                ) {
                    if (state.characters.isEmpty()) {
                        item { EmptyState() }
                    } else {
                        item {
                            Text(
                                text = "Heroes",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                        }
                        items(state.characters) { character ->
                            CharacterCard(
                                character = character,
                                onClick = { onContinueCampaign(character.id) },
                                onDelete = {
                                    viewModel.deleteCharacter(character)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("'${character.name}' removed")
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }

        // ── API Setup Guard Overlay ──────────────────────────────────────────
        if (state.isApiKeyMissing) {
            SetupGuardOverlay(onSettingsClick = onSettingsClick)
        }
    }
}

@Composable
private fun SetupGuardOverlay(onSettingsClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            /* Emoji removed as requested */
            Text(
                text = "Summon the Dungeon Master",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "The Dungeon Master remains beyond the veil. To begin your adventure, you must provide an API key in the settings.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                fontStyle = FontStyle.Italic
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onSettingsClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Open Settings")
            }
        }
    }
}

// ── CharacterCard ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun CharacterCard(
    character: CharacterEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    val hpPct = character.currentHp.toFloat() / character.maxHp.toFloat()
    val hpColor = when {
        hpPct >= 0.80f -> Moss
        hpPct >= 0.50f -> FadedGold
        hpPct >= 0.11f -> Ember
        else -> Wine
    }

    Box {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = { showMenu = true }),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp, MaterialTheme.colorScheme.outline,
            ),
            tonalElevation = 2.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Left: name + class/level
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = character.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Level ${character.level} ${character.characterClass}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Right: HP pip
                Column(horizontalAlignment = Alignment.End) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            tint = hpColor,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = "${character.currentHp}/${character.maxHp}",
                            style = MaterialTheme.typography.labelSmall.copy(color = hpColor),
                        )
                    }
                    // HP bar
                    LinearProgressIndicator(
                        progress = { hpPct.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .width(80.dp)
                            .height(4.dp),
                        color = hpColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }
        }

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text("Delete Character", color = MaterialTheme.colorScheme.error) },
                onClick = { showMenu = false; showConfirm = true },
                leadingIcon = {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                },
            )
        }

        if (showConfirm) {
            AlertDialog(
                onDismissRequest = { showConfirm = false },
                containerColor = MaterialTheme.colorScheme.surface,
                title = {
                    Text("Delete Character", color = MaterialTheme.colorScheme.onSurface)
                },
                text = {
                    Text(
                        "Remove ${character.name} permanently?",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                confirmButton = {
                    TextButton(onClick = { onDelete(); showConfirm = false }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirm = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
            )
        }
    }
}

// ── EmptyState ────────────────────────────────────────────────────────────────

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "⚔",
            style = MaterialTheme.typography.displayMedium.copy(
                color = MaterialTheme.colorScheme.outline,
            ),
        )
        Text(
            text = "No heroes yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Tap 'New Adventure' to begin your quest.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
    }
}
