package eu.tutorials.mybizz.Chatbot

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import eu.tutorials.mybizz.Chatbot.model.ChatMessage
import eu.tutorials.mybizz.Chatbot.model.SourceDocument
import eu.tutorials.mybizz.ui.theme.AccentBlue
import eu.tutorials.mybizz.ui.theme.AccentGold
import eu.tutorials.mybizz.ui.theme.BotBubble
import eu.tutorials.mybizz.ui.theme.CardDark
import eu.tutorials.mybizz.ui.theme.DarkNavy
import eu.tutorials.mybizz.ui.theme.DeepBlue
import eu.tutorials.mybizz.ui.theme.ErrorBubble
import eu.tutorials.mybizz.ui.theme.InputBg
import eu.tutorials.mybizz.ui.theme.MyBizzTheme
import eu.tutorials.mybizz.ui.theme.TextPrimary
import eu.tutorials.mybizz.ui.theme.TextSecondary
import eu.tutorials.mybizz.ui.theme.UserBubble
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel = viewModel()) {
    val messages by viewModel.messages.collectAsState()
    val isTyping by viewModel.isTyping.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Auto scroll to bottom on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    MyBizzTheme {
        Scaffold(
            containerColor = DarkNavy,
            topBar = { ChatTopBar(onClearChat = { viewModel.clearChat() }) },
            bottomBar = {
                ChatInputBar(
                    text = inputText,
                    isLoading = isTyping,
                    onTextChange = { inputText = it },
                    onSend = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendMessage(inputText.trim())
                            inputText = ""
                            scope.launch {
                                if (messages.isNotEmpty())
                                    listState.animateScrollToItem(messages.size - 1)
                            }
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    ChatBubble(message = message)
                }

                if (isTyping) {
                    item { TypingIndicator() }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(onClearChat: () -> Unit) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = DeepBlue,
            titleContentColor = TextPrimary
        ),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(AccentBlue, AccentGold))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "MyBizz Assistant",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        "AI powered by Groq",
                        color = AccentGold,
                        fontSize = 11.sp
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onClearChat) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Clear chat",
                    tint = TextSecondary
                )
            }
        }
    )
}

@Composable
fun ChatBubble(message: ChatMessage) {
    var showSources by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isFromUser) Alignment.End else Alignment.Start
    ) {
        // Bubble
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (message.isFromUser) 18.dp else 4.dp,
                        bottomEnd = if (message.isFromUser) 4.dp else 18.dp
                    )
                )
                .background(
                    when {
                        message.isFromUser -> UserBubble
                        message.isError -> ErrorBubble
                        else -> BotBubble
                    }
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.content,
                color = TextPrimary,
                fontSize = 14.sp,
                lineHeight = 21.sp
            )
        }

        // Sources toggle button
        if (!message.isFromUser && message.sources.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            TextButton(
                onClick = { showSources = !showSources },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Icon(
                    if (showSources) Icons.Default.Refresh else Icons.Default.Build,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = AccentGold
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "${message.sources.size} source${if (message.sources.size > 1) "s" else ""}",
                    color = AccentGold,
                    fontSize = 11.sp
                )
            }

            // Sources list
            AnimatedVisibility(visible = showSources) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    message.sources.forEach { source ->
                        SourceChip(source)
                    }
                }
            }
        }
    }
}

@Composable
fun SourceChip(source: SourceDocument) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CardDark)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Create,
            contentDescription = null,
            tint = AccentBlue,
            modifier = Modifier.size(12.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            "${source.sheetName} · Row ${source.rowNumber}",
            color = TextSecondary,
            fontSize = 11.sp
        )
    }
}

@Composable
fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val dot1 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f, label = "d1",
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse)
    )
    val dot2 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f, label = "d2",
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 200), RepeatMode.Reverse)
    )
    val dot3 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f, label = "d3",
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 400), RepeatMode.Reverse)
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(BotBubble)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(dot1, dot2, dot3).forEach { alpha ->
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(TextSecondary.copy(alpha = alpha))
            )
        }
    }
}

@Composable
fun ChatInputBar(
    text: String,
    isLoading: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        color = DeepBlue,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalAlignment = Alignment.Bottom
        ) {
            TextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp)),
                placeholder = {
                    Text("Ask about bills, tenants, payments...", color = TextSecondary, fontSize = 13.sp)
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = InputBg,
                    unfocusedContainerColor = InputBg,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = AccentBlue
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                maxLines = 4,
                enabled = !isLoading
            )

            Spacer(Modifier.width(8.dp))

            // Send Button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (text.isNotBlank() && !isLoading)
                            Brush.linearGradient(listOf(AccentBlue, Color(0xFF1D4ED8)))
                        else
                            Brush.linearGradient(listOf(CardDark, CardDark))
                    )
                    .clickable(enabled = text.isNotBlank() && !isLoading) { onSend() },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = AccentGold,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (text.isNotBlank()) Color.White else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}