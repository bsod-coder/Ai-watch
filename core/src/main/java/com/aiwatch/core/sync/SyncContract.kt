package com.aiwatch.core.sync

import kotlinx.serialization.json.Json

/**
 * The wire contract between the phone and the watch. Both sides depend on this
 * object, so a change here is automatically seen by both apps.
 */
object SyncContract {

    /** DataItem path carrying the full [SyncConfig]. */
    const val CONFIG_PATH: String = "/aiwatch/config"

    /** Message path the watch sends when it boots with no stored config. */
    const val REQUEST_CONFIG_PATH: String = "/aiwatch/request-config"

    /** DataMap keys. */
    const val FIELD_CONFIG: String = "config"
    const val FIELD_VERSION: String = "version"
    const val FIELD_UPDATED_AT: String = "updatedAt"

    /** Bump when the shape of [SyncConfig] changes incompatibly. */
    const val PROTOCOL_VERSION: Int = 1

    /**
     * The Data Layer limit is ~100 KB; we leave headroom for the DataMap
     * overhead and for the UTF-8 expansion of non-ASCII characters.
     */
    const val MAX_PAYLOAD_BYTES: Int = 90_000

    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        prettyPrint = false
    }

    fun encode(config: SyncConfig): String =
        json.encodeToString(SyncConfig.serializer(), config)

    /** Never throws: a malformed or stale payload simply yields null. */
    fun decode(raw: String?): SyncConfig? {
        if (raw.isNullOrBlank()) return null
        return runCatching { json.decodeFromString(SyncConfig.serializer(), raw) }.getOrNull()
    }

    /**
     * Returns a config small enough to cross the Data Layer, dropping models from
     * the tail if the user's list is enormous.
     */
    fun fit(config: SyncConfig): PayloadBudget.FitResult<com.aiwatch.core.model.ModelEntry> {
        val full = encode(config)
        if (full.toByteArray(Charsets.UTF_8).size <= MAX_PAYLOAD_BYTES) {
            return PayloadBudget.FitResult(kept = config.models, dropped = 0)
        }
        val result = PayloadBudget.fitReportingDropped(
            items = config.models,
            maxBytes = MAX_PAYLOAD_BYTES,
            encodedSize = { models ->
                encode(config.copy(models = models)).toByteArray(Charsets.UTF_8).size
            },
        )
        return result
    }

    fun encodeForTransport(config: SyncConfig): String {
        val fitted = fit(config)
        return encode(config.copy(models = fitted.kept))
    }
}
