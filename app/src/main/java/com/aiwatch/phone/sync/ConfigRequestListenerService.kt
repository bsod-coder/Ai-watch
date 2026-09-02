package com.aiwatch.phone.sync

import com.aiwatch.core.sync.ConfigStore
import com.aiwatch.core.sync.SyncContract
import com.aiwatch.core.sync.WearConfigBridge
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Answers the watch when it boots with no stored configuration.
 *
 * Play services starts this service on demand, so it must not assume the phone
 * UI is alive. It reads the config straight from DataStore and re-publishes it.
 */
class ConfigRequestListenerService : WearableListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != SyncContract.REQUEST_CONFIG_PATH) return
        scope.launch {
            val store = ConfigStore(applicationContext)
            val config = store.config.first()
            if (config.apiKey.isBlank()) return@launch
            WearConfigBridge.publish(applicationContext, config)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
