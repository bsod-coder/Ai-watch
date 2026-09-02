package com.bsodcoder.aiwatch.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bsodcoder.aiwatch.data.ModelStore
import com.bsodcoder.aiwatch.data.WatchSync
import com.bsodcoder.aiwatch.shared.ModelEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SendState {
    data object Idle : SendState
    data object Sending : SendState
    data object Success : SendState
    data class Error(val message: String) : SendState
}

class SetupViewModel(app: Application) : AndroidViewModel(app) {

    private val store = ModelStore(app)

    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _models = MutableStateFlow<List<ModelEntry>>(emptyList())
    val models: StateFlow<List<ModelEntry>> = _models.asStateFlow()

    private val _sendState = MutableStateFlow<SendState>(SendState.Idle)
    val sendState: StateFlow<SendState> = _sendState.asStateFlow()

    init {
        viewModelScope.launch {
            store.apiKey.collect { _apiKey.value = it }
        }
        viewModelScope.launch {
            store.models.collect { _models.value = it }
        }
    }

    fun onApiKeyChanged(value: String) {
        _apiKey.value = value
        viewModelScope.launch { store.setApiKey(value) }
    }

    fun addModel(rawId: String) {
        val id = rawId.trim()
        if (id.isEmpty()) return
        if (_models.value.any { it.id == id }) return
        val updated = _models.value + ModelEntry(id = id)
        _models.value = updated
        viewModelScope.launch { store.setModels(updated) }
    }

    fun removeModel(id: String) {
        val updated = _models.value.filterNot { it.id == id }
        _models.value = updated
        viewModelScope.launch { store.setModels(updated) }
    }

    fun sendToWatch() {
        if (_apiKey.value.isBlank() || _models.value.isEmpty()) {
            _sendState.value = SendState.Error("Add an API key and at least one model first")
            return
        }
        viewModelScope.launch {
            _sendState.value = SendState.Sending
            runCatching {
                WatchSync.sendConfig(getApplication(), _apiKey.value, _models.value)
            }.onSuccess {
                _sendState.value = SendState.Success
            }.onFailure {
                _sendState.value = SendState.Error(it.message ?: "Failed to reach watch")
            }
        }
    }
}
