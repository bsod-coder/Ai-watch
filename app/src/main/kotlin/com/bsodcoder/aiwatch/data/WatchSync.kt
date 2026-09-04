package com.bsodcoder.aiwatch.data

import android.content.Context
import com.bsodcoder.aiwatch.shared.AiWatchJson
import com.bsodcoder.aiwatch.shared.AiWatchPaths
import com.bsodcoder.aiwatch.shared.ModelEntry
import com.bsodcoder.aiwatch.shared.WatchConfig
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

/**
 * Pushes the current API key + model list to any paired watch(es)
 * using the Wearable Data Layer API. The watch's WearableListenerService
 * (see the :wear module) receives this as a DataChanged event.
 *
 * Deliberately does NOT gate on a live node connection: DataClient
 * queues writes and syncs them automatically once a node becomes
 * reachable, and NodeClient.connectedNodes is unreliable (it can
 * report empty even when the watch is paired and reachable, e.g.
 * screen off or BLE briefly idle). Gating the send on that check
 * caused pushes to be silently refused with "Not connected" even
 * when the watch was working fine.
 */
object WatchSync {

    suspend fun sendConfig(context: Context, apiKey: String, models: List<ModelEntry>) {
        val config = WatchConfig(apiKey = apiKey, models = models)
        val request = PutDataMapRequest.create(AiWatchPaths.CONFIG_PATH).apply {
            dataMap.putString(AiWatchPaths.KEY_PAYLOAD, AiWatchJson.encode(config))
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()

        Wearable.getDataClient(context).putDataItem(request).await()
    }
}
