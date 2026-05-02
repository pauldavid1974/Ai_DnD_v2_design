package com.pauldavid74.ai_dnd.feature.pregame.character

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterCreationScreen(
    viewModel: CharacterCreationViewModel = hiltViewModel(),
    onSettingsClick: () -> Unit,
    onComplete: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.isComplete) {
        if (state.isComplete && state.createdCharacterId != null) {
            onComplete(state.createdCharacterId!!)
        }
    }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("Character Creation", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChanged,
                label = { Text("Character Name") },
                modifier = Modifier.fillMaxWidth(),
                isError = state.name.isBlank() && state.error != null
            )

            // Class selection Dropdown
            val classes = listOf("Barbarian", "Bard", "Cleric", "Druid", "Fighter", "Monk", "Paladin", "Ranger", "Rogue", "Sorcerer", "Warlock", "Wizard")
            Text("Class", style = MaterialTheme.typography.titleMedium)
            ClassDropdown(
                selectedClass = state.characterClass, 
                options = classes, 
                onSelect = viewModel::onClassChanged,
                isError = state.characterClass.isBlank() && state.error != null
            )

            Text("Generation Method", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GenerationMethod.entries.forEach { method ->
                    FilterChip(
                        selected = state.generationMethod == method,
                        onClick = { viewModel.onMethodChanged(method) },
                        label = { Text(method.name.replace("_", " ")) }
                    )
                }
            }

            if (state.generationMethod == GenerationMethod.POINT_BUY) {
                Text(
                    text = "Points Remaining: ${state.pointsRemaining}", 
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.pointsRemaining >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }

            val stats = listOf("Strength", "Dexterity", "Constitution", "Intelligence", "Wisdom", "Charisma")
            stats.forEach { stat ->
                StatRow(
                    name = stat,
                    value = getStatValue(state, stat),
                    onValueChange = { viewModel.updateStat(stat, it) },
                    method = state.generationMethod
                )
            }

            if (state.error != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = state.error!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Button(
                onClick = viewModel::saveCharacter,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                enabled = !state.isSaving && state.isValid
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Create Character")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassDropdown(selectedClass: String, options: List<String>, onSelect: (String) -> Unit, isError: Boolean = false) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedClass,
            onValueChange = {},
            readOnly = true,
            label = { Text("Select Class") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            isError = isError,
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text = option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@Composable
fun StatRow(name: String, value: Int, onValueChange: (Int) -> Unit, method: GenerationMethod) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        
        if (method == GenerationMethod.POINT_BUY) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { if (value > 8) onValueChange(value - 1) }) {
                    Text("-", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    text = value.toString(),
                    modifier = Modifier.padding(horizontal = 12.dp),
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = { if (value < 15) onValueChange(value + 1) }) {
                    Text("+", style = MaterialTheme.typography.titleLarge)
                }
            }
        } else {
            StatDropdown(value = value, onValueChange = onValueChange)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatDropdown(value: Int, onValueChange: (Int) -> Unit) {
    val array = listOf(15, 14, 13, 12, 10, 8)
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.width(120.dp)
    ) {
        OutlinedTextField(
            value = value.toString(),
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.menuAnchor(),
            textStyle = MaterialTheme.typography.bodyLarge
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            array.forEach { arrayValue ->
                DropdownMenuItem(
                    text = { Text(text = arrayValue.toString()) },
                    onClick = {
                        onValueChange(arrayValue)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

private fun getStatValue(state: CharacterCreationState, stat: String): Int {
    return when (stat.lowercase()) {
        "strength" -> state.strength
        "dexterity" -> state.dexterity
        "constitution" -> state.constitution
        "intelligence" -> state.intelligence
        "wisdom" -> state.wisdom
        "charisma" -> state.charisma
        else -> 8
    }
}
