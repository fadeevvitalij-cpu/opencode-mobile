package com.opencode.mobile.data.websocket

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import okhttp3.*
import okio.ByteString

class OpencodeWebSocketClient(
    private val url: String,
    private val authToken: String?,
    private val onMessage: (WSResponse) -> Unit,
    private val onConnectionChange: (Boolean) -> Unit,
    private val onError: (Throwable) -> Unit
) {
    private val client = OkHttpClient.Builder()
        .pingInterval(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val messageQueue = Channel<String>(Channel.UNLIMITED)
    private var isConnected = false
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5

    fun connect() {
        scope.launch { attemptConnection() }
    }

    private fun attemptConnection() {
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $authToken")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                scope.launch {
                    isConnected = true
                    reconnectAttempts = 0
                    onConnectionChange(true)
                    sendAuth()
                    processQueue()
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                scope.launch {
                    try {
                        val response = Protocol.fromJson(text)
                        onMessage(response)
                    } catch (e: Exception) {
                        Log.e("WSClient", "Parse error: ${e.message}", e)
                        onMessage(WSResponse(type = "terminal_output", content = text))
                    }
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                scope.launch {
                    try {
                        val text = bytes.utf8()
                        val response = Protocol.fromJson(text)
                        onMessage(response)
                    } catch (e: Exception) {
                        Log.e("WSClient", "Parse error (binary): ${e.message}", e)
                        onMessage(WSResponse(type = "terminal_output", content = bytes.utf8()))
                    }
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                scope.launch {
                    isConnected = false
                    onConnectionChange(false)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                scope.launch {
                    isConnected = false
                    onConnectionChange(false)
                    if (reconnectAttempts < maxReconnectAttempts) scheduleReconnect()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                scope.launch {
                    isConnected = false
                    onConnectionChange(false)
                    onError(t)
                    if (reconnectAttempts < maxReconnectAttempts) scheduleReconnect()
                }
            }
        })
    }

    private fun sendAuth() {
        val auth = WSMessage.Auth(token = authToken ?: "")
        send(Protocol.toJson(auth))
    }

    fun sendPrompt(prompt: String, sessionId: String? = null, model: String? = null) {
        send(Protocol.toJson(WSMessage.Prompt(prompt = prompt, sessionId = sessionId, model = model)))
    }

    fun sendTerminalCommand(command: String, sessionId: String, cols: Int, rows: Int) {
        val msg = WSMessage.TerminalCommand(command = command, sessionId = sessionId, cols = cols, rows = rows)
        send(Protocol.toJson(msg))
    }

    fun sendTerminalResize(sessionId: String, cols: Int, rows: Int) {
        val msg = WSMessage.TerminalResize(sessionId = sessionId, cols = cols, rows = rows)
        send(Protocol.toJson(msg))
    }

    fun sendPing() {
        val msg = WSMessage.Ping()
        send(Protocol.toJson(msg))
    }

    fun sendListSessions() {
        val msg = WSMessage.SessionList()
        send(Protocol.toJson(msg))
    }

    fun sendDeleteSession(sessionId: String) {
        val msg = WSMessage.SessionDelete(sessionId = sessionId)
        send(Protocol.toJson(msg))
    }

    fun sendNewSession() {
        val msg = WSMessage.SessionNew()
        send(Protocol.toJson(msg))
    }

    fun sendListFiles(path: String) {
        val msg = WSMessage.ListFiles(path = path)
        send(Protocol.toJson(msg))
    }

    fun sendReadFile(path: String) {
        val msg = WSMessage.ReadFile(path = path)
        send(Protocol.toJson(msg))
    }

    fun sendWriteFile(path: String, content: String) {
        val msg = WSMessage.WriteFile(path = path, content = content)
        send(Protocol.toJson(msg))
    }

    private fun send(json: String) {
        if (isConnected) {
            webSocket?.send(json)
        } else {
            scope.launch { messageQueue.send(json) }
        }
    }

    private fun processQueue() {
        scope.launch {
            for (msg in messageQueue) {
                webSocket?.send(msg)
            }
        }
    }

    private fun scheduleReconnect() {
        scope.launch {
            reconnectAttempts++
            val delayMs = minOf(1000L * (2L shl (reconnectAttempts - 1)), 30000L)
            delay(delayMs)
            if (!isConnected) attemptConnection()
        }
    }

    fun disconnect() {
        scope.cancel()
        webSocket?.close(1000, "Client disconnect")
        client.connectionPool.evictAll()
    }
}