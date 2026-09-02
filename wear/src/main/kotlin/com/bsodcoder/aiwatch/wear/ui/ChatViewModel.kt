package com.bsodcoder.aiwatch.wear.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bsodcoder.aiwatch.shared.ModelEntry
import com.bsodcoder.aiwatch.wear.data.AppDatabase
import com.bsodcoder.aiwatch.wear.data.ChatEntity
import com.bsodcoder.aiwatch.wear.data.ConfigStore
import com.bsodcoder.aiwatch.wear.data.MessageEntity
import com.bsodcoder.aiwatch.wear.net.ChatCompletionMessage
import com.bsodcoder.aiwatch.wear.net.OpenRouterClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SendState {
    data object Idle : SendState
    data object Sending : SendState
    data class Error(val message: String) : SendState
}

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)

    private val _availableModels = MutableStateFlow(ConfigStore.models())
    val availableModels: StateFlow<List<ModelEntry>> = _availableModels.asStateFlow()

    init {
        ConfigStore.init(app)
        viewModelScope.launch {
            ConfigStore.config.collect { _availableModels.value = it?.models.orEmpty() }
        }
    }

    val chats: StateFlow<List<ChatEntity>> = db.chatDao().observeChats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _sendState = MutableStateFlow<SendState>(SendState.Idle)
    val sendState: StateFlow<SendState> = _sendState.asStateFlow()

    fun messagesFor(chatId: Long): StateFlow<List<MessageEntity>> =
        db.messageDao().observeMessages(chatId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Creates a new chat pinned to the given model and returns its id via callback. */
    fun createChat(model: ModelEntry, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = db.chatDao().insertChat(
                ChatEntity(title = "New chat", modelId = model.id)
            )
            onCreated(id)
        }
    }

    fun deleteChat(chatId: Long) {
        viewModelScope.launch {
            db.messageDao().deleteMessagesForChat(chatId)
            db.chatDao().deleteChat(chatId)
        }
    }

    fun sendMessage(chatId: Long, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            _sendState.value = SendState.Sending
            val chat = db.chatDao().getChat(chatId) ?: return@launch

            db.messageDao().insertMessage(MessageEntity(chatId = chatId, role = "user", content = trimmed))

            if (chat.title == "New chat") {
                db.chatDao().updateChat(chat.copy(title = trimmed.take(28), updatedAt = System.currentTimeMillis()))
            }

            runCatching {
                val existing = db.messageDao().observeMessages(chatId).first()
                val apiHistory = existing.map { ChatCompletionMessage(it.role, it.content) }
                OpenRouterClient.sendChat(ConfigStore.apiKey(), chat.modelId, apiHistory)
            }.onSuccess { reply ->
                db.messageDao().insertMessage(MessageEntity(chatId = chatId, role = "assistant", content = reply))
                db.chatDao().updateChat(chat.copy(updatedAt = System.currentTimeMillis()))
                _sendState.value = SendState.Idle
            }.onFailure { error ->
                db.messageDao().insertMessage(
                    MessageEntity(chatId = chatId, role = "assistant", content = "⚠ ${error.message}")
                )
                _sendState.value = SendState.Error(error.message ?: "Something went wrong")
            }
        }
    }
}
