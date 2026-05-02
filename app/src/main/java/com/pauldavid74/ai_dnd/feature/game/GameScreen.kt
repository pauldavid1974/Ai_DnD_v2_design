package com.pauldavid74.ai_dnd.feature.game

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pauldavid74.ai_dnd.core.ui.HapticManager
import kotlinx.coroutines.delay
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    characterId: Long,
    viewModel: GameViewModel = hiltViewModel(),
    onSettingsClick: () -> Unit,
    onCharacterSheetClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val hapticManager = remember { HapticManager(context) }

    LaunchedEffect(characterId) {
        viewModel.loadCampaign(characterId)
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    LaunchedEffect(state.kineticEffect) {
        state.kineticEffect?.let { effect ->
            when (effect) {
                KineticEffect.HeavyThud -> hapticManager.heavyThud()
                KineticEffect.LightClick -> hapticManager.lightTick()
                KineticEffect.SuccessCrescendo -> hapticManager.successCrescendo()
                KineticEffect.StatusWobble -> hapticManager.statusWobble()
                KineticEffect.GlitchUpdate -> hapticManager.lightTick()
            }
            viewModel.onEffectConsumed()
        }
    }

    LaunchedEffect(state.chatMessages.size) {
        if (state.chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(state.chatMessages.size - 1)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Black,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { 
                        Column {
                            Text(state.character?.name ?: "Adventure", color = Color.White, style = MaterialTheme.typography.titleLarge)
                            if (state.uiStatus != GameUiStatus.AwaitingInput) {
                                Text(
                                    text = state.uiStatus.toString().substringAfterLast("$"), 
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = onCharacterSheetClick) {
                            Icon(Icons.Default.Description, contentDescription = "Character Sheet", tint = Color.White)
                        }
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black,
                        titleContentColor = Color.White
                    )
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .background(Color.Black)
                        .padding(16.dp)
                ) {
                    if (state.availableChoices.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            state.availableChoices.forEach { choice ->
                                ActionChip(label = choice.label) { viewModel.onSendMessage(choice.label) }
                            }
                        }
                    } else if (state.uiStatus == GameUiStatus.AwaitingInput) {
                        Row(
                            modifier = Modifier.padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ActionChip("Attack") { viewModel.onSendMessage("Attack") }
                            ActionChip("Investigate") { viewModel.onSendMessage("Investigate") }
                            ActionChip("Talk") { viewModel.onSendMessage("Talk") }
                        }
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier.weight(1f),
                            enabled = state.uiStatus == GameUiStatus.AwaitingInput,
                            placeholder = { Text("What do you do?", color = Color.Gray, style = MaterialTheme.typography.bodyMedium) },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White)
                        )
                        IconButton(
                            onClick = { 
                                viewModel.onSendMessage(inputText)
                                inputText = ""
                            },
                            enabled = state.uiStatus == GameUiStatus.AwaitingInput && inputText.isNotBlank()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send, 
                                contentDescription = "Send",
                                tint = if (inputText.isNotBlank()) Color.White else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(state.chatMessages) { message ->
                    ChatBubble(message)
                }

                state.streamingMessage?.let { streaming ->
                    item {
                        ChatBubble(streaming)
                    }
                }
                
                if (state.uiStatus != GameUiStatus.AwaitingInput && state.streamingMessage == null) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        if (state.diceResult != null) {
            DiceChip(state.diceResult!!, Modifier.align(Alignment.Center))
        }

        if (state.uiStatus == GameUiStatus.Chronicling) {
            GlitchOverlay()
        }
    }
}

@Composable
fun ActionChip(label: String, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = Color.DarkGray,
            labelColor = Color.White
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
    )
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.sender == MessageSender.USER
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val shape = if (isUser) {
        MaterialTheme.shapes.medium.copy(bottomEnd = CornerSize(0.dp))
    } else {
        MaterialTheme.shapes.medium.copy(bottomStart = CornerSize(0.dp))
    }
    
    val color = when (message.sender) {
        MessageSender.USER -> Color(0xFF333333)
        MessageSender.AI -> Color.Black
        MessageSender.SYSTEM -> Color(0xFF222222)
    }

    val textColor = when (message.sender) {
        MessageSender.USER -> Color.White
        MessageSender.AI -> Color.White
        MessageSender.SYSTEM -> Color.LightGray
    }
    
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), 
        horizontalAlignment = alignment
    ) {
        Surface(
            color = color,
            shape = shape,
            tonalElevation = if (isUser) 2.dp else 1.dp,
            border = if (!isUser) BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)) else null
        ) {
            if (message.sender == MessageSender.AI) {
                TypewriterText(
                    text = message.content,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyLarge.copy(color = textColor)
                )
            } else {
                Text(
                    text = message.content,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyLarge.copy(color = textColor)
                )
            }
        }
    }
}

@Composable
fun TypewriterText(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle
) {
    var visibleText by remember { mutableStateOf("") }
    
    LaunchedEffect(text) {
        // If the incoming text is shorter than what we've already typed (e.g. a reset), reset visibility
        if (text.length < visibleText.length) {
            visibleText = ""
        }
        
        // Type out remaining characters
        for (i in visibleText.length until text.length) {
            visibleText += text[i]
            delay(Random.nextLong(15, 35)) // PRD mandated 15-35ms
        }
    }
    
    Text(text = visibleText, modifier = modifier, style = style)
}

@Composable
fun DiceChip(result: String, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(true) }
    
    LaunchedEffect(result) {
        visible = true
        delay(2000)
        visible = false
    }

    AnimatedVisibility(
        visible = visible,
        enter = scaleIn() + fadeIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier.padding(16.dp),
            color = Color.White,
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 12.dp,
            shadowElevation = 8.dp,
            border = BorderStroke(2.dp, Color.Black.copy(alpha = 0.5f))
        ) {
            Text(
                text = result,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                color = Color.Black,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        }
    }
}

@Composable
fun SemanticVignette(currentHp: Int, maxHp: Int) {
    val hpPercent = currentHp.toFloat() / maxHp.toFloat()
    
    val color = when {
        hpPercent <= 0.10f -> Color.Red
        hpPercent <= 0.49f -> Color.Red
        hpPercent <= 0.79f -> Color(0xFFD4AF37) // Amber
        else -> Color.Transparent
    }

    val opacity by animateFloatAsState(
        targetValue = when {
            hpPercent <= 0.10f -> 0.8f
            hpPercent <= 0.49f -> 0.4f
            hpPercent <= 0.79f -> 0.2f
            else -> 0f
        },
        animationSpec = if (hpPercent <= 0.10f) {
            infiniteRepeatable(animation = tween(800), repeatMode = RepeatMode.Reverse)
        } else {
            tween(500)
        }
    )

    if (opacity > 0f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.Transparent, color.copy(alpha = opacity)),
                        radius = 2500f
                    )
                )
        )
    }
}

@Composable
fun GlitchOverlay() {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(80, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = alpha))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            repeat(15) {
                drawLine(
                    color = Color.Cyan.copy(alpha = 0.4f),
                    start = androidx.compose.ui.geometry.Offset(0f, Random.nextFloat() * size.height),
                    end = androidx.compose.ui.geometry.Offset(size.width, Random.nextFloat() * size.height),
                    strokeWidth = 1f
                )
            }
        }
    }
}
