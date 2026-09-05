package com.bsodcoder.aiwatch.wear.service

import android.util.Log
import com.bsodcoder.aiwatch.shared.AiWatchPaths
import com.bsodcoder.aiwatch.shared.AiWatchJson
import com.bsodcoder.aiwatch.wear.data.ConfigStore
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

private const val TAG = "ConfigListenerService"

/**
 * Receives WatchConfig pushes from the phone app over the Data Layer
 * and stores them via ConfigStore for the chat screens to read.
 */
class ConfigListenerService : WearableListenerService() {

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate() — service is alive, ready to receive DataChanged events")
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        Log.d(TAG, "onDataChanged() fired with ${dataEvents.count} event(s)")
        dataEvents.forEach { event ->
            Log.d(TAG, "  event type=${event.type} path=${event.dataItem.uri.path}")
            if (event.type != DataEvent.TYPE_CHANGED) return@forEach
            val item = event.dataItem
            if (item.uri.path != AiWatchPaths.CONFIG_PATH) {
                Log.d(TAG, "  ignoring: path doesn't match ${AiWatchPaths.CONFIG_PATH}")
                return@forEach
            }

            val map = DataMapItem.fromDataItem(item).dataMap
            val raw = map.getString(AiWatchPaths.KEY_PAYLOAD)
            if (raw == null) {
                Log.w(TAG, "  payload missing for key ${AiWatchPaths.KEY_PAYLOAD}")
                return@forEach
            }
            runCatching { AiWatchJson.decode(raw) }
                .onFailure { Log.e(TAG, "  failed to decode config JSON", it) }
                .getOrNull()?.let { config ->
                    Log.d(TAG, "  saving config: ${config.models.size} model(s)")
                    ConfigStore.save(applicationContext, config)
                }
        }
        dataEvents.release()
    }
}
