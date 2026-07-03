package com.opencode.mobile.ui.screens.chat

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opencode.mobile.data.model.MessageEntity
import com.opencode.mobile.data.websocket.SessionInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(connectionId: Long) {
    val app = LocalContext.current.applicationContext as Application
    val viewModel: ChatViewModel = viewModel(key = "chat_$connectionId", factory = ChatViewModel.factory(app, connectionId))
    val messages by viewModel.messages.collectAsState()
    val status by viewModel.status.collectAsState()
    val connectionName by viewModel.connectionName.collectAsState()
    val error by viewModel.error.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val sessions by viewModel.sessions.collectAsState()
    val currentSessionTitle by viewModel.currentSessionTitle.collectAsState()
    var showSessionsDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf("") }

    LaunchedEffect(error) {
        error?.let {
            errorText = it
            showErrorDialog = true
            viewModel.clearError()
        }
    }

    if (showSessionsDialog) {
        SessionsDialog(
            sessions = sessions,
            currentSessionId = viewModel.currentSessionId.collectAsState().value,
            onSelect = { viewModel.selectSession(it); showSessionsDialog = false },
            onDelete = { viewModel.deleteSession(it) },
            onDismiss = { showSessionsDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(connectionName.ifEmpty { "Чат" })
                        if (currentSessionTitle.isNotEmpty()) {
                            Text(
                                currentSessionTitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        }
                        Text(
                            when (status) {
                                ConnectionStatus.CONNECTED -> "Подключено"
                                ConnectionStatus.CONNECTING -> "Подключение..."
                                ConnectionStatus.DISCONNECTED -> "Отключено"
                                ConnectionStatus.ERROR -> "Ошибка"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = when (status) {
                                ConnectionStatus.CONNECTED -> Color(0xFF4CAF50)
                                ConnectionStatus.CONNECTING -> MaterialTheme.colorScheme.tertiary
                                ConnectionStatus.ERROR -> MaterialTheme.colorScheme.error
                                ConnectionStatus.DISCONNECTED -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                },
                actions = {
                    if (status == ConnectionStatus.CONNECTED) {
                        IconButton(onClick = { viewModel.loadSessions(); showSessionsDialog = true }) {
                            Icon(Icons.Default.List, "Сессии")
                        }
                        IconButton(onClick = { viewModel.newSession() }) {
                            Icon(Icons.Default.Add, "Новая сессия")
                        }
                        IconButton(onClick = { viewModel.clearChat() }) {
                            Icon(Icons.Default.Clear, "Очистить чат")
                        }
                        IconButton(onClick = { viewModel.disconnect() }) {
                            Icon(Icons.Default.LinkOff, "Отключиться")
                        }
                    } else if (status == ConnectionStatus.ERROR || status == ConnectionStatus.DISCONNECTED) {
                        IconButton(onClick = { viewModel.reconnect() }) {
                            Icon(Icons.Default.Refresh, "Переподключиться")
                        }
                    }
                }
            )
        },
        snackbarHost = {}
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            if (status == ConnectionStatus.CONNECTING) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (messages.isEmpty() && !isLoading && status == ConnectionStatus.CONNECTED) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Подключено к серверу. Напишите сообщение.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    reverseLayout = true
                ) {
                    items(messages.reversed(), key = { it.id }) { message ->
                        MessageBubble(message)
                    }
                }
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Введите сообщение...") },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                singleLine = true,
                enabled = status == ConnectionStatus.CONNECTED,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { viewModel.sendPrompt(inputText); inputText = "" }),
                trailingIcon = {
                    IconButton(
                        onClick = { viewModel.sendPrompt(inputText); inputText = "" },
                        enabled = inputText.isNotBlank() && status == ConnectionStatus.CONNECTED
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, "Отправить")
                    }
                }
            )
        }
    }

    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("Ошибка") },
            text = {
                SelectionContainer {
                    Text(errorText)
                }
            },
            confirmButton = { TextButton(onClick = { showErrorDialog = false }) { Text("OK") } }
        )
    }
}

@Composable
fun SessionsDialog(
    sessions: List<SessionInfo>,
    currentSessionId: String?,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Сессии") },
        text = {
            if (sessions.isEmpty()) {
                Text("Нет сессий")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(sessions) { session ->
                        val isCurrent = session.id == currentSessionId
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(session.id) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        session.title.ifEmpty { session.id.take(16) + "..." },
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1
                                    )
                                    Text(
                                        session.id.take(16) + "...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onDelete(session.id) }) {
                                    Icon(Icons.Default.Delete, "Удалить", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } }
    )
}

@Composable
fun MessageBubble(message: MessageEntity) {
    val isUser = message.role == "user"
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    if (isUser) Icons.Default.Person else Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(if (isUser) "Вы" else "OpenCode", style = MaterialTheme.typography.labelMedium)
            }
            Spacer(modifier = Modifier.height(4.dp))
            SelectionContainer {
                Text(text = message.content, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}