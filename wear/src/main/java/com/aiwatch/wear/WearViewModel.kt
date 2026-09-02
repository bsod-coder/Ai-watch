package com.aiwatch.wear

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aiwatch.core.chat.ChatTurn
import com.aiwatch.core.chat.Conversation
import com.aiwatch.core.model.ModelEntry
import com.aiwatch.core.net.ChatMessage
import com.aiwatch.core.net.OpenRouterClient
import com.aiwatch.core.net.OpenRouterException
import com.aiwatch.core.sync.ConfigStore
import com.aiwatch.core.sync.SyncConfig
import com.aiwatch.core.sync.WearConfigBridge
import com.aiwatch.wear.data.ChatRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Watch navigation. `Models` is deliberately not a separate screen: the Home
 * screen lists the synced models and starting one *is* the selection.
 */
enum class Screen { Home, Chat, History }

data class WearUiState(
    val configLoaded: Boolean = false,
    val config: SyncConfig = SyncConfig(),
    val conversations: List<Conversation> = emptyList(),
    val screen: Screen = Screen.Home,
    val activeId: String? = null,
    val input: String = "",
    /** Non-empty while an assistant reply is still arriving. */
    val streamingText: String = "",
    val isStreaming: Boolean = false,
    val error: String? = null,
    val isRequestingConfig: Boolean = false,
    /** Drives BackHandler: when false the system back-swipe exits the app. */
    val canGoBack: Boolean = false,
) {
    val activeConversation: Conversation?
        get() = conversations.firstOrNull { it.id == activeId }

    val isConfigured: Boolean get() = config.isReady
}

class WearViewModel(application: Application) : AndroidViewModel(application) {

    private val store = ConfigStore(application)
    private val repository = ChatRepository(application)
    private val api = OpenRouterClient()

    private val _state = MutableStateFlow(WearUiState())
    val state: StateFlow<WearUiState> = _state.asStateFlow()

    private val backStack = ArrayDeque<Screen>()
    private var streamJob: Job? = null

    init {
        viewModelScope.launch {
            repository.ensureLoaded()
            repository.conversations.collect { conversations ->
                _state.update { it.copy(conversations = conversations) }
            }
        }
        viewModelScope.launch {
            store.config.collect { config ->
                _state.update { current ->
                    current.copy(configLoaded = true, config = config)
                }
            }
        }
        // A freshly paired watch has no DataItem yet, so ask the phone once.
        // Deliberately only inspects the first emission, so a later edit cannot
        // trigger another request.
        viewModelScope.launch {
            if (!store.config.first().isReady) requestConfig()
        }
    }

    /* ------------------------------------------------------------ config */

    fun requestConfig() {
        _state.update { it.copy(isRequestingConfig = true, error = null) }
        viewModelScope.launch {
            val reached = WearConfigBridge.requestFromPhone(getApplication())
            _state.update {
                it.copy(
                    isRequestingConfig = false,
                    error = if (reached == 0) "No phone in range" else null,
                )
            }
        }
    }

    /* -------------------------------------------------------- navigation */

    fun goTo(next: Screen) {
        backStack.addLast(_state.value.screen)
        _state.update { it.copy(screen = next, error = null, canGoBack = true) }
    }

    /** @return false when there was nothing left to pop. */
    fun back(): Boolean {
        if (isStreamingActive()) stopStreaming()
        val previous = backStack.removeLastOrNull() ?: return false
        _state.update {
            it.copy(screen = previous, error = null, canGoBack = backStack.isNotEmpty())
        }
        return true
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    /* ------------------------------------------------------------- chats */

    fun startChat(model: ModelEntry) {
        val now = System.currentTimeMillis()
        val conversation = Conversation(
            id = UUID.randomUUID().toString(),
            modelId = model.id,
            modelLabel = model.label,
            turns = emptyList(),
            createdAt = now,
            updatedAt = now,
        )
        viewModelScope.launch {
            repository.upsert(conversation)
            _state.update {
                it.copy(activeId = conversation.id, input = "", streamingText = "", error = null)
            }
            goTo(Screen.Chat)
        }
    }

    fun startChatWithDefault() {
        val model = _state.value.config.preferredModel ?: return
        startChat(model)
    }

    fun openConversation(id: String) {
        stopStreaming()
        backStack.addLast(_state.value.screen)
        _state.update {
            it.copy(
                activeId = id,
                screen = Screen.Chat,
                streamingText = "",
                error = null,
                canGoBack = true,
            )
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            repository.delete(id)
            _state.update { current ->
                current.copy(
                    activeId = if (current.activeId == id) null else current.activeId,
                    screen = if (current.activeId == id) Screen.History else current.screen,
                )
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            stopStreaming()
            repository.clearAll()
            _state.update { it.copy(activeId = null) }
        }
    }

    fun onInputChange(value: String) {
        _state.update { it.copy(input = value) }
    }

    fun sendQueuedInput() = send(_state.value.input)

    /* --------------------------------------------------------- streaming */

    fun send(rawText: String) {
        val text = rawText.trim()
        if (text.isEmpty()) return

        val current = _state.value
        if (current.isStreaming) return

        val conversation = current.activeConversation ?: return
        if (current.config.apiKey.isBlank()) {
            _state.update { it.copy(error = "No key on the watch") }
            return
        }

        val now = System.currentTimeMillis()
        val withUser = conversation.copy(
            turns = conversation.turns + ChatTurn(ChatTurn.ROLE_USER, text, now),
            updatedAt = now,
        )

        _state.update {
            it.copy(
                input = "",
                error = null,
                streamingText = "",
                isStreaming = true,
            )
        }

        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            repository.upsert(withUser)

            val messages = withUser.historyForApi(current.config.systemPrompt)
                .map { ChatMessage(it.role, it.content) }

            val builder = StringBuilder()
            var failure: String? = null

            try {
                api.streamChat(
                    apiKey = current.config.apiKey,
                    model = withUser.modelId,
                    messages = messages,
                    temperature = current.config.temperature,
                    maxTokens = current.config.maxTokens,
                ).collect { delta ->
                    builder.append(delta)
                    _state.update { it.copy(streamingText = builder.toString()) }
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (t: Throwable) {
                failure = describe(t)
            }

            // Persist whatever arrived, even on a truncated stream, so the user
            // does not lose a half-written answer.
            val reply = builder.toString()
            if (reply.isNotBlank()) {
                val stamp = System.currentTimeMillis()
                repository.upsert(
                    withUser.copy(
                        turns = withUser.turns + ChatTurn(ChatTurn.ROLE_ASSISTANT, reply, stamp),
                        updatedAt = stamp,
                    ),
                )
            }

            _state.update {
                it.copy(isStreaming = false, streamingText = "", error = failure ?: it.error)
            }
        }
    }

    fun stopStreaming() {
        if (streamJob?.isActive != true) return
        streamJob?.cancel()
        streamJob = null
        _state.update { it.copy(isStreaming = false, streamingText = "") }
    }

    private fun isStreamingActive(): Boolean = streamJob?.isActive == true

    private fun describe(t: Throwable): String = when (t) {
        is OpenRouterException -> "${t.shortHint}: ${t.message ?: "request failed"}"
        else -> t.message ?: "Request failed"
    }
}
