package com.bsodcoder.aiwatch.wear.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@Serializable
data class ChatCompletionMessage(val role: String, val content: String)

@Serializable
private data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatCompletionMessage>
)

@Serializable
private data class ChatCompletionChoice(val message: ChatCompletionMessage)

@Serializable
private data class ChatCompletionResponse(val choices: List<ChatCompletionChoice> = emptyList())

@Serializable
private data class OpenRouterErrorBody(val error: OpenRouterErrorDetail? = null)

@Serializable
private data class OpenRouterErrorDetail(val message: String? = null)

class OpenRouterException(message: String) : Exception(message)

/**
 * Minimal client for OpenRouter's chat completions endpoint, called
 * directly from the watch. Kept dependency-light (plain OkHttp, no
 * Retrofit) since APK size matters more on Wear OS.
 */
object OpenRouterClient {

    private const val ENDPOINT = "https://openrouter.ai/api/v1/chat/completions"
    private val json = Json { ignoreUnknownKeys = true }

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun sendChat(apiKey: String, model: String, history: List<ChatCompletionMessage>): String =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) throw OpenRouterException("No API key configured. Set one up on your phone.")

            val body = json.encodeToString(ChatCompletionRequest(model = model, messages = history))
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(ENDPOINT)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("HTTP-Referer", "https://github.com/bsod-coder/Ai-watch")
                .addHeader("X-Title", "AI Watch")
                .post(body)
                .build()

            http.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val message = runCatching { json.decodeFromString<OpenRouterErrorBody>(raw).error?.message }
                        .getOrNull() ?: "Request failed (${response.code})"
                    throw OpenRouterException(message)
                }
                val parsed = runCatching { json.decodeFromString<ChatCompletionResponse>(raw) }.getOrNull()
                parsed?.choices?.firstOrNull()?.message?.content
                    ?: throw OpenRouterException("Empty response from model")
            }
        }
}
