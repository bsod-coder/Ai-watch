package com.aiwatch.core.sync

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * The DataStore delegate must be a top-level property, not a member of
 * [ConfigStore]. Declaring it inside the class would build a fresh delegate per
 * instance, and DataStore throws if two instances are active for one file.
 */
private val Context.aiWatchConfigStore: DataStore<Preferences> by preferencesDataStore(
    name = "aiwatch_config",
)

/**
 * Persists the [SyncConfig] locally on whichever device is running it.
 *
 * The phone treats it as the source of truth the user edits; the watch treats it
 * as the cache that Data Layer writes land in, so a reboot never loses the key.
 */
class ConfigStore(private val context: Context) {

    /** The whole config as one JSON blob, plus the local sync timestamp. */
    val config: Flow<SyncConfig> = context.aiWatchConfigStore.data
        .catch { cause ->
            // A corrupt or half-written file should degrade to "not configured"
            // rather than crash the watch on boot.
            if (cause is IOException) emit(emptyPreferences()) else throw cause
        }
        .map { prefs -> SyncContract.decode(prefs[KEY_CONFIG_JSON]) ?: SyncConfig() }

    val lastSyncedAt: Flow<Long> = context.aiWatchConfigStore.data
        .map { it[KEY_LAST_SYNCED_AT] ?: 0L }

    /** Writes [config] as the new local truth, stamping it if the caller did not. */
    suspend fun save(config: SyncConfig) {
        val stamped = config.copy(
            updatedAt = if (config.updatedAt > 0L) config.updatedAt else System.currentTimeMillis(),
        )
        write(stamped)
    }

    /** Stores a config that arrived from the other device, verbatim. */
    suspend fun applyRemote(config: SyncConfig) = write(config)

    suspend fun setLastSynced(at: Long) {
        context.aiWatchConfigStore.edit { prefs -> prefs[KEY_LAST_SYNCED_AT] = at }
    }

    suspend fun clearApiKey() {
        context.aiWatchConfigStore.edit { prefs ->
            val current = SyncContract.decode(prefs[KEY_CONFIG_JSON]) ?: SyncConfig()
            prefs[KEY_CONFIG_JSON] = SyncContract.encode(current.copy(apiKey = ""))
        }
    }

    private suspend fun write(config: SyncConfig) {
        val encoded = SyncContract.encode(config)
        context.aiWatchConfigStore.edit { prefs ->
            prefs[KEY_CONFIG_JSON] = encoded
        }
    }

    private companion object {
        val KEY_CONFIG_JSON = stringPreferencesKey("config_json")
        val KEY_LAST_SYNCED_AT = longPreferencesKey("last_synced_at")
    }
}
