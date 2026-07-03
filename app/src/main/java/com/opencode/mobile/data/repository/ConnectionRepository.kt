package com.opencode.mobile.data.repository

import android.content.Context
import com.opencode.mobile.data.database.AppDatabase
import com.opencode.mobile.data.model.ConnectionEntity
import com.opencode.mobile.data.model.MessageEntity
import com.opencode.mobile.data.model.TerminalSessionEntity
import kotlinx.coroutines.flow.Flow

class ConnectionRepository(private val db: AppDatabase) {

    fun getAllConnections(): Flow<List<ConnectionEntity>> = db.connectionDao().getAll()

    suspend fun getConnection(id: Long): ConnectionEntity? = db.connectionDao().getById(id)

    suspend fun saveConnection(connection: ConnectionEntity): Long = db.connectionDao().insert(connection)

    suspend fun updateConnection(connection: ConnectionEntity): Int = db.connectionDao().update(connection)

    suspend fun deleteConnection(id: Long): Int = db.connectionDao().delete(id)

    suspend fun updateLastConnected(id: Long) =
        db.connectionDao().updateLastConnected(id, System.currentTimeMillis())

    fun getMessages(connectionId: Long): Flow<List<MessageEntity>> = db.messageDao().getByConnectionId(connectionId)

    suspend fun getMessagesSync(connectionId: Long): List<MessageEntity> = db.messageDao().getByConnectionIdSync(connectionId)

    fun getMessagesBySession(connectionId: Long, sessionId: String?): Flow<List<MessageEntity>> =
        if (sessionId != null) db.messageDao().getBySessionId(connectionId, sessionId)
        else db.messageDao().getByConnectionNoSession(connectionId)

    suspend fun saveMessages(messages: List<MessageEntity>) = db.messageDao().insertAll(messages)

    suspend fun saveMessage(message: MessageEntity) = db.messageDao().insert(message)

    suspend fun clearMessages(connectionId: Long) = db.messageDao().deleteByConnectionId(connectionId)

    suspend fun clearMessagesBySession(connectionId: Long, sessionId: String?) {
        if (sessionId != null) db.messageDao().deleteBySessionId(connectionId, sessionId)
        else db.messageDao().deleteByConnectionNoSession(connectionId)
    }

    suspend fun getRecentMessages(connectionId: Long, limit: Int): List<MessageEntity> =
        db.messageDao().getRecentByConnectionId(connectionId, limit)

    suspend fun updateNullSessionIdMessages(connectionId: Long, sessionId: String) =
        db.messageDao().updateNullSessionId(connectionId, sessionId)

    fun getSessions(connectionId: Long): Flow<List<TerminalSessionEntity>> = db.terminalSessionDao().getByConnectionId(connectionId)

    suspend fun saveSession(session: TerminalSessionEntity) = db.terminalSessionDao().insert(session)

    suspend fun updateSession(session: TerminalSessionEntity) = db.terminalSessionDao().update(session)

    suspend fun deleteSession(id: String) = db.terminalSessionDao().delete(id)

    companion object {
        private var instance: ConnectionRepository? = null
        fun create(context: Context): ConnectionRepository {
            if (instance == null) {
                instance = ConnectionRepository(AppDatabase.getInstance(context))
            }
            return instance!!
        }
    }
}