package com.opencode.mobile.ui.screens.terminal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.opencode.mobile.data.repository.ConnectionRepository
import com.opencode.mobile.data.websocket.OpencodeWebSocketClient
import com.opencode.mobile.data.websocket.WSMessage
import com.opencode.mobile.data.websocket.WSResponse
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class TerminalStatus { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

class TerminalViewModel(application: Application, private val connectionId: Long) : AndroidViewModel(application) {
    private val repository = ConnectionRepository.create(application)

    private val _output = MutableStateFlow("")
    val output = _output.asStateFlow()

    private val _status = MutableStateFlow(TerminalStatus.DISCONNECTED)
    val status = _status.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _connectionName = MutableStateFlow("")
    val connectionName = _connectionName.asStateFlow()

    private var wsClient: OpencodeWebSocketClient? = null
    private val commandQueue = Channel<WSMessage>(Channel.UNLIMITED)
    private var cols = 80
    private var rows = 24
    private var rootPath: String = ""
    private var sessionId: String = "term-main"

    init {
        connect()
    }

    fun connect() {
        viewModelScope.launch {
            _status.value = TerminalStatus.CONNECTING
            _isLoading.value = true

            val connection = repository.getConnection(connectionId)
            if (connection == null) {
                _error.value = "Подключение не найдено"
                _status.value = TerminalStatus.ERROR
                _isLoading.value = false
                return@launch
            }
            _connectionName.value = connection.name
            rootPath = connection.rootPath
            sessionId = "term-${System.currentTimeMillis()}"

            _output.value = "Подключение к ${connection.host}:${connection.port}...\n"

            wsClient = OpencodeWebSocketClient(
                url = connection.url,
                authToken = connection.authToken,
                onMessage = { response ->
                    when (response.type) {
                        "terminal_output" -> {
                            val text = response.content?.ifBlank { response.terminalData } ?: response.terminalData ?: ""
                            _output.value += text
                            response.exitCode?.let { _output.value += "\n[Процесс завершён с кодом $it]\n$ " }
                        }
                        "error" -> {
                            _error.value = response.error ?: "Ошибка терминала"
                        }
                    }
                },
                onConnectionChange = { connected ->
                    _status.value = if (connected) TerminalStatus.CONNECTED else TerminalStatus.DISCONNECTED
                    if (connected) {
                        _output.value += "Подключено к терминалу\n\n$ "
                        _isLoading.value = false
                        processQueue()
                    }
                },
                onError = { err ->
                    _error.value = err.message ?: "Ошибка подключения"
                    _status.value = TerminalStatus.ERROR
                    _isLoading.value = false
                }
            )
            wsClient?.connect()
        }
    }

    fun sendCommand(command: String) {
        if (command.trim().lowercase() == "clear") {
            _output.value = ""
            return
        }
        _output.value += "$ $command\n"
        commandQueue.trySend(WSMessage.TerminalCommand(command = command, sessionId = sessionId, cols = cols, rows = rows))
    }

    fun resize(newCols: Int, newRows: Int) {
        cols = newCols
        rows = newRows
        commandQueue.trySend(WSMessage.TerminalResize(sessionId = sessionId, cols = cols, rows = rows))
    }

    private fun processQueue() {
        viewModelScope.launch {
            for (msg in commandQueue) {
                when (msg) {
                    is WSMessage.TerminalCommand -> wsClient?.sendTerminalCommand(msg.command, msg.sessionId, msg.cols, msg.rows)
                    is WSMessage.TerminalResize -> wsClient?.sendTerminalResize(msg.sessionId, msg.cols, msg.rows)
                    else -> {}
                }
            }
        }
    }

    fun disconnect() {
        wsClient?.disconnect()
        wsClient = null
        _status.value = TerminalStatus.DISCONNECTED
    }

    fun clearError() { _error.value = null }

    override fun onCleared() {
        disconnect()
        super.onCleared()
    }

    companion object {
        fun factory(application: Application, connectionId: Long): ViewModelProvider.Factory =
            object : ViewModelProvider.AndroidViewModelFactory(application) {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    TerminalViewModel(application, connectionId) as T
            }
    }
}