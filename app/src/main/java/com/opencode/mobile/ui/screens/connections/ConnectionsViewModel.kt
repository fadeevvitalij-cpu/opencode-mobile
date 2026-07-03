package com.opencode.mobile.ui.screens.connections

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.opencode.mobile.data.model.ConnectionEntity
import com.opencode.mobile.data.repository.ConnectionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConnectionsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ConnectionRepository.create(application)

    private val _connections = MutableStateFlow<List<ConnectionEntity>>(emptyList())
    val connections = _connections.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getAllConnections().collect { list ->
                _connections.value = list
                _isLoading.value = false
            }
        }
    }

    fun addConnection(connection: ConnectionEntity) {
        viewModelScope.launch {
            repository.saveConnection(connection)
        }
    }

    fun updateConnection(connection: ConnectionEntity) {
        viewModelScope.launch {
            repository.updateConnection(connection)
        }
    }

    fun deleteConnection(id: Long) {
        viewModelScope.launch {
            repository.deleteConnection(id)
        }
    }
}