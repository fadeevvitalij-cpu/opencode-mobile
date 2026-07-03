package com.opencode.mobile.ui.screens.files

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(connectionId: Long) {
    val app = LocalContext.current.applicationContext as Application
    val viewModel: FileBrowserViewModel = viewModel(key = "files_$connectionId", factory = FileBrowserViewModel.factory(app, connectionId))
    val files by viewModel.files.collectAsState()
    val currentPath by viewModel.currentPath.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val connectionName by viewModel.connectionName.collectAsState()
    val fileContent by viewModel.fileContent.collectAsState()
    val fileContentTitle by viewModel.fileContentTitle.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(connectionName) },
            actions = {
                if (currentPath != "/") {
                    IconButton(onClick = { viewModel.navigateUp() }) {
                        Icon(Icons.Default.ArrowUpward, "На уровень выше")
                    }
                }
            }
        )

        Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth().padding(8.dp).padding(horizontal = 8.dp)) {
            Text(currentPath, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(files) { file ->
                    FileItem(file) {
                        if (file.isDirectory) viewModel.loadDirectory(file.path)
                        else viewModel.readFile(file.path)
                    }
                }
            }
        }
    }

    if (fileContent != null) {
        AlertDialog(
            onDismissRequest = { viewModel.closeFile() },
            title = { Text(fileContentTitle) },
            text = {
                SelectionContainer {
                    Text(
                        text = fileContent ?: "",
                        modifier = Modifier.verticalScroll(rememberScrollState()).fillMaxWidth(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.closeFile() }) { Text("Закрыть") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileItem(file: FileNode, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (file.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                contentDescription = null,
                tint = if (file.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(file.name, modifier = Modifier.weight(1f))
            if (!file.isDirectory && file.size > 0) {
                Text(formatSize(file.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun formatSize(size: Long): String = when {
    size < 1024 -> "$size B"
    size < 1024 * 1024 -> "${size / 1024} KB"
    else -> "${size / 1024 / 1024} MB"
}