package com.bsodcoder.aiwatch.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bsodcoder.aiwatch.data.ModelStore
import com.bsodcoder.aiwatch.data.WatchConnection
import com.bsodcoder.aiwatch.data.WatchLinkState
import com.bsodcoder.aiwatch.data.WatchSync
import com.bsodcoder.aiwatch.shared.ModelEntry
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DeliveryStatus(val label: String) {
    Empty("0/2 fields filled"),
    Partial("1/2 fields filled"),
    Ready("2/2 Ready!"),
    Sending("Sending!"),
    Success("Success!"),
    Failed("Failed!"),
    NotConnected("Not connected")
}

data class SetupUiState(
    val apiKey: String = "",
    val models: List<ModelEntry> = emptyList(),
    val watchConnected: Boolean = false,
    val watchNames: List<String> = emptyList(),
    val delivery: DeliveryStatus = DeliveryStatus.Empty,
    val errorMessage: String? = null
) {
    val canSend: Boolean
        get() = delivery == DeliveryStatus.Ready ||
            delivery == DeliveryStatus.Failed ||
            delivery == DeliveryStatus.Success
}

private sealed interface SendState {
    data object Idle : SendState
    data object Sending : SendState
    data object Success : SendState
    data class Error(val message: String) : SendState
}

class SetupViewModel(app: Application) : AndroidViewModel(app) {

    private val store = ModelStore(app)

    private val _apiKey = MutableStateFlow("")
    private val _models = MutableStateFlow<List<ModelEntry>>(emptyList())
    private val _sendState = MutableStateFlow<SendState>(SendState.Idle)
    private val _watchLink = MutableStateFlow(WatchLinkState())

    val uiState: StateFlow<SetupUiState> = combine(
        _apiKey,
        _models,
        _sendState,
        _watchLink
    ) { key, models, send, link ->
        val filled = listOf(key.isNotBlank(), models.isNotEmpty()).count { it }
        val delivery = when (send) {
            SendState.Sending -> DeliveryStatus.Sending
            SendState.Success -> DeliveryStatus.Success
            is SendState.Error -> DeliveryStatus.Failed
            SendState.Idle -> when {
                filled == 0 -> DeliveryStatus.Empty
                filled == 1 -> DeliveryStatus.Partial
                !link.connected -> DeliveryStatus.NotConnected
                else -> DeliveryStatus.Ready
            }
        }
        SetupUiState(
            apiKey = key,
            models = models,
            watchConnected = link.connected,
            watchNames = link.names,
            delivery = delivery,
            errorMessage = (send as? SendState.Error)?.message
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SetupUiState())

    init {
        viewModelScope.launch {
            store.apiKey.collect { _apiKey.value = it }
        }
        viewModelScope.launch {
            store.models.collect { _models.value = it }
        }
        viewModelScope.launch {
            WatchConnection.observe(getApplication()).collect { _watchLink.value = it }
        }
    }

    fun onApiKeyChanged(value: String) {
        _apiKey.value = value
        clearTransientSend()
        viewModelScope.launch { store.setApiKey(value) }
    }

    fun addModel(rawId: String) {
        val id = rawId.trim()
        if (id.isEmpty()) return
        if (_models.value.any { it.id == id }) return
        val updated = _models.value + ModelEntry(id = id)
        _models.value = updated
        clearTransientSend()
        viewModelScope.launch { store.setModels(updated) }
    }

    fun removeModel(id: String) {
        val updated = _models.value.filterNot { it.id == id }
        _models.value = updated
        clearTransientSend()
        viewModelScope.launch { store.setModels(updated) }
    }

    fun sendToWatch() {
        if (_apiKey.value.isBlank() || _models.value.isEmpty()) {
            _sendState.value = SendState.Error("Add an API key and at least one model first")
            return
        }
        viewModelScope.launch {
            val link = WatchConnection.current(getApplication())
            _watchLink.value = link
            if (!link.connected) {
                _sendState.value = SendState.Error("Watch is not connected")
                return@launch
            }
            _sendState.value = SendState.Sending
            runCatching {
                WatchSync.sendConfig(getApplication(), _apiKey.value, _models.value)
            }.onSuccess {
                _sendState.value = SendState.Success
                delay(3_500)
                if (_sendState.value is SendState.Success) {
                    _sendState.value = SendState.Idle
                }
            }.onFailure {
                _sendState.value = SendState.Error(it.message ?: "Failed to reach watch")
            }
        }
    }

    private fun clearTransientSend() {
        val current = _sendState.value
        if (current is SendState.Success || current is SendState.Error) {
            _sendState.value = SendState.Idle
        }
    }
}
