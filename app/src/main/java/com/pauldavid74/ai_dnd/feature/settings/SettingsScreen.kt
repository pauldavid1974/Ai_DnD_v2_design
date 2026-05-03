package com.pauldavid74.ai_dnd.feature.settings

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pauldavid74.ai_dnd.core.network.model.AiModel
import com.pauldavid74.ai_dnd.core.network.model.LLMProvider
import com.pauldavid74.ai_dnd.core.ui.theme.Ember
import com.pauldavid74.ai_dnd.core.ui.theme.Moss
import com.pauldavid74.ai_dnd.core.ui.theme.Wine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    onAboutClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("AI Configuration") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 20.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Step 1: Select Provider
            SetupStep(
                number = 1,
                title = "Choose AI Provider",
                isComplete = state.selectedProviderId.isNotEmpty()
            ) {
                ProviderDropdown(
                    selectedId = state.selectedProviderId,
                    savedProviders = state.savedProviders,
                    onSelect = viewModel::onProviderSelected
                )
            }

            // Step 2: API Key
            AnimatedVisibility(visible = state.selectedProviderId.isNotEmpty()) {
                SetupStep(
                    number = 2,
                    title = "Verify API Key",
                    isComplete = state.isCurrentKeySaved
                ) {
                    KeyVerificationSection(
                        provider = state.currentProvider,
                        currentKey = state.currentKey,
                        currentUrl = state.currentUrl,
                        isVerifying = state.isVerifying,
                        isSaved = state.isCurrentKeySaved,
                        result = state.currentVerificationResult,
                        onKeyChange = viewModel::onKeyChanged,
                        onUrlChange = viewModel::onUrlChanged,
                        onVerify = viewModel::verifyKeyAndFetchModels
                    )
                }
            }

            // Step 3: Choose Model
            AnimatedVisibility(visible = state.isCurrentKeySaved) {
                SetupStep(
                    number = 3,
                    title = "Select Chat Model",
                    isComplete = state.isCurrentModelSaved
                ) {
                    ModelSelectionSection(
                        selectedId = state.currentSelectedModel,
                        models = state.currentModels,
                        isSaved = state.isCurrentModelSaved,
                        onSelect = viewModel::onModelSelected,
                        onSave = viewModel::saveModel
                    )
                }
            }

            // Final Step: Start Adventure
            AnimatedVisibility(visible = state.isCurrentModelSaved) {
                Button(
                    onClick = onNavigateToHome,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Ready for Adventure", style = MaterialTheme.typography.titleMedium)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // About & Legal
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Legal & Attributions", style = MaterialTheme.typography.titleMedium)
                Surface(
                    onClick = onAboutClick,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("About the App", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text("Campaign licenses and attributions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.ChevronRight, null)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SetupStep(
    number: Int,
    title: String,
    isComplete: Boolean,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(
                shape = CircleShape,
                color = if (isComplete) Moss else MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isComplete) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = Color.White)
                    } else {
                        Text(number.toString(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        }
        
        Box(modifier = Modifier.padding(start = 40.dp)) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderDropdown(selectedId: String, savedProviders: Set<String>, onSelect: (String) -> Unit) {
    val providers = LLMProvider.ALL_PROVIDERS
    var expanded by remember { mutableStateOf(false) }
    val currentProvider = providers.find { it.id == selectedId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = currentProvider?.name ?: "Select a Provider",
            onValueChange = {},
            readOnly = true,
            label = { Text("Active Provider") },
            leadingIcon = { 
                if (currentProvider != null) {
                    Icon(currentProvider.icon, null, tint = MaterialTheme.colorScheme.primary) 
                } else {
                    Icon(Icons.Default.CloudQueue, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            providers.forEach { provider ->
                val isSetup = savedProviders.contains(provider.id)
                DropdownMenuItem(
                    text = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(provider.name, style = MaterialTheme.typography.bodyLarge)
                                if (isSetup) {
                                    Icon(Icons.Default.CheckCircle, "Configured", tint = Moss, modifier = Modifier.size(16.dp))
                                }
                            }
                            Text(provider.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    leadingIcon = { Icon(provider.icon, null) },
                    onClick = {
                        onSelect(provider.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun KeyVerificationSection(
    provider: LLMProvider?,
    currentKey: String,
    currentUrl: String,
    isVerifying: Boolean,
    isSaved: Boolean,
    result: VerificationResult?,
    onKeyChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onVerify: (String) -> Unit
) {
    if (provider == null) return
    var showKey by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (provider.isCustom) {
            OutlinedTextField(
                value = currentUrl,
                onValueChange = onUrlChange,
                label = { Text("Custom Base URL (e.g. http://localhost:1234/v1)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )
        }

        OutlinedTextField(
            value = currentKey,
            onValueChange = onKeyChange,
            label = { Text("${provider.name} API Key") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (showKey) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showKey = !showKey }) {
                    Icon(if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                }
            },
            shape = RoundedCornerShape(8.dp)
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AnimatedContent(targetState = isSaved, label = "verify_btn") { saved ->
                if (saved) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.VpnKey, null, tint = Moss, modifier = Modifier.size(20.dp))
                        Text("Key Saved", color = Moss, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                } else if (currentKey.isNotBlank() && (!provider.isCustom || currentUrl.isNotBlank())) {
                    Button(
                        onClick = { onVerify(currentKey) },
                        enabled = !isVerifying,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isVerifying) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Text("Verify & Save Key")
                        }
                    }
                }
            }

            if (!isSaved && result is VerificationResult.Failure) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Error, null, tint = Wine, modifier = Modifier.size(20.dp))
                    Text("Fail", color = Wine, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        
        if (!isSaved && result is VerificationResult.Failure) {
            Text(result.message, style = MaterialTheme.typography.labelSmall, color = Wine)
        }
    }
}

@Composable
private fun ModelSelectionSection(
    selectedId: String,
    models: List<AiModel>,
    isSaved: Boolean,
    onSelect: (String) -> Unit,
    onSave: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ModelDropdown(selectedModelId = selectedId, models = models, onSelect = onSelect)
        
        if (models.isEmpty()) {
            Text(
                text = if (selectedId.isBlank()) 
                    "No chat models found. You can still enter a model name manually above." 
                    else "Could not fetch model list. Manual input is active.", 
                color = MaterialTheme.colorScheme.onSurfaceVariant, 
                style = MaterialTheme.typography.labelSmall
            )
        }
            
        AnimatedContent(targetState = isSaved, label = "save_btn") { saved ->
            if (saved) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.BookmarkAdded, null, tint = Moss, modifier = Modifier.size(20.dp))
                    Text("Model Active", color = Moss, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            } else if (selectedId.isNotBlank()) {
                Button(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Ember)
                ) {
                    Text("Set as Active Model")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelDropdown(selectedModelId: String, models: List<AiModel>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val currentModel = models.find { it.id == selectedModelId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = currentModel?.name ?: selectedModelId,
            onValueChange = { onSelect(it) },
            readOnly = models.isNotEmpty(),
            label = { Text("Model Selector") },
            placeholder = { Text(if (models.isEmpty()) "Enter model name manually" else "Select a Model") },
            trailingIcon = { 
                if (models.isNotEmpty()) {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) 
                }
            },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        )
        if (models.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                models.forEach { model ->
                    val isSelected = model.id == selectedModelId
                    DropdownMenuItem(
                        text = { 
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(model.name)
                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, "Selected", tint = Moss, modifier = Modifier.size(16.dp))
                                    Text("Selected", color = Moss, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        },
                        onClick = {
                            onSelect(model.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
