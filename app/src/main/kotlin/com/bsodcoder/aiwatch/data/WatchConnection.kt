package com.bsodcoder.aiwatch.data

import android.content.Context
import com.bsodcoder.aiwatch.shared.AiWatchPaths
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class WatchLinkState(
    val connected: Boolean = false,
    val names: List<String> = emptyList()
)

/**
 * Live Wear OS node presence. Combines [NodeClient.connectedNodes] with a
 * capability listener so the phone UI updates when a watch pairs, unpairs,
 * or drops Bluetooth.
 */
object WatchConnection {

    fun observe(context: Context): Flow<WatchLinkState> = callbackFlow {
        val appContext = context.applicationContext
        val nodeClient = Wearable.getNodeClient(appContext)
        val capabilityClient = Wearable.getCapabilityClient(appContext)

        suspend fun refresh() {
            val nodes = runCatching { nodeClient.connectedNodes.await() }
                .getOrDefault(emptyList())
            trySend(nodes.toLinkState())
        }

        val listener = CapabilityClient.OnCapabilityChangedListener {
            launch { refresh() }
        }

        runCatching {
            capabilityClient.addListener(listener, AiWatchPaths.CAPABILITY)
        }

        refresh()

        val poll = launch {
            while (isActive) {
                delay(3_000)
                refresh()
            }
        }

        awaitClose {
            poll.cancel()
            runCatching { capabilityClient.removeListener(listener) }
        }
    }.distinctUntilChanged()

    suspend fun current(context: Context): WatchLinkState {
        val nodes = runCatching {
            Wearable.getNodeClient(context).connectedNodes.await()
        }.getOrDefault(emptyList())
        return nodes.toLinkState()
    }

    private fun List<Node>.toLinkState(): WatchLinkState = WatchLinkState(
        connected = isNotEmpty(),
        names = map { it.displayName }.distinct()
    )
}
