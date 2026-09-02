package com.aiwatch.wear.sync

import com.aiwatch.core.sync.ConfigStore
import com.aiwatch.core.sync.SyncContract
import com.aiwatch.core.sync.WearConfigBridge
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Receives the config the phone publishes.
 *
 * The [DataEventBuffer] is recycled as soon as this method returns, so every
 * field is copied out synchronously and only the DataStore write is deferred.
 */
class ConfigListenerService : WearableListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDataChanged(events: DataEventBuffer) {
        var newest = -1L
        var incoming: com.aiwatch.core.sync.SyncConfig? = null

        for (event in events) {
            if (event.type != DataEvent.TYPE_CHANGED) continue
            val item = event.dataItem
            if (item.uri?.path != SyncContract.CONFIG_PATH) continue
            val config = WearConfigBridge.read(item) ?: continue
            if (config.updatedAt >= newest) {
                newest = config.updatedAt
                incoming = config
            }
        }

        val config = incoming ?: return
        scope.launch {
            val store = ConfigStore(applicationContext)
            // Ignore a stale replica that arrives out of order.
            if (config.updatedAt >= store.config.first().updatedAt) {
                store.applyRemote(config)
                store.setLastSyncedAt(System.currentTimeMillis())
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
