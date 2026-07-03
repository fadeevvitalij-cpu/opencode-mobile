package com.opencode.mobile.ui.screens.terminal

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(connectionId: Long) {
    val app = LocalContext.current.applicationContext as Application
    val viewModel: TerminalViewModel = viewModel(key = "term_$connectionId", factory = TerminalViewModel.factory(app, connectionId))
    val output by viewModel.output.collectAsState()
    val status by viewModel.status.collectAsState()
    val connectionName by viewModel.connectionName.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    LaunchedEffect(output) { scrollState.animateScrollTo(scrollState.maxValue) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(connectionName.ifEmpty { "Терминал" })
                    Text(
                        when (status) {
                            TerminalStatus.CONNECTED -> "Подключено"
                            TerminalStatus.CONNECTING -> "Подключение..."
                            TerminalStatus.DISCONNECTED -> "Отключено"
                            TerminalStatus.ERROR -> "Ошибка"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when (status) {
                            TerminalStatus.CONNECTED -> Color(0xFF4CAF50)
                            TerminalStatus.ERROR -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            },
            actions = {
                IconButton(onClick = { viewModel.sendCommand("clear") }) {
                    Icon(Icons.Default.Delete, "Очистить")
                }
                if (status == TerminalStatus.CONNECTED) {
                    IconButton(onClick = { viewModel.disconnect() }) {
                        Icon(Icons.Default.LinkOff, "Отключиться")
                    }
                }
            }
        )

        Surface(
            color = Color.Black,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(8.dp)
        ) {
            Text(
                text = output,
                modifier = Modifier.padding(12.dp).fillMaxSize().verticalScroll(scrollState),
                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = Color.Green)
            )
        }

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        error?.let {
            Snackbar(modifier = Modifier.fillMaxWidth()) {
                SelectionContainer { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = Color(0xFF1E1E1E),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.weight(1f)
            ) {
                BasicTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    singleLine = true,
                    enabled = status == TerminalStatus.CONNECTED,
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = Color.Green),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { viewModel.sendCommand(inputText); inputText = "" }),
                    decorationBox = { innerTextField ->
                        if (inputText.isEmpty()) {
                            Text("$ ", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = Color.Green.copy(alpha = 0.5f)))
                        }
                        innerTextField()
                    }
                )
            }
            Button(
                onClick = { viewModel.sendCommand(inputText); inputText = "" },
                enabled = status == TerminalStatus.CONNECTED && inputText.isNotBlank()
            ) {
                Text(">", fontFamily = FontFamily.Monospace)
            }
        }
    }
}