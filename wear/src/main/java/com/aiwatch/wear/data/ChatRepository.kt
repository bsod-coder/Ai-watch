package com.aiwatch.wear.data

import android.content.Context
import com.aiwatch.core.chat.Conversation
import com.aiwatch.core.chat.ConversationStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Conversation history, persisted as a single JSON document in the app's private
 * storage.
 *
 * A watch accumulates tens of conversations, not millions, so a whole-file write
 * behind a [Mutex] is simpler and has fewer moving parts than a database, and it
 * keeps the build free of annotation processors. Swap in Room here if the volume
 * ever grows.
 */
class ChatRepository(private val context: Context) {

    private val mutex = Mutex()
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private var loaded = false

    private val file: File
        get() = File(context.filesDir, FILE_NAME)

    /** Reads the file once. Later calls are cheap. */
    suspend fun ensureLoaded() = mutex.withLock {
        if (loaded) return@withLock
        _conversations.value = readFromDisk()
        loaded = true
    }

    suspend fun upsert(conversation: Conversation) = mutex.withLock {
        if (!loaded) {
            _conversations.value = readFromDisk()
            loaded = true
        }
        val next = _conversations.value.toMutableList()
        val index = next.indexOfFirst { it.id == conversation.id }
        if (index >= 0) next[index] = conversation else next.add(0, conversation)
        val sorted = next.sortedByDescending { it.updatedAt }
        // Bound the file so a long-lived watch cannot grow it without limit.
        val capped = if (sorted.size > MAX_CONVERSATIONS) {
            sorted.take(MAX_CONVERSATIONS)
        } else {
            sorted
        }
        _conversations.value = capped
        writeLocked(capped)
    }

    suspend fun delete(id: String) = mutex.withLock {
        val next = _conversations.value.filterNot { it.id == id }
        _conversations.value = next
        writeLocked(next)
    }

    suspend fun clearAll() = mutex.withLock {
        _conversations.value = emptyList()
        writeLocked(emptyList())
    }

    fun find(id: String?): Conversation? =
        id?.let { target -> _conversations.value.firstOrNull { it.id == target } }

    private fun readFromDisk(): List<Conversation> {
        val target = file
        if (!target.exists()) return emptyList()
        return runCatching {
            json.decodeFromString(ConversationStore.serializer(), target.readText())
                .conversations
                .sortedByDescending { it.updatedAt }
        }.getOrDefault(emptyList())
    }

    private fun writeLocked(conversations: List<Conversation>) {
        runCatching {
            val encoded = json.encodeToString(
                ConversationStore.serializer(),
                ConversationStore(conversations),
            )
            // Write to a temp file and rename so a crash mid-write cannot leave
            // a truncated document behind.
            val temp = File(context.filesDir, "$FILE_NAME.tmp")
            temp.writeText(encoded)
            if (!temp.renameTo(file)) {
                file.writeText(encoded)
                temp.delete()
            }
        }
    }

    private companion object {
        const val FILE_NAME = "conversations.json"
        const val MAX_CONVERSATIONS = 100
    }
}
