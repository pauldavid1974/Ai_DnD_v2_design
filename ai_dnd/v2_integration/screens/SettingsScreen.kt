package com.pauldavid74.ai_dnd.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pauldavid74.ai_dnd.core.security.KeyManager
import com.pauldavid74.ai_dnd.core.ui.theme.*

// ── SettingsScreen ────────────────────────────────────────────────────────────
// V1-styled settings: provider status chips, show/hide key toggles, model picker.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) snackbarHostState.showSnackbar("Keys saved securely ✓")
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
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
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            // ── API Keys section ──────────────────────────────────────────────
            SettingsLabel("AI Providers")
            Text(
                text = "Keys are stored locally using Android EncryptedSharedPreferences and never transmitted to any server except your chosen provider.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = FontStyle.Italic,
                ),
            )

            ProviderKeyCard(
                providerName = "OpenAI",
                providerId = KeyManager.PROVIDER_OPENAI,
                keyValue = state.openAiKey,
                onKeyChange = viewModel::onOpenAiKeyChanged,
            )
            ProviderKeyCard(
                providerName = "Anthropic",
                providerId = KeyManager.PROVIDER_ANTHROPIC,
                keyValue = state.anthropicKey,
                onKeyChange = viewModel::onAnthropicKeyChanged,
            )
            ProviderKeyCard(
                providerName = "Groq",
                providerId = KeyManager.PROVIDER_GROQ,
                keyValue = state.groqKey,
                onKeyChange = viewModel::onGroqKeyChanged,
            )

            Button(
                onClick = viewModel::saveKeys,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text("Save Keys", style = MaterialTheme.typography.labelLarge)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            // ── Active provider & model ───────────────────────────────────────
            SettingsLabel("Active Provider")

            // Provider selector — segmented row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    KeyManager.PROVIDER_OPENAI to "OpenAI",
                    KeyManager.PROVIDER_ANTHROPIC to "Anthropic",
                    KeyManager.PROVIDER_GROQ to "Groq",
                ).forEach { (id, label) ->
                    val selected = state.selectedProviderId == id
                    Surface(
                        onClick = { viewModel.onProviderSelected(id) },
                        modifier = Modifier.weight(1f),
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            if (selected) 2.dp else 1.dp,
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline,
                        ),
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(vertical = 10.dp),
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            }

            // Model picker
            SettingsLabel("Model")
            if (state.isLoadingModels) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            } else if (state.availableModels.isNotEmpty()) {
                ThemedModelDropdown(
                    selectedModelId = state.selectedModelId,
                    models = state.availableModels,
                    onSelect = viewModel::onModelSelected,
                )
            } else {
                Text(
                    text = "No models found. Save a valid API key and select a provider.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.error,
                        fontStyle = FontStyle.Italic,
                    ),
                )
            }

            AnimatedVisibility(visible = state.error != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = state.error ?: "",
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── ProviderKeyCard ───────────────────────────────────────────────────────────

@Composable
private fun ProviderKeyCard(
    providerName: String,
    providerId: String,
    keyValue: String,
    onKeyChange: (String) -> Unit,
) {
    var showKey by remember { mutableStateOf(false) }
    val isConfigured = keyValue.isNotBlank()

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = providerName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                // Status chip
                Surface(
                    color = if (isConfigured) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(50),
                ) {
                    Text(
                        text = if (isConfigured) "● Configured" else "○ Not set",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isConfigured) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }

            OutlinedTextField(
                value = keyValue,
                onValueChange = onKeyChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "Paste API key…",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontStyle = FontStyle.Italic,
                        ),
                    )
                },
                textStyle = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                visualTransformation = if (showKey) VisualTransformation.None
                else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showKey = !showKey }) {
                        Icon(
                            imageVector = if (showKey) Icons.Default.VisibilityOff
                            else Icons.Default.Visibility,
                            contentDescription = if (showKey) "Hide key" else "Show key",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    cursorColor = MaterialTheme.colorScheme.primary,
                ),
                singleLine = true,
            )
        }
    }
}

// ── ThemedModelDropdown ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemedModelDropdown(
    selectedModelId: String,
    models: List<com.pauldavid74.ai_dnd.core.network.model.AiModel>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        OutlinedTextField(
            value = selectedModelId,
            onValueChange = {},
            readOnly = true,
            label = { Text("Active Model") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
            ),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            models.forEach { model ->
                DropdownMenuItem(
                    text = {
                        Text(
                            model.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    onClick = { onSelect(model.id); expanded = false },
                )
            }
        }
    }
}

// ── SettingsLabel ──────────────────────────────────────────────────────────────

@Composable
private fun SettingsLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
    )
}
