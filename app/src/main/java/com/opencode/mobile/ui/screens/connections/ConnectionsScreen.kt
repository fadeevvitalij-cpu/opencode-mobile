package com.opencode.mobile.ui.screens.connections

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opencode.mobile.data.model.ConnectionEntity
import com.opencode.mobile.ui.components.EmptyState

private val FREE_MODELS = listOf(
    "" to "Без модели (по умолчанию сервера)",
    "deepseek-v4-flash" to "DeepSeek V4 Flash",
    "deepseek-v4-pro" to "DeepSeek V4 Pro",
    "deepseek-chat" to "DeepSeek Chat (V3)",
    "deepseek-reasoner" to "DeepSeek Reasoner (R1)",
    "nvidia/nemotron-3-ultra-550b-a55b" to "NVIDIA Nemotron 3 Ultra 550B",
    "nvidia/nemotron-3-super-120b-a12b" to "NVIDIA Nemotron 3 Super 120B",
    "nvidia/nemotron-3-nano-30b-a3b" to "NVIDIA Nemotron 3 Nano 30B",
    "nvidia/llama-3.1-nemotron-70b-instruct" to "NVIDIA Llama Nemotron 70B",
    "nvidia/llama-3.3-nemotron-super-49b-v1.5" to "NVIDIA Nemotron Super 49B v1.5",
    "nvidia/mistral-nemotron" to "NVIDIA Mistral Nemotron",
    "meta/llama-4-maverick-17b-128e-instruct" to "Meta Llama 4 Maverick 17B"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionsScreen(onNavigateToChat: (Long) -> Unit) {
    val viewModel: ConnectionsViewModel = viewModel()
    val connections by viewModel.connections.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingConnection by remember { mutableStateOf<ConnectionEntity?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Подключения") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "Добавить")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading && connections.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (connections.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Link,
                    title = "Нет подключений",
                    subtitle = "Добавьте первое подключение к OpenCode серверу",
                    onAction = { showAddDialog = true },
                    actionLabel = "Добавить подключение"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(connections, key = { it.id }) { connection ->
                        ConnectionCard(
                            connection = connection,
                            onClick = { onNavigateToChat(connection.id) },
                            onEdit = { editingConnection = connection },
                            onDelete = { viewModel.deleteConnection(connection.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog || editingConnection != null) {
        AddConnectionDialog(
            connection = editingConnection,
            onDismiss = { showAddDialog = false; editingConnection = null },
            onSave = { name, host, port, useSsl, token, model, rootPath ->
                val conn = ConnectionEntity(
                    id = editingConnection?.id ?: 0,
                    name = name,
                    host = host,
                    port = port,
                    useSsl = useSsl,
                    authToken = token,
                    model = model,
                    rootPath = rootPath
                )
                if (editingConnection != null) {
                    viewModel.updateConnection(conn)
                } else {
                    viewModel.addConnection(conn)
                }
                showAddDialog = false
                editingConnection = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionCard(connection: ConnectionEntity, onClick: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(connection.name, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(connection.url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (connection.model != null) {
                    Text("Модель: ${connection.model}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Редактировать")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Удалить", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddConnectionDialog(
    connection: ConnectionEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, Int, Boolean, String?, String?, String) -> Unit
) {
    var name by remember { mutableStateOf(connection?.name ?: "") }
    var host by remember { mutableStateOf(connection?.host ?: "") }
    var portText by remember { mutableStateOf((connection?.port ?: 8080).toString()) }
    var useSsl by remember { mutableStateOf(connection?.useSsl ?: false) }
    var token by remember { mutableStateOf(connection?.authToken ?: "") }
    var model by remember { mutableStateOf(connection?.model ?: "") }
    var rootPath by remember { mutableStateOf(connection?.rootPath ?: "") }
    var hostTouched by remember { mutableStateOf(false) }
    val hostError = host.isBlank() && hostTouched

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (connection != null) "Редактировать" else "Новое подключение") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Название") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it; hostTouched = true },
                    label = { Text("Хост (IP/домен)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = hostError,
                    supportingText = if (hostError) {{ Text("Хост не может быть пустым") }} else null
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = portText,
                        onValueChange = { portText = it.filter { c -> c.isDigit() } },
                        label = { Text("Порт") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Text(if (useSsl) "wss://" else "ws://")
                    Switch(checked = useSsl, onCheckedChange = { useSsl = it })
                }
                OutlinedTextField(value = token, onValueChange = { token = it }, label = { Text("Токен (опционально)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                var modelExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = modelExpanded,
                    onExpandedChange = { modelExpanded = it }
                ) {
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it; modelExpanded = false },
                        label = { Text("Модель") },
                        placeholder = { Text("Выберите модель") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = modelExpanded,
                        onDismissRequest = { modelExpanded = false }
                    ) {
                        FREE_MODELS.forEach { (id, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = { model = id; modelExpanded = false }
                            )
                        }
                    }
                }
                OutlinedTextField(value = rootPath, onValueChange = { rootPath = it }, label = { Text("Корневая директория") }, placeholder = { Text("D:/Opencode") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val port = portText.toIntOrNull() ?: 8080
                    onSave(name.ifBlank { "Сервер" }, host, port, useSsl, token.ifBlank { null }, model.ifBlank { null }, rootPath)
                },
                enabled = host.isNotBlank()
            ) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}