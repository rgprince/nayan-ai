package com.rgprince.nayanai.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.rgprince.nayanai.data.ChatRepository
import com.rgprince.nayanai.data.Message
import com.rgprince.nayanai.model.OnnxInference
import com.rgprince.nayanai.ui.theme.AIBubbleDark
import com.rgprince.nayanai.ui.theme.AIBubbleLight
import com.rgprince.nayanai.ui.theme.UserBubbleDark
import com.rgprince.nayanai.ui.theme.UserBubbleLight
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    sessionId: Long,
    modelPath: String?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { ChatRepository(context) }
    val inference = remember { OnnxInference(context) }
    
    val messages by repository.getMessagesForSession(sessionId).collectAsState(initial = emptyList())
    var inputText by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var modelLoaded by remember { mutableStateOf(false) }
    
    val listState = rememberLazyListState()
    
    // Load model on first composition
    LaunchedEffect(modelPath) {
        modelPath?.let {
            modelLoaded = inference.loadModel(it)
        }
    }
    
    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nayan AI") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Settings */ }) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(message = message)
                }
                
                // Typing indicator
                if (isGenerating) {
                    item {
                        TypingIndicator()
                    }
                }
            }
            
            // Input Field
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 3.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Message Nayan...") },
                        enabled = !isGenerating && modelLoaded,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputText.isNotBlank()) {
                                    val userMessage = inputText
                                    inputText = ""
                                    
                                    scope.launch {
                                        isGenerating = true
                                        
                                        // Save user message
                                        repository.addMessage(sessionId, userMessage, isUser = true)
                                        
                                        // Generate AI response
                                        val response = inference.generate(userMessage)
                                        
                                        // Save AI response
                                        repository.addMessage(sessionId, response, isUser = false)
                                        
                                        isGenerating = false
                                    }
                                }
                            }
                        ),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    FilledIconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                val userMessage = inputText
                                inputText = ""
                                
                                scope.launch {
                                    isGenerating = true
                                    repository.addMessage(sessionId, userMessage, isUser = true)
                                    val response = inference.generate(userMessage)
                                    repository.addMessage(sessionId, response, isUser = false)
                                    isGenerating = false
                                }
                            }
                        },
                        enabled = inputText.isNotBlank() && !isGenerating && modelLoaded,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Send, "Send")
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = if (message.isUser) 20.dp else 4.dp,
                        bottomEnd = if (message.isUser) 4.dp else 20.dp
                    )
                )
                .background(
                    color = if (message.isUser) {
                        UserBubbleLight
                    } else {
                        AIBubbleLight
                    }
                )
                .padding(16.dp)
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyLarge,
                color = if (message.isUser) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
fun TypingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(AIBubbleLight)
                .padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    TypingDot(delay = index * 150)
                }
            }
        }
    }
}

@Composable
fun TypingDot(delay: Int) {
    var scale by remember { mutableStateOf(1f) }
    
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(delay.toLong())
            scale = 1.3f
            kotlinx.coroutines.delay(300)
            scale = 1f
            kotlinx.coroutines.delay(300)
        }
    }
    
    Box(
        modifier = Modifier
            .size(8.dp)
            .background(
                MaterialTheme.colorScheme.onSurfaceVariant,
                shape = androidx.compose.foundation.shape.CircleShape
            )
    )
}
