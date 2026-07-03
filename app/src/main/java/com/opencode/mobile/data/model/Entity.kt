package com.opencode.mobile.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.opencode.mobile.data.database.Converters
import java.time.Instant

@Entity(tableName = "connections")
@TypeConverters(Converters::class)
data class ConnectionEntity(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    val name: String,
    val host: String,
    val port: Int,
    val useSsl: Boolean = false,
    val authToken: String? = null,
    val model: String? = null,
    val rootPath: String = "",
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    var lastConnectedAt: Instant? = null
) {
    val url: String
        get() = "${if (useSsl) "wss" else "ws"}://$host:$port/ws"
}

@Entity(tableName = "messages")
@TypeConverters(Converters::class)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val connectionId: Long,
    val sessionId: String? = null,
    val content: String,
    val role: String, // "user" | "assistant" | "system"
    val timestamp: Instant = Instant.now(),
    val metadata: String? = null // JSON
)

@Entity(tableName = "terminal_sessions")
@TypeConverters(Converters::class)
data class TerminalSessionEntity(
    @PrimaryKey val id: String,
    val connectionId: Long,
    val name: String,
    val workingDir: String,
    val createdAt: Instant = Instant.now(),
    val lastActiveAt: Instant = Instant.now()
)