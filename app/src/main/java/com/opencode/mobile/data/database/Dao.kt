package com.opencode.mobile.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.opencode.mobile.data.model.ConnectionEntity
import com.opencode.mobile.data.model.MessageEntity
import com.opencode.mobile.data.model.TerminalSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConnectionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(connection: ConnectionEntity): Long

    @Update
    suspend fun update(connection: ConnectionEntity): Int

    @Query("DELETE FROM connections WHERE id = :id")
    suspend fun delete(id: Long): Int

    @Query("SELECT * FROM connections ORDER BY updatedAt DESC")
    fun getAll(): Flow<List<ConnectionEntity>>

    @Query("SELECT * FROM connections WHERE id = :id")
    suspend fun getById(id: Long): ConnectionEntity?

    @Query("UPDATE connections SET lastConnectedAt = :now WHERE id = :id")
    suspend fun updateLastConnected(id: Long, now: Long)
}

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<MessageEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity): Long

    @Query("SELECT * FROM messages WHERE connectionId = :connectionId ORDER BY timestamp ASC")
    fun getByConnectionId(connectionId: Long): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE connectionId = :connectionId ORDER BY timestamp ASC")
    suspend fun getByConnectionIdSync(connectionId: Long): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE connectionId = :connectionId AND (sessionId = :sessionId OR sessionId IS NULL) ORDER BY timestamp ASC")
    fun getBySessionId(connectionId: Long, sessionId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE connectionId = :connectionId AND sessionId IS NULL ORDER BY timestamp ASC")
    fun getByConnectionNoSession(connectionId: Long): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE connectionId = :connectionId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentByConnectionId(connectionId: Long, limit: Int): List<MessageEntity>

    @Query("DELETE FROM messages WHERE connectionId = :connectionId")
    suspend fun deleteByConnectionId(connectionId: Long): Int

    @Query("DELETE FROM messages WHERE connectionId = :connectionId AND (sessionId = :sessionId OR sessionId IS NULL)")
    suspend fun deleteBySessionId(connectionId: Long, sessionId: String): Int

    @Query("DELETE FROM messages WHERE connectionId = :connectionId AND sessionId IS NULL")
    suspend fun deleteByConnectionNoSession(connectionId: Long): Int

    @Query("UPDATE messages SET sessionId = :sessionId WHERE connectionId = :connectionId AND sessionId IS NULL")
    suspend fun updateNullSessionId(connectionId: Long, sessionId: String): Int
}

@Dao
interface TerminalSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: TerminalSessionEntity): Long

    @Update
    suspend fun update(session: TerminalSessionEntity): Int

    @Query("DELETE FROM terminal_sessions WHERE id = :id")
    suspend fun delete(id: String): Int

    @Query("SELECT * FROM terminal_sessions WHERE connectionId = :connectionId ORDER BY lastActiveAt DESC")
    fun getByConnectionId(connectionId: Long): Flow<List<TerminalSessionEntity>>

    @Query("SELECT * FROM terminal_sessions WHERE id = :id")
    suspend fun getById(id: String): TerminalSessionEntity?
}