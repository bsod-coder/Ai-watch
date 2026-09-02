package com.aiwatch.phone

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aiwatch.core.model.ModelEntry
import com.aiwatch.core.net.CatalogModel
import com.aiwatch.core.net.OpenRouterClient
import com.aiwatch.core.net.OpenRouterException
import com.aiwatch.core.sync.ConfigStore
import com.aiwatch.core.sync.PeerInfo
import com.aiwatch.core.sync.PublishOutcome
import com.aiwatch.core.sync.SyncConfig
import com.aiwatch.core.sync.WearConfigBridge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Result of validating the API key against OpenRouter. */
sealed interface KeyCheck {
    data object Idle : KeyCheck
    data object Checking : KeyCheck
    data class Valid(
        val label: String?,
        val usage: Double?,
        val isFreeTier: Boolean?,
    ) : KeyCheck

    data class Invalid(val message: String) : KeyCheck
}

/** State of the "browse the OpenRouter catalogue" sheet. */
sealed interface CatalogState {
    data object Idle : CatalogState
    data object Loading : CatalogState
    data class Ready(val models: List<CatalogModel>) : CatalogState
    data class Error(val message: String) : CatalogState
}

data class Banner(val text: String, val isError: Boolean)

data class PhoneUiState(
    val isLoaded: Boolean = false,
    val config: SyncConfig = SyncConfig(),
    val draftKey: String = "",
    val keyVisible: Boolean = false,
    val keyCheck: KeyCheck = KeyCheck.Idle,
    val peers: List<PeerInfo> = emptyList(),
    val isSyncing: Boolean = false,
    val lastSyncedAt: Long = 0L,
    val catalog: CatalogState = CatalogState.Idle,
    val banner: Banner? = null,
)

class PhoneViewModel(application: Application) : AndroidViewModel(application) {

    private val store = ConfigStore(application)
    private val api = OpenRouterClient()

    private val _state = MutableStateFlow(PhoneUiState())
    val state: StateFlow<PhoneUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            store.config.collect { config ->
                _state.update { current ->
                    current.copy(
                        isLoaded = true,
                        config = config,
                        // Keep the text field in step with storage until the user
                        // starts typing their own edit.
                        draftKey = if (current.draftKey.isBlank()) config.apiKey else current.draftKey,
                    )
                }
            }
        }
        viewModelScope.launch {
            store.lastSyncedAt.collect { at ->
                _state.update { it.copy(lastSyncedAt = at) }
            }
        }
        refreshPeers()
    }

    /* ---------------------------------------------------------------- key */

    fun onDraftKeyChange(value: String) {
        _state.update { it.copy(draftKey = value.trim(), keyCheck = KeyCheck.Idle) }
    }

    fun toggleKeyVisible() {
        _state.update { it.copy(keyVisible = !it.keyVisible) }
    }

    fun saveKey() {
        val key = _state.value.draftKey.trim()
        if (key.isBlank()) {
            _state.update { it.copy(banner = Banner("Enter a key first", isError = true)) }
            return
        }
        viewModelScope.launch {
            store.save(_state.value.config.copy(apiKey = key))
            _state.update { it.copy(banner = Banner("Key saved", isError = false)) }
        }
    }

    fun clearKey() {
        viewModelScope.launch {
            store.save(_state.value.config.copy(apiKey = ""))
            // Push the removal to the watch so a lost phone does not leave a
            // usable key sitting on the wrist.
            WearConfigBridge.publish(getApplication(), _state.value.config.copy(apiKey = ""))
            _state.update {
                it.copy(
                    draftKey = "",
                    keyCheck = KeyCheck.Idle,
                    banner = Banner("Key removed from this device and the watch", isError = false),
                )
            }
        }
    }

    fun testKey() {
        val key = _state.value.draftKey.trim()
        if (key.isBlank()) {
            _state.update { it.copy(banner = Banner("Enter a key first", isError = true)) }
            return
        }
        _state.update { it.copy(keyCheck = KeyCheck.Checking) }
        viewModelScope.launch {
            api.verifyKey(key)
                .onSuccess { info ->
                    val data = info.data
                    _state.update {
                        it.copy(
                            keyCheck = KeyCheck.Valid(
                                label = data?.label,
                                usage = data?.usage,
                                isFreeTier = data?.isFreeTier,
                            ),
                        )
                    }
                }
                .onFailure { t ->
                    _state.update {
                        it.copy(keyCheck = KeyCheck.Invalid(describe(t)))
                    }
                }
        }
    }

    /* ------------------------------------------------------------- models */

    fun addModelById(raw: String) {
        val entry = ModelEntry.parse(raw) ?: return
        val current = _state.value.config.models
        if (current.any { it.id == entry.id }) {
            _state.update { it.copy(banner = Banner("Already in your list", isError = false)) }
            return
        }
        val next = current + entry
        viewModelScope.launch {
            store.save(_state.value.config.withModels(next))
            _state.update { it.copy(banner = Banner("Added ${entry.shortId}", isError = false)) }
        }
    }

    fun addFromCatalog(model: CatalogModel) {
        val entry = ModelEntry(
            id = model.id,
            label = model.name?.takeIf { it.isNotBlank() } ?: model.id,
            contextLength = model.contextLength,
        )
        val current = _state.value.config.models
        if (current.any { it.id == entry.id }) {
            _state.update { it.copy(banner = Banner("Already in your list", isError = false)) }
            return
        }
        viewModelScope.launch {
            store.save(_state.value.config.withModels(current + entry))
        }
    }

    fun removeModel(id: String) {
        val next = _state.value.config.models.filterNot { it.id == id }
        viewModelScope.launch {
            store.save(_state.value.config.withModels(next))
        }
    }

    fun makeDefault(id: String) {
        viewModelScope.launch {
            store.save(_state.value.config.copy(defaultModelId = id))
        }
    }

    fun moveModel(id: String, offset: Int) {
        val models = _state.value.config.models.toMutableList()
        val from = models.indexOfFirst { it.id == id }
        if (from < 0) return
        val to = (from + offset).coerceIn(0, models.size - 1)
        if (from == to) return
        val moved = models.removeAt(from)
        models.add(to, moved)
        viewModelScope.launch {
            store.save(_state.value.config.withModels(models))
        }
    }

    fun loadCatalog() {
        val key = _state.value.config.apiKey.ifBlank { _state.value.draftKey.trim() }
        if (key.isBlank()) {
            _state.update { it.copy(catalog = CatalogState.Error("Save a key first")) }
            return
        }
        _state.update { it.copy(catalog = CatalogState.Loading) }
        viewModelScope.launch {
            api.listModels(key)
                .onSuccess { models ->
                    _state.update { it.copy(catalog = CatalogState.Ready(models)) }
                }
                .onFailure { t ->
                    _state.update { it.copy(catalog = CatalogState.Error(describe(t))) }
                }
        }
    }

    fun closeCatalog() {
        _state.update { it.copy(catalog = CatalogState.Idle) }
    }

    /* -------------------------------------------------------------- sync */

    fun refreshPeers() {
        viewModelScope.launch {
            val peers = WearConfigBridge.connectedPeers(getApplication())
            _state.update { it.copy(peers = peers) }
        }
    }

    fun syncToWatch() {
        val config = _state.value.config
        if (config.apiKey.isBlank()) {
            _state.update { it.copy(banner = Banner("Add a key before syncing", isError = true)) }
            return
        }
        if (config.models.isEmpty()) {
            _state.update { it.copy(banner = Banner("Add at least one model before syncing", isError = true)) }
            return
        }
        _state.update { it.copy(isSyncing = true) }
        viewModelScope.launch {
            val stamped = config.copy(updatedAt = System.currentTimeMillis())
            store.save(stamped)
            when (val outcome = WearConfigBridge.publish(getApplication(), stamped)) {
                is PublishOutcome.Sent -> {
                    store.setLastSyncedAt(System.currentTimeMillis())
                    val text = if (outcome.modelsDropped > 0) {
                        "Sent. List was too large for the Data Layer — kept the first " +
                            "${stamped.models.size - outcome.modelsDropped} models."
                    } else {
                        "Sent to ${outcome.peerCount} watch${if (outcome.peerCount == 1) "" else "es"}"
                    }
                    _state.update {
                        it.copy(isSyncing = false, banner = Banner(text, isError = false))
                    }
                }

                is PublishOutcome.NoPeer -> {
                    store.setLastSyncedAt(System.currentTimeMillis())
                    _state.update {
                        it.copy(isSyncing = false, banner = Banner(outcome.message, isError = false))
                    }
                }

                is PublishOutcome.Failed -> _state.update {
                    it.copy(isSyncing = false, banner = Banner(outcome.message, isError = true))
                }
            }
            refreshPeers()
        }
    }

    /* ---------------------------------------------------------- settings */

    fun setTemperature(value: Float) {
        viewModelScope.launch {
            store.save(_state.value.config.copy(temperature = value))
        }
    }

    fun setMaxTokens(value: Int) {
        viewModelScope.launch {
            store.save(_state.value.config.copy(maxTokens = value))
        }
    }

    fun setSystemPrompt(value: String) {
        viewModelScope.launch {
            store.save(_state.value.config.copy(systemPrompt = value))
        }
    }

    fun dismissBanner() {
        _state.update { it.copy(banner = null) }
    }

    private fun describe(t: Throwable): String = when (t) {
        is OpenRouterException -> t.message ?: t.shortHint
        else -> "No connection to OpenRouter"
    }
}
