package com.pauldavid74.ai_dnd.feature.pregame.levelup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelUpScreen(
    characterId: Long,
    viewModel: LevelUpViewModel = hiltViewModel(),
    onComplete: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var featureInput by remember { mutableStateOf("") }
    var spellInput by remember { mutableStateOf("") }

    LaunchedEffect(characterId) {
        viewModel.loadCharacter(characterId)
    }

    LaunchedEffect(state.isComplete) {
        if (state.isComplete) {
            onComplete()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Level Up: ${state.character?.name ?: ""}") }) }
    ) { padding ->
        if (state.character == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text("Level ${state.character!!.level} -> ${state.character!!.level + 1}", 
                        style = MaterialTheme.typography.headlineSmall)
                }

                item {
                    Text("Add New Features", style = MaterialTheme.typography.titleMedium)
                    Row {
                        OutlinedTextField(
                            value = featureInput,
                            onValueChange = { featureInput = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("e.g. Action Surge") }
                        )
                        IconButton(onClick = { 
                            viewModel.addFeature(featureInput)
                            featureInput = ""
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Feature")
                        }
                    }
                }

                items(state.newFeatures) { feature ->
                    ListItem(headlineContent = { Text(feature) })
                }

                item {
                    Text("Add New Spells", style = MaterialTheme.typography.titleMedium)
                    Row {
                        OutlinedTextField(
                            value = spellInput,
                            onValueChange = { spellInput = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("e.g. Fireball") }
                        )
                        IconButton(onClick = { 
                            viewModel.addSpell(spellInput)
                            spellInput = ""
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Spell")
                        }
                    }
                }

                items(state.newSpells) { spell ->
                    ListItem(headlineContent = { Text(spell) })
                }

                item {
                    if (state.error != null) {
                        Text(state.error!!, color = MaterialTheme.colorScheme.error)
                    }

                    Button(
                        onClick = viewModel::levelUp,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isSaving
                    ) {
                        if (state.isSaving) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        else Text("Confirm Level Up")
                    }
                }
            }
        }
    }
}
