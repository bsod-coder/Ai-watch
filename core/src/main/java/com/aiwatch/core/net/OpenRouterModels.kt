package com.aiwatch.core.net

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/* ---- request / response bodies for POST /api/v1/chat/completions ---- */

@Serializable
data class ChatMessage(
    val role: String,
    val content: String,
) {
    companion object {
        const val ROLE_SYSTEM: String = "system"
        const val ROLE_USER: String = "user"
        const val ROLE_ASSISTANT: String = "assistant"
    }
}

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Float? = null,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    val stream: Boolean = false,
)

@Serializable
data class ResponseMessage(
    val role: String? = null,
    val content: String? = null,
)

@Serializable
data class ChatChoice(
    val index: Int = 0,
    val message: ResponseMessage? = null,
    @SerialName("finish_reason") val finishReason: String? = null,
)

@Serializable
data class TokenUsage(
    @SerialName("prompt_tokens") val promptTokens: Int = 0,
    @SerialName("completion_tokens") val completionTokens: Int = 0,
    @SerialName("total_tokens") val totalTokens: Int = 0,
)

@Serializable
data class ChatCompletion(
    val id: String? = null,
    val model: String? = null,
    val choices: List<ChatChoice> = emptyList(),
    val usage: TokenUsage? = null,
)

/* ---- streaming deltas ---- */

@Serializable
data class StreamDelta(
    val role: String? = null,
    val content: String? = null,
)

@Serializable
data class StreamChoice(
    val index: Int = 0,
    val delta: StreamDelta? = null,
    @SerialName("finish_reason") val finishReason: String? = null,
)

@Serializable
data class StreamChunk(
    val id: String? = null,
    val choices: List<StreamChoice> = emptyList(),
)

/* ---- GET /api/v1/key ---- */

@Serializable
data class KeyInfoData(
    val label: String? = null,
    val usage: Double? = null,
    val limit: Double? = null,
    @SerialName("is_free_tier") val isFreeTier: Boolean? = null,
    @SerialName("rate_limit") val rateLimit: RateLimit? = null,
)

@Serializable
data class RateLimit(
    val requests: Int? = null,
    val interval: String? = null,
)

/* ---- GET /api/v1/models ---- */

@Serializable
data class CatalogPricing(
    val prompt: String? = null,
    val completion: String? = null,
)

@Serializable
data class CatalogModel(
    val id: String = "",
    val name: String? = null,
    val description: String? = null,
    @SerialName("context_length") val contextLength: Long? = null,
    val pricing: CatalogPricing? = null,
) {
    val isFree: Boolean
        get() = pricing?.prompt?.toDoubleOrNull() == 0.0 &&
            pricing?.completion?.toDoubleOrNull() == 0.0
}

@Serializable
data class CatalogResponse(
    val data: List<CatalogModel> = emptyList(),
)

/* ---- error envelope ---- */

@Serializable
data class ApiError(
    val message: String? = null,
    val code: Int? = null,
)

@Serializable
data class ApiErrorEnvelope(
    val error: ApiError? = null,
)

@Serializable
data class KeyInfo(
    val data: KeyInfoData? = null,
    val error: ApiError? = null,
)
