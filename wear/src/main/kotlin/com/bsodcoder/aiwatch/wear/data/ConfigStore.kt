package com.bsodcoder.aiwatch.wear.data

import android.content.Context
import com.bsodcoder.aiwatch.shared.AiWatchJson
import com.bsodcoder.aiwatch.shared.ModelEntry
import com.bsodcoder.aiwatch.shared.WatchConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the last WatchConfig received from the phone (API key + model
 * list). Backed by plain SharedPreferences, which is sufficient since
 * the watch app is the sole reader/writer and the value is small.
 *
 * NOTE: for stronger at-rest protection, swap this for
 * androidx.security:security-crypto's EncryptedSharedPreferences.
 */
object ConfigStore {
    private const val PREFS = "aiwatch_config"
    private const val KEY_CONFIG = "config_json"

    private val _config = MutableStateFlow<WatchConfig?>(null)
    val config: StateFlow<WatchConfig?> = _config.asStateFlow()

    fun init(context: Context) {
        if (_config.value != null) return
        val raw = prefs(context).getString(KEY_CONFIG, null) ?: return
        _config.value = runCatching { AiWatchJson.decode(raw) }.getOrNull()
    }

    fun save(context: Context, config: WatchConfig) {
        prefs(context).edit().putString(KEY_CONFIG, AiWatchJson.encode(config)).apply()
        _config.value = config
    }

    fun apiKey(): String = _config.value?.apiKey.orEmpty()
    fun models(): List<ModelEntry> = _config.value?.models.orEmpty()

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
