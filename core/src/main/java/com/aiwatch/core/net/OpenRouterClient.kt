package com.aiwatch.core.net

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Thin OpenRouter client. Used by the phone to validate a key and pull the model
 * catalogue, and by the watch to stream replies.
 *
 * Both apps call OpenRouter directly; the phone's only job is to hand the key
 * and the chosen model list to the watch over the Data Layer.
 */
class OpenRouterClient(
    private val client: OkHttpClient = defaultClient(),
    private val appTitle: String = DEFAULT_APP_TITLE,
    private val referer: String = DEFAULT_REFERER,
) {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    /** `GET /api/v1/key` — cheap way to confirm a key is live and see its quota. */
    suspend fun verifyKey(apiKey: String): Result<KeyInfo> = runCatching {
        val request = Request.Builder()
            .url("$BASE_URL/key")
            .get()
            .withAuth(apiKey)
            .build()

        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw errorFrom(response.code, text)
            val parsed = json.decodeFromString(KeyInfo.serializer(), text)
            if (parsed.error != null) {
                throw OpenRouterException(response.code, parsed.error.message ?: "Key rejected")
            }
            parsed
        }
    }.mapNetworkFailure()

    /** `GET /api/v1/models` — the full catalogue, used to populate the picker. */
    suspend fun listModels(apiKey: String): Result<List<CatalogModel>> = runCatching {
        val request = Request.Builder()
            .url("$BASE_URL/models")
            .get()
            .withAuth(apiKey)
            .build()

        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw errorFrom(response.code, text)
            val parsed = json.decodeFromString(CatalogResponse.serializer(), text)
            parsed.data.filter { it.id.isNotBlank() }
                .sortedWith(compareByDescending<CatalogModel> { it.isFree }.thenBy { it.id })
        }
    }.mapNetworkFailure()

    /**
     * `POST /api/v1/chat/completions` with `stream = true`.
     *
     * Emits text deltas as they arrive and completes normally at `[DONE]`.
     * Failures surface on the flow as an [OpenRouterException].
     *
     * The bridge channel is unlimited on purpose: OkHttp delivers on its own
     * thread and must never have a delta dropped because a collector is briefly
     * slow. Back-pressure is applied at `emit`, which suspends the iterator
     * rather than losing text.
     */
    fun streamChat(
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        temperature: Float? = null,
        maxTokens: Int? = null,
    ): Flow<String> = flow {
        val payload = ChatRequest(
            model = model,
            messages = messages,
            temperature = temperature,
            maxTokens = maxTokens,
            stream = true,
        )
        val request = Request.Builder()
            .url("$BASE_URL/chat/completions")
            .post(json.encodeToString(ChatRequest.serializer(), payload).toRequestBody(JSON_MEDIA_TYPE))
            .withAuth(apiKey)
            .build()

        val bridge = Channel<String>(Channel.UNLIMITED)
        val call = client.newCall(request)

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                bridge.close(OpenRouterException(0, e.message ?: "Network error"))
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    try {
                        // Only read the body as text on the error path; on the
                        // success path the body IS the stream we must consume.
                        if (!resp.isSuccessful) {
                            bridge.close(errorFrom(resp.code, resp.body?.string().orEmpty()))
                            return
                        }
                        val source = resp.body?.source()
                        if (source == null) {
                            bridge.close(OpenRouterException(resp.code, "Empty response"))
                            return
                        }
                        val assembler = SseFrameAssembler()
                        var finished = false
                        // No break inside a lambda here: non-local break and
                        // continue from inline lambdas is experimental in Kotlin.
                        while (!finished) {
                            val line = source.readUtf8Line()
                            if (line == null) {
                                assembler.flush()?.let { tail -> forward(bridge, tail) }
                                finished = true
                                continue
                            }
                            val frame = assembler.onLine(line) ?: continue
                            if (SseFrameAssembler.isDone(frame)) {
                                finished = true
                                continue
                            }
                            forward(bridge, frame)
                        }
                        bridge.close()
                    } catch (t: Throwable) {
                        bridge.close(
                            if (t is OpenRouterException) {
                                t
                            } else {
                                OpenRouterException(0, t.message ?: "Stream failed")
                            },
                        )
                    }
                }
            }
        })

        try {
            for (delta in bridge) emit(delta)
        } finally {
            call.cancel()
            bridge.cancel()
        }
    }

    /** Non-streaming completion, used by the phone's "test this model" action. */
    suspend fun complete(
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        temperature: Float? = null,
        maxTokens: Int? = null,
    ): Result<String> = runCatching {
        val payload = ChatRequest(
            model = model,
            messages = messages,
            temperature = temperature,
            maxTokens = maxTokens,
            stream = false,
        )
        val request = Request.Builder()
            .url("$BASE_URL/chat/completions")
            .post(json.encodeToString(ChatRequest.serializer(), payload).toRequestBody(JSON_MEDIA_TYPE))
            .withAuth(apiKey)
            .build()

        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw errorFrom(response.code, text)
            val parsed = json.decodeFromString(ChatCompletion.serializer(), text)
            parsed.choices.firstOrNull()?.message?.content
                ?: throw OpenRouterException(response.code, "Model returned no content")
        }
    }.mapNetworkFailure()

    private fun extractDelta(frame: String): String? = runCatching {
        json.decodeFromString(StreamChunk.serializer(), frame)
            .choices
            .firstOrNull()
            ?.delta
            ?.content
    }.getOrNull()

    /**
     * Pushes one SSE frame's text onto the bridge channel. The channel is
     * unlimited, so `trySend` cannot fail and no delta is ever dropped.
     */
    private fun forward(bridge: Channel<String>, frame: String) {
        val delta = extractDelta(frame)
        if (delta.isNullOrEmpty()) return
        bridge.trySend(delta)
    }

    private fun Request.Builder.withAuth(apiKey: String): Request.Builder = this
        .header("Authorization", "Bearer $apiKey")
        .header("Content-Type", "application/json")
        // OpenRouter asks for these to attribute traffic in its rankings.
        .header("HTTP-Referer", referer)
        .header("X-Title", appTitle)

    private fun errorFrom(status: Int, body: String): OpenRouterException {
        val parsed = runCatching {
            json.decodeFromString(ApiErrorEnvelope.serializer(), body)
        }.getOrNull()
        val message = parsed?.error?.message
            ?: body.take(300).ifBlank { "HTTP $status" }
        return OpenRouterException(status, message, parsed?.error?.code)
    }

    /**
     * Normalises transport failures (DNS, TLS, socket) onto the same exception
     * type as HTTP failures, so callers have one thing to handle.
     */
    private fun <T> Result<T>.mapNetworkFailure(): Result<T> {
        val cause = exceptionOrNull() ?: return this
        val mapped = if (cause is OpenRouterException) {
            cause
        } else {
            OpenRouterException(0, cause.message ?: "Network error")
        }
        return Result.failure(mapped)
    }

    companion object {
        const val BASE_URL: String = "https://openrouter.ai/api/v1"
        const val DEFAULT_APP_TITLE: String = "AiWatch"
        const val DEFAULT_REFERER: String = "https://github.com/bsod-coder/Ai-watch"

        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
