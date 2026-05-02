package com.pauldavid74.ai_dnd.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pauldavid74.ai_dnd.core.security.KeyManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            snackbarHostState.showSnackbar("API Keys saved securely")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "AI Configuration",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                "API Keys are stored locally using Android EncryptedSharedPreferences.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            KeyField(
                label = "OpenAI API Key",
                value = state.openAiKey,
                onValueChange = viewModel::onOpenAiKeyChanged
            )

            KeyField(
                label = "Anthropic API Key",
                value = state.anthropicKey,
                onValueChange = viewModel::onAnthropicKeyChanged
            )

            KeyField(
                label = "Groq API Key",
                value = state.groqKey,
                onValueChange = viewModel::onGroqKeyChanged
            )

            Button(
                onClick = viewModel::saveKeys,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Keys")
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                "Model Selection",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )

            ProviderDropdown(
                selectedProviderId = state.selectedProviderId,
                onSelect = viewModel::onProviderSelected
            )

            if (state.isLoadingModels) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else if (state.availableModels.isNotEmpty()) {
                ModelDropdown(
                    selectedModelId = state.selectedModelId,
                    models = state.availableModels,
                    onSelect = viewModel::onModelSelected
                )
            } else {
                Text(
                    "No models found. Ensure your API key is saved and valid.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (state.error != null) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderDropdown(selectedProviderId: String, onSelect: (String) -> Unit) {
    val providers = listOf(
        KeyManager.PROVIDER_OPENAI,
        KeyManager.PROVIDER_ANTHROPIC,
        KeyManager.PROVIDER_GROQ
    )
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedProviderId.uppercase(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Active Provider") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            providers.forEach { providerId ->
                DropdownMenuItem(
                    text = { Text(providerId.uppercase()) },
                    onClick = {
                        onSelect(providerId)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelDropdown(selectedModelId: String, models: List<com.pauldavid74.ai_dnd.core.network.model.AiModel>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedModelId,
            onValueChange = {},
            readOnly = true,
            label = { Text("Active Model") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            models.forEach { model ->
                DropdownMenuItem(
                    text = { Text(model.name) },
                    onClick = {
                        onSelect(model.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun KeyField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true
    )
}
