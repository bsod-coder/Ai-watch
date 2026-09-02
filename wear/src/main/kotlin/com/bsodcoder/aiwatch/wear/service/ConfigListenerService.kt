package com.bsodcoder.aiwatch.wear.service

import com.bsodcoder.aiwatch.shared.AiWatchPaths
import com.bsodcoder.aiwatch.shared.AiWatchJson
import com.bsodcoder.aiwatch.wear.data.ConfigStore
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

/**
 * Receives WatchConfig pushes from the phone app over the Data Layer
 * and stores them via ConfigStore for the chat screens to read.
 */
class ConfigListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type != DataEvent.TYPE_CHANGED) return@forEach
            val item = event.dataItem
            if (item.uri.path != AiWatchPaths.CONFIG_PATH) return@forEach

            val map = DataMapItem.fromDataItem(item).dataMap
            val raw = map.getString(AiWatchPaths.KEY_PAYLOAD) ?: return@forEach
            runCatching { AiWatchJson.decode(raw) }.getOrNull()?.let { config ->
                ConfigStore.save(applicationContext, config)
            }
        }
        dataEvents.release()
    }
}
