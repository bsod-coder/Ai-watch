package com.aiwatch.core.sync

import android.content.Context
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

/** A paired device as surfaced by the Data Layer, for display in the phone UI. */
data class PeerInfo(
    val id: String,
    val displayName: String,
    val isNearby: Boolean,
)

/** Outcome of pushing a config, so the phone can explain what happened. */
sealed interface PublishOutcome {
    data class Sent(val peerCount: Int, val modelsDropped: Int) : PublishOutcome
    data class NoPeer(val message: String) : PublishOutcome
    data class Failed(val message: String) : PublishOutcome
}

/**
 * Everything that touches the Wear Data Layer, shared by both apps.
 *
 * The phone writes a single `DataItem` at [SyncContract.CONFIG_PATH]; Google Play
 * services replicates it to the watch, which is why the watch keeps its config
 * across reboots without either app having to be running.
 */
object WearConfigBridge {

    /** Phone side: write the config and wait for the Data Layer to accept it. */
    suspend fun publish(context: Context, config: SyncConfig): PublishOutcome {
        return try {
            val fit = SyncContract.fit(config)
            val payload = SyncContract.encode(config.copy(models = fit.kept))

            val request = PutDataMapRequest.create(SyncContract.CONFIG_PATH).apply {
                dataMap.putString(SyncContract.FIELD_CONFIG, payload)
                dataMap.putInt(SyncContract.FIELD_VERSION, SyncContract.PROTOCOL_VERSION)
                dataMap.putLong(SyncContract.FIELD_UPDATED_AT, config.updatedAt)
            }

            Wearable.getDataClient(context)
                .putDataItem(request.asPutDataRequest().setUrgent())
                .await()

            val peers = connectedPeers(context)
            if (peers.isEmpty()) {
                PublishOutcome.NoPeer("Config saved, but no watch is connected right now. " +
                    "It will arrive as soon as one pairs.")
            } else {
                PublishOutcome.Sent(peerCount = peers.size, modelsDropped = fit.dropped)
            }
        } catch (t: Throwable) {
            PublishOutcome.Failed(t.message ?: "Could not reach the Data Layer")
        }
    }

    /**
     * Watch side: ask every connected phone to re-publish. Needed on first run
     * and after a watch factory reset, when the local DataItem is gone.
     *
     * @return how many phones were asked.
     */
    suspend fun requestFromPhone(context: Context): Int {
        return try {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            nodes.forEach { node ->
                runCatching {
                    Wearable.getMessageClient(context)
                        .sendMessage(node.id, SyncContract.REQUEST_CONFIG_PATH, null)
                        .await()
                }
            }
            nodes.size
        } catch (t: Throwable) {
            0
        }
    }

    suspend fun connectedPeers(context: Context): List<PeerInfo> {
        return try {
            Wearable.getNodeClient(context).connectedNodes.await().map { node ->
                PeerInfo(
                    id = node.id,
                    displayName = node.displayName,
                    isNearby = node.isNearby,
                )
            }
        } catch (t: Throwable) {
            emptyList()
        }
    }

    /** Watch side: pull a [SyncConfig] out of a replicated DataItem. */
    fun read(dataItem: DataItem): SyncConfig? {
        if (dataItem.uri?.path != SyncContract.CONFIG_PATH) return null
        val version = runCatching {
            DataMapItem.fromDataItem(dataItem).dataMap.getInt(SyncContract.FIELD_VERSION)
        }.getOrDefault(SyncContract.PROTOCOL_VERSION)
        if (version > SyncContract.PROTOCOL_VERSION) return null
        val raw = runCatching {
            DataMapItem.fromDataItem(dataItem).dataMap.getString(SyncContract.FIELD_CONFIG)
        }.getOrNull()
        return SyncContract.decode(raw)
    }
}
