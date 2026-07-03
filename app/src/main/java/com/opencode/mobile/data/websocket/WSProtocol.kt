package com.opencode.mobile.data.websocket

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName

sealed class WSMessage {
    data class Auth(
        val type: String = "auth",
        @SerializedName("token") val token: String
    ) : WSMessage()

    data class Prompt(
        val type: String = "prompt",
        @SerializedName("prompt") val prompt: String,
        @SerializedName("session_id") val sessionId: String? = null,
        @SerializedName("model") val model: String? = null
    ) : WSMessage()

    data class TerminalCommand(
        val type: String = "terminal_command",
        @SerializedName("command") val command: String,
        @SerializedName("session_id") val sessionId: String,
        @SerializedName("cols") val cols: Int = 80,
        @SerializedName("rows") val rows: Int = 24
    ) : WSMessage()

    data class TerminalResize(
        val type: String = "terminal_resize",
        @SerializedName("session_id") val sessionId: String,
        @SerializedName("cols") val cols: Int,
        @SerializedName("rows") val rows: Int
    ) : WSMessage()

    data class ListFiles(
        val type: String = "list_files",
        @SerializedName("path") val path: String = "/"
    ) : WSMessage()

    data class ReadFile(
        val type: String = "read_file",
        @SerializedName("path") val path: String
    ) : WSMessage()

    data class WriteFile(
        val type: String = "write_file",
        @SerializedName("path") val path: String,
        @SerializedName("content") val content: String
    ) : WSMessage()

    data class Ping(
        val type: String = "ping",
        @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis()
    ) : WSMessage()

    data class SessionList(
        val type: String = "session_list"
    ) : WSMessage()

    data class SessionDelete(
        val type: String = "session_delete",
        @SerializedName("session_id") val sessionId: String
    ) : WSMessage()

    data class SessionNew(
        val type: String = "session_new"
    ) : WSMessage()
}

data class WSResponse(
    val type: String? = null,
    val content: String? = null,
    @SerializedName("success") val authSuccess: Boolean? = null,
    @SerializedName("session_id") val sessionId: String? = null,
    @SerializedName("tool_name") val toolName: String? = null,
    @SerializedName("tool_args") val toolArgs: String? = null,
    @SerializedName("error") val error: String? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("data") val terminalData: String? = null,
    @SerializedName("exit_code") val exitCode: Int? = null,
    @SerializedName("token") val token: String? = null,
    @SerializedName("timestamp") val timestamp: Long? = null,
    @SerializedName("files") val files: List<FileItem>? = null,
    @SerializedName("result") val result: String? = null,
    @SerializedName("path") val path: String? = null,
    @SerializedName("sessions") val sessions: List<SessionInfo>? = null
)

data class SessionInfo(
    val id: String,
    val title: String,
    val updated: String
)

data class FileItem(
    @SerializedName("name") val name: String,
    @SerializedName("path") val path: String,
    @SerializedName("isDirectory") val isDirectory: Boolean,
    @SerializedName("size") val size: Long = 0
)

object Protocol {
    private val gson = GsonBuilder().serializeNulls().create()

    fun toJson(message: WSMessage): String = gson.toJson(message)

    fun fromJson(json: String): WSResponse =
        gson.fromJson(json, WSResponse::class.java)
}