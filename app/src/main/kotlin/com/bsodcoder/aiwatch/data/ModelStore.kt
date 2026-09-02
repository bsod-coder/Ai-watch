package com.bsodcoder.aiwatch.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bsodcoder.aiwatch.shared.AiWatchJson
import com.bsodcoder.aiwatch.shared.ModelEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

private val Context.dataStore by preferencesDataStore(name = "aiwatch_prefs")

/**
 * Persists the OpenRouter API key and the list of configured models
 * locally on the phone, so the setup screen survives process death.
 */
class ModelStore(private val context: Context) {

    private val keyApiKey = stringPreferencesKey("api_key")
    private val keyModels = stringPreferencesKey("models_json")

    val apiKey: Flow<String> = context.dataStore.data.map { it[keyApiKey] ?: "" }

    val models: Flow<List<ModelEntry>> = context.dataStore.data.map { prefs ->
        val raw = prefs[keyModels] ?: return@map emptyList()
        runCatching {
            AiWatchJson.instance.decodeFromString(ListSerializer(ModelEntry.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    suspend fun setApiKey(value: String) {
        context.dataStore.edit { it[keyApiKey] = value }
    }

    suspend fun setModels(models: List<ModelEntry>) {
        context.dataStore.edit {
            it[keyModels] = AiWatchJson.instance.encodeToString(ListSerializer(ModelEntry.serializer()), models)
        }
    }
}
