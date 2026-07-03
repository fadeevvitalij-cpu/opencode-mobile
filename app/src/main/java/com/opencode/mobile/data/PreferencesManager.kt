package com.opencode.mobile.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.Preferences

private val Context.dataStore by preferencesDataStore(name = "app_prefs")

class PreferencesManager(private val context: Context) {
    private val KEY_LAST_CONNECTION_ID = stringPreferencesKey("last_connection_id")
    private val KEY_THEME = stringPreferencesKey("theme")
    private val KEY_FONT_SIZE = floatPreferencesKey("font_size")
    private val KEY_AUTO_RECONNECT = booleanPreferencesKey("auto_reconnect")
    private val KEY_SKIN = stringPreferencesKey("skin")

    val lastConnectionId: Flow<String?> = context.dataStore.data
        .map { it[KEY_LAST_CONNECTION_ID] }

    val theme: Flow<String> = context.dataStore.data
        .map { it[KEY_THEME] ?: "system" }

    val fontSize: Flow<Float> = context.dataStore.data
        .map { it[KEY_FONT_SIZE] ?: 14f }

    val autoReconnect: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_AUTO_RECONNECT] ?: true }

    val skin: Flow<String> = context.dataStore.data
        .map { it[KEY_SKIN] ?: "default" }

    suspend fun setLastConnectionId(id: String) {
        context.dataStore.edit { it[KEY_LAST_CONNECTION_ID] = id }
    }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { it[KEY_THEME] = theme }
    }

    suspend fun setFontSize(size: Float) {
        context.dataStore.edit { it[KEY_FONT_SIZE] = size }
    }

    suspend fun setAutoReconnect(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_RECONNECT] = enabled }
    }

    suspend fun setSkin(skin: String) {
        context.dataStore.edit { it[KEY_SKIN] = skin }
    }
}