package com.opencode.mobile.ui.screens.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.opencode.mobile.data.model.ConnectionEntity
import com.opencode.mobile.data.model.MessageEntity
import com.opencode.mobile.data.repository.ConnectionRepository
import com.opencode.mobile.data.websocket.OpencodeWebSocketClient
import com.opencode.mobile.data.websocket.SessionInfo
import com.opencode.mobile.data.websocket.WSResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ConnectionStatus { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

class ChatViewModel(application: Application, private val connectionId: Long) : AndroidViewModel(application) {
    private val repository = ConnectionRepository.create(application)

    private val _messages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _status = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val status = _status.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId = _currentSessionId.asStateFlow()

    private val _currentSessionTitle = MutableStateFlow("")
    val currentSessionTitle = _currentSessionTitle.asStateFlow()

    private val _connectionName = MutableStateFlow("")
    val connectionName = _connectionName.asStateFlow()

    private val _sessions = MutableStateFlow<List<SessionInfo>>(emptyList())
    val sessions = _sessions.asStateFlow()

    private var wsClient: OpencodeWebSocketClient? = null
    private var selectedModel: String? = null

    private var messagesJob: Job? = null

    init {
        loadMessages()
        connectToServer()
    }

    private fun loadMessages() {
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            _isLoading.value = true
            refreshMessages()
            repository.getMessages(connectionId).collect { allMsgs ->
                applyFilter(allMsgs)
            }
        }
    }

    private suspend fun refreshMessages() {
        val allMsgs = repository.getMessagesSync(connectionId)
        applyFilter(allMsgs)
    }

    private fun updateSessionTitle() {
        val sid = _currentSessionId.value
        _currentSessionTitle.value = if (sid != null) {
            _sessions.value.find { it.id == sid }?.title ?: sid.take(16) + "..."
        } else {
            ""
        }
    }

    private fun applyFilter(allMsgs: List<MessageEntity>) {
        val sessionId = _currentSessionId.value
        _messages.value = if (sessionId != null) {
            allMsgs.filter { it.sessionId == sessionId || it.sessionId == null }
        } else {
            allMsgs
        }
        _isLoading.value = false
    }

    private fun connectToServer() {
        viewModelScope.launch {
            _status.value = ConnectionStatus.CONNECTING
            val connection = repository.getConnection(connectionId)
            if (connection == null) {
                _error.value = "Подключение не найдено"
                _status.value = ConnectionStatus.ERROR
                return@launch
            }
            _connectionName.value = connection.name
            selectedModel = connection.model
            repository.updateLastConnected(connectionId)

            val url = connection.url
            if (connection.host.isBlank()) {
                _error.value = "Не указан хост сервера"
                _status.value = ConnectionStatus.ERROR
                return@launch
            }

            wsClient = OpencodeWebSocketClient(
                url = url,
                authToken = connection.authToken,
                onMessage = { handleResponse(it) },
                onConnectionChange = { connected ->
                    _status.value = if (connected) ConnectionStatus.CONNECTED else ConnectionStatus.DISCONNECTED
                },
                onError = { err ->
                    _error.value = err.message ?: "Ошибка подключения"
                    _status.value = ConnectionStatus.ERROR
                }
            )
            wsClient?.connect()
        }
    }

    private fun handleResponse(response: WSResponse) {
        viewModelScope.launch {
            when (response.type) {
                "text", "tool" -> {
                    response.content?.let { content ->
                        val msg = MessageEntity(
                            connectionId = connectionId,
                            sessionId = response.sessionId ?: _currentSessionId.value,
                            content = content,
                            role = "assistant",
                            metadata = response.toolName?.let { "tool:$it" }
                        )
                        repository.saveMessage(msg)
                        refreshMessages()
                    }
                }
                "done" -> {
                    response.sessionId?.let {
                        repository.updateNullSessionIdMessages(connectionId, it)
                        _currentSessionId.value = it
                        updateSessionTitle()
                        refreshMessages()
                    }
                }
                "error" -> {
                    _error.value = response.error ?: "Ошибка сервера"
                }
                "session_list" -> {
                    response.sessions?.let { _sessions.value = it; updateSessionTitle() }
                }
                "session_deleted" -> {
                    loadSessions()
                }
                "session_new", "connected", "auth" -> {
                    // игнорируем — эти типы не требуют обработки на UI
                }
                else -> {
                    response.sessionId?.let {
                        _currentSessionId.value = it
                        updateSessionTitle()
                        repository.updateNullSessionIdMessages(connectionId, it)
                        refreshMessages()
                    }
                    response.content?.let { content ->
                        val msg = MessageEntity(
                            connectionId = connectionId,
                            sessionId = response.sessionId ?: _currentSessionId.value,
                            content = content,
                            role = "assistant",
                            metadata = response.type?.let { "type:$it" }
                        )
                        repository.saveMessage(msg)
                        refreshMessages()
                    }
                }
            }
        }
    }

    fun sendPrompt(prompt: String) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            val sessionId = _currentSessionId.value
            val userMsg = MessageEntity(connectionId = connectionId, sessionId = sessionId, content = prompt, role = "user")
            repository.saveMessage(userMsg)
            refreshMessages()
            wsClient?.sendPrompt(prompt, sessionId, selectedModel)
        }
    }

    fun reconnect() {
        disconnect()
        connectToServer()
    }

    fun disconnect() {
        wsClient?.disconnect()
        wsClient = null
        _status.value = ConnectionStatus.DISCONNECTED
    }

    fun loadSessions() {
        wsClient?.sendListSessions()
    }

    fun newSession() {
        wsClient?.sendNewSession()
        _currentSessionId.value = null
        _currentSessionTitle.value = "Новая сессия..."
        loadMessages()
        loadSessions()
    }

    fun selectSession(sessionId: String) {
        _currentSessionId.value = sessionId
        updateSessionTitle()
        loadMessages()
    }

    fun deleteSession(sessionId: String) {
        wsClient?.sendDeleteSession(sessionId)
        if (_currentSessionId.value == sessionId) {
            _currentSessionId.value = null
            _currentSessionTitle.value = ""
            loadMessages()
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearMessages(connectionId)
            refreshMessages()
        }
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
                    ChatViewModel(application, connectionId) as T
            }
    }
}