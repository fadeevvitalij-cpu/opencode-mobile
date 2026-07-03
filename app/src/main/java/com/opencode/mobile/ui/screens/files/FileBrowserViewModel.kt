package com.opencode.mobile.ui.screens.files

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.opencode.mobile.data.repository.ConnectionRepository
import com.opencode.mobile.data.websocket.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FileNode(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long = 0
)

class FileBrowserViewModel(application: Application, private val connectionId: Long) : AndroidViewModel(application) {
    private val repository = ConnectionRepository.create(application)

    private val _files = MutableStateFlow<List<FileNode>>(emptyList())
    val files = _files.asStateFlow()

    private var rootPath: String = ""

    private val _currentPath = MutableStateFlow("/")
    val currentPath = _currentPath.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _connectionName = MutableStateFlow("")
    val connectionName = _connectionName.asStateFlow()

    private val _fileContent = MutableStateFlow<String?>(null)
    val fileContent = _fileContent.asStateFlow()

    private val _fileContentTitle = MutableStateFlow("")
    val fileContentTitle = _fileContentTitle.asStateFlow()

    private var wsClient: OpencodeWebSocketClient? = null

    init {
        viewModelScope.launch {
            val conn = repository.getConnection(connectionId)
            _connectionName.value = conn?.name ?: "Файлы"
            rootPath = conn?.rootPath?.ifBlank { "/" } ?: "/"
            _currentPath.value = rootPath
            if (conn != null) {
                connectAndLoad(conn)
            }
        }
    }

    private fun connectAndLoad(conn: com.opencode.mobile.data.model.ConnectionEntity) {
        wsClient = OpencodeWebSocketClient(
            url = conn.url,
            authToken = conn.authToken,
            onMessage = { response ->
                if (response.type == "file_list") {
                    _files.value = response.files?.map {
                        FileNode(it.name, it.path, it.isDirectory, it.size)
                    } ?: emptyList()
                    _isLoading.value = false
                } else {
                    val text = response.content ?: response.result ?: response.terminalData
                    if (text != null) {
                        _fileContent.value = text
                        _isLoading.value = false
                    }
                }
            },
            onConnectionChange = { connected ->
                if (connected) loadDirectory(rootPath)
            },
            onError = {}
        )
        wsClient?.connect()
    }

    fun loadDirectory(path: String) {
        _isLoading.value = true
        _currentPath.value = path
        wsClient?.sendListFiles(path)
    }

    fun readFile(path: String) {
        _isLoading.value = true
        _fileContentTitle.value = path.substringAfterLast("/")
        _fileContent.value = null
        wsClient?.sendReadFile(path)
    }

    fun closeFile() {
        _fileContent.value = null
        _fileContentTitle.value = ""
    }

    fun navigateUp() {
        val path = _currentPath.value
        if (path != rootPath) {
            val parent = path.substringBeforeLast("/").ifEmpty { "/" }
            loadDirectory(parent)
        }
    }

    override fun onCleared() {
        wsClient?.disconnect()
        super.onCleared()
    }

    companion object {
        fun factory(application: Application, connectionId: Long): ViewModelProvider.Factory =
            object : ViewModelProvider.AndroidViewModelFactory(application) {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    FileBrowserViewModel(application, connectionId) as T
            }
    }
}