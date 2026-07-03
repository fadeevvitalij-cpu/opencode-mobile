package com.opencode.mobile.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.opencode.mobile.data.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = PreferencesManager(application)

    private val _theme = MutableStateFlow("system")
    val theme = _theme.asStateFlow()

    private val _fontSize = MutableStateFlow(14f)
    val fontSize = _fontSize.asStateFlow()

    private val _autoReconnect = MutableStateFlow(true)
    val autoReconnect = _autoReconnect.asStateFlow()

    private val _skin = MutableStateFlow("default")
    val skin = _skin.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.theme.collect { _theme.value = it }
        }
        viewModelScope.launch {
            prefs.fontSize.collect { _fontSize.value = it }
        }
        viewModelScope.launch {
            prefs.autoReconnect.collect { _autoReconnect.value = it }
        }
        viewModelScope.launch {
            prefs.skin.collect { _skin.value = it }
        }
    }

    fun setTheme(value: String) {
        _theme.value = value
        viewModelScope.launch { prefs.setTheme(value) }
    }

    fun setFontSize(value: Float) {
        _fontSize.value = value
        viewModelScope.launch { prefs.setFontSize(value) }
    }

    fun setAutoReconnect(value: Boolean) {
        _autoReconnect.value = value
        viewModelScope.launch { prefs.setAutoReconnect(value) }
    }

    fun setSkin(value: String) {
        _skin.value = value
        viewModelScope.launch { prefs.setSkin(value) }
    }
}