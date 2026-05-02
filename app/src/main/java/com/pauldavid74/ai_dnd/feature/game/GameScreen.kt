package com.pauldavid74.ai_dnd.feature.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pauldavid74.ai_dnd.core.ui.HapticManager
import com.pauldavid74.ai_dnd.core.ui.components.*

// ── GameScreen ────────────────────────────────────────────────────────────────
// Drop-in replacement for V2's GameScreen.kt. Applies V1 visual language:
//  • Ink/Parchment palette via MaterialTheme (no hardcoded Color.Black)
//  • NarrationBlock (serif body, typewriter on newest message only)
//  • PlayerInputBlock (right-aligned pill)
//  • SystemBlock (monospace, centred — used for roll summaries)
//  • ChoiceChipBar with action-type icons
//  • HpPip + StatusIndicator in top bar
//  • SemanticVignette fullscreen overlay driven by character HP
//  • ShakeModifier on the chat list triggered by HeavyThud / StatusWobble
//  • GlitchOverlay during Chronicling state
//  • DiceRollChip centred overlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    characterId: Long,
    viewModel: GameViewModel = hiltViewModel(),
    onSettingsClick: () -> Unit,
    onCharacterSheetClick: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val hapticManager = remember { HapticManager(context) }

    // ── Side effects ──────────────────────────────────────────────────────────
    LaunchedEffect(characterId) { viewModel.loadCampaign(characterId) }

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(state.kineticEffect) {
        state.kineticEffect?.let { effect ->
            when (effect) {
                KineticEffect.HeavyThud       -> hapticManager.heavyThud()
                KineticEffect.LightClick      -> hapticManager.lightTick()
                KineticEffect.SuccessCrescendo -> hapticManager.successCrescendo()
                KineticEffect.StatusWobble    -> hapticManager.statusWobble()
                KineticEffect.GlitchUpdate    -> hapticManager.lightTick()
            }
            viewModel.onEffectConsumed()
        }
    }

    LaunchedEffect(state.chatMessages.size) {
        if (state.chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(state.chatMessages.size - 1)
        }
    }

    // Index of the last AI message — only this one gets typewriter animation
    val lastAiIndex = state.chatMessages.indexOfLast { it.sender == MessageSender.AI }

    // ── Root Box (layers: scaffold → vignette → dice chip → glitch) ───────────
    Box(modifier = Modifier.fillMaxSize()) {

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                GameTopBar(
                    characterName = state.character?.name ?: "Adventure",
                    currentHp = state.character?.currentHp,
                    maxHp = state.character?.maxHp,
                    uiStatus = state.uiStatus,
                    onMenuClick = onCharacterSheetClick,
                    onSettingsClick = onSettingsClick,
                    onUndoClick = { viewModel.onUndo() },
                )
            },
            bottomBar = {
                GameInputBar(
                    choices = state.availableChoices,
                    inputText = inputText,
                    isEnabled = state.uiStatus == GameUiStatus.AwaitingInput,
                    onInputChange = { inputText = it },
                    onSend = {
                        if (inputText.isNotBlank()) {
                            viewModel.onSendMessage(inputText)
                            inputText = ""
                        }
                    },
                    onChipClick = { viewModel.onSendMessage(it) },
                )
            },
        ) { padding ->

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .shakeOnEffect(state.kineticEffect)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
            ) {
                itemsIndexed(state.chatMessages) { index, message ->
                    when (message.sender) {
                        MessageSender.AI ->
                            NarrationBlock(
                                text = message.content,
                                animate = (index == lastAiIndex) && state.streamingMessage == null,
                            )
                        MessageSender.USER ->
                            PlayerInputBlock(text = message.content)
                        MessageSender.SYSTEM ->
                            if (message.content.startsWith("DICE_ROLL:")) {
                                DiceRollBlock(text = message.content.removePrefix("DICE_ROLL:"))
                            } else {
                                SystemBlock(text = message.content)
                            }
                    }
                }

                // Live streaming bubble
                state.streamingMessage?.let { streaming ->
                    item {
                        NarrationBlock(
                            text = streaming.content,
                            isStreaming = true,
                        )
                    }
                }

                // Thinking indicator (no streaming text yet)
                if (state.uiStatus != GameUiStatus.AwaitingInput && state.streamingMessage == null) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }

        // Semantic vignette — rendered above scaffold content
        state.character?.let { char ->
            SemanticVignette(currentHp = char.currentHp, maxHp = char.maxHp)
        }

        // Dice roll overlay
        DiceRollChip(
            result = state.diceResult,
            modifier = Modifier.align(Alignment.Center)
        )

        // Chronicler glitch — fullscreen, topmost
        if (state.uiStatus == GameUiStatus.Chronicling) {
            GlitchOverlay()
        }
    }
}

// ── GameTopBar ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameTopBar(
    characterName: String,
    currentHp: Int?,
    maxHp: Int?,
    uiStatus: GameUiStatus,
    onMenuClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onUndoClick: () -> Unit,
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Description, contentDescription = "Character Sheet")
            }
        },
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = characterName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (currentHp != null && maxHp != null) {
                    HpPip(currentHp = currentHp, maxHp = maxHp)
                }
                StatusIndicator(status = uiStatus)
            }
        },
        actions = {
            IconButton(onClick = onUndoClick) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
            }
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        },
    )
}

// ── GameInputBar ──────────────────────────────────────────────────────────────

@Composable
private fun GameInputBar(
    choices: List<com.pauldavid74.ai_dnd.core.network.model.UiChoice>,
    inputText: String,
    isEnabled: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onChipClick: (String) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .imePadding()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
        ) {
            // Action drawer (from AI response)
            ActionBottomDrawer(
                choices = choices,
                enabled = isEnabled,
                onChoice = onChipClick,
            )

            // Text input row
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    modifier = Modifier.weight(1f),
                    enabled = isEnabled,
                    placeholder = {
                        Text(
                            text = "What do you do?",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontStyle = FontStyle.Italic,
                            ),
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        cursorColor = MaterialTheme.colorScheme.primary,
                    ),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 5,
                )

                FilledIconButton(
                    onClick = onSend,
                    enabled = isEnabled && inputText.isNotBlank(),
                    modifier = Modifier.size(52.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
