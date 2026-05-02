package com.pauldavid74.ai_dnd.feature.game.charactersheet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pauldavid74.ai_dnd.feature.game.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterSheetScreen(
    characterId: Long,
    viewModel: GameViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(characterId) {
        viewModel.loadCampaign(characterId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.character?.name ?: "Character Sheet") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val character = state.character
        if (character == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Info
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Level ${character.level} ${character.characterClass}", style = MaterialTheme.typography.titleMedium)
                        Text("HP: ${character.currentHp}/${character.maxHp}", style = MaterialTheme.typography.bodyLarge)
                        Text("Proficiency Bonus: +${character.proficiencyBonus}", style = MaterialTheme.typography.bodySmall)
                    }
                }

                // Ability Scores
                Text("Ability Scores", style = MaterialTheme.typography.headlineSmall)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AbilityScoreCard("STR", character.strength, character.strengthModifier, Modifier.weight(1f))
                    AbilityScoreCard("DEX", character.dexterity, character.dexterityModifier, Modifier.weight(1f))
                    AbilityScoreCard("CON", character.constitution, character.constitutionModifier, Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AbilityScoreCard("INT", character.intelligence, character.intelligenceModifier, Modifier.weight(1f))
                    AbilityScoreCard("WIS", character.wisdom, character.wisdomModifier, Modifier.weight(1f))
                    AbilityScoreCard("CHA", character.charisma, character.charismaModifier, Modifier.weight(1f))
                }

                // Features & Spells
                if (character.classFeatures.isNotEmpty()) {
                    Text("Features", style = MaterialTheme.typography.headlineSmall)
                    character.classFeatures.forEach { feature ->
                        ListItem(headlineContent = { Text(feature) })
                    }
                }

                if (character.spells.isNotEmpty()) {
                    Text("Spells", style = MaterialTheme.typography.headlineSmall)
                    character.spells.forEach { spell ->
                        ListItem(headlineContent = { Text(spell) })
                    }
                }
            }
        }
    }
}

@Composable
fun AbilityScoreCard(label: String, score: Int, mod: Int, modifier: Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(score.toString(), style = MaterialTheme.typography.titleLarge)
            val modText = if (mod >= 0) "+$mod" else mod.toString()
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = modText,
                    modifier = Modifier.padding(horizontal = 4.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
