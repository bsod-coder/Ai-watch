package com.aiwatch.core.chat

import kotlinx.serialization.Serializable

/** One message in a conversation. Roles match the OpenRouter API. */
@Serializable
data class ChatTurn(
    val role: String,
    val content: String,
    val createdAt: Long,
) {
    val isUser: Boolean get() = role == ROLE_USER

    companion object {
        const val ROLE_SYSTEM: String = "system"
        const val ROLE_USER: String = "user"
        const val ROLE_ASSISTANT: String = "assistant"
    }
}

/** A saved conversation, i.e. one entry in the watch's History screen. */
@Serializable
data class Conversation(
    val id: String,
    val modelId: String,
    val modelLabel: String,
    val turns: List<ChatTurn> = emptyList(),
    val createdAt: Long,
    val updatedAt: Long = createdAt,
) {
    /** Derived from the first user message so history rows read like a summary. */
    val title: String
        get() = turns.firstOrNull { it.isUser }
            ?.content
            ?.lineSequence()
            ?.firstOrNull { it.isNotBlank() }
            ?.trim()
            ?.take(60)
            ?: "New chat"

    val lastMessage: String?
        get() = turns.lastOrNull()?.content?.takeIf { it.isNotBlank() }

    val turnCount: Int get() = turns.count { it.isUser }

    /**
     * The transcript to send to OpenRouter, excluding any in-progress assistant
     * turn (which is still being streamed and therefore incomplete).
     */
    fun historyForApi(systemPrompt: String): List<ChatTurn> {
        val transcript = turns.filter { it.content.isNotBlank() }
        return if (systemPrompt.isBlank()) {
            transcript
        } else {
            listOf(ChatTurn(ChatTurn.ROLE_SYSTEM, systemPrompt, createdAt)) + transcript
        }
    }
}

/** Envelope so the persisted file can gain fields without breaking old saves. */
@Serializable
data class ConversationStore(
    val conversations: List<Conversation> = emptyList(),
)
