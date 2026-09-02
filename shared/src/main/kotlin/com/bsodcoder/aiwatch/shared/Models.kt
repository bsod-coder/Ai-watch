package com.bsodcoder.aiwatch.shared

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * A single model entry the user has configured on the phone,
 * e.g. id = "deepseek/deepseek-v4-flash-0731"
 */
@Serializable
data class ModelEntry(
    val id: String,
    val displayName: String = id
)

/**
 * The full payload sent from the phone to the watch via the
 * Wearable Data Layer API. Kept intentionally small since Data
 * Layer payloads should stay well under 100kb.
 */
@Serializable
data class WatchConfig(
    val apiKey: String,
    val models: List<ModelEntry>,
    val updatedAt: Long = System.currentTimeMillis()
)

object AiWatchPaths {
    /** DataItem path used for phone -> watch config sync. */
    const val CONFIG_PATH = "/aiwatch/config"
    const val KEY_PAYLOAD = "payload"
}

object AiWatchJson {
    val instance: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(config: WatchConfig): String = instance.encodeToString(config)

    fun decode(raw: String): WatchConfig = instance.decodeFromString(raw)
}
