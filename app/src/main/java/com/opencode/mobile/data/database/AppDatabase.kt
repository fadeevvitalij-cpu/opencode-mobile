package com.opencode.mobile.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.opencode.mobile.data.database.Converters
import com.opencode.mobile.data.model.ConnectionEntity
import com.opencode.mobile.data.model.MessageEntity
import com.opencode.mobile.data.model.TerminalSessionEntity

@Database(
    entities = [
        ConnectionEntity::class,
        MessageEntity::class,
        TerminalSessionEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun connectionDao(): ConnectionDao
    abstract fun messageDao(): MessageDao
    abstract fun terminalSessionDao(): TerminalSessionDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "opencode-mobile.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}