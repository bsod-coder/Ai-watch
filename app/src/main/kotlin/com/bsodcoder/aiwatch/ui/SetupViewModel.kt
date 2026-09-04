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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

sealed interface SendState {
    data object Idle : SendState
    data object Sending : SendState
    data object Success : SendState
    data class Error(val message: String) : SendState
}

fun deliveryStatus(
    apiKey: String,
    models: List<ModelEntry>,
    send: SendState,
    watchConnected: Boolean
): DeliveryStatus {
    return when (send) {
        SendState.Sending -> DeliveryStatus.Sending
        SendState.Success -> DeliveryStatus.Success
        is SendState.Error -> DeliveryStatus.Failed
        SendState.Idle -> {
            val filled = listOf(apiKey.isNotBlank(), models.isNotEmpty()).count { it }
            when {
                filled == 0 -> DeliveryStatus.Empty
                filled == 1 -> DeliveryStatus.Partial
                // NOTE: previously blocked here with `!watchConnected ->
                // DeliveryStatus.NotConnected`, which disabled the send button
                // whenever NodeClient reported no connected node. That signal
                // is unreliable (frequent false negatives), and DataClient
                // queues + syncs writes on its own once a node is reachable,
                // so gating the button on it caused sync to silently never
                // happen. `watchConnected` is still shown as a badge in the
                // header for information, it just no longer blocks sending.
                else -> DeliveryStatus.Ready
            }
        }
    }
}

class SetupViewModel(app: Application) : AndroidViewModel(app) {

    private val store = ModelStore(app)

    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _models = MutableStateFlow<List<ModelEntry>>(emptyList())
    val models: StateFlow<List<ModelEntry>> = _models.asStateFlow()

    private val _sendState = MutableStateFlow<SendState>(SendState.Idle)
    val sendState: StateFlow<SendState> = _sendState.asStateFlow()

    private val _watchLink = MutableStateFlow(WatchLinkState())
    val watchLink: StateFlow<WatchLinkState> = _watchLink.asStateFlow()

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
            // Note: we deliberately don't block sending on watchLink.connected.
            // NodeClient's "connected" signal is unreliable (false negatives are
            // common), and DataClient queues writes and syncs them automatically
            // once a node is reachable. Blocking here previously caused sends to
            // be silently refused even when the watch was working fine.
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
