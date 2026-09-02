package com.aiwatch.phone.ui

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aiwatch.phone.PhoneUiState
import com.aiwatch.phone.PhoneViewModel
import com.aiwatch.phone.ui.components.EmptyState
import com.aiwatch.phone.ui.components.Hairline
import com.aiwatch.phone.ui.components.SectionBody
import com.aiwatch.phone.ui.components.SectionCard
import com.aiwatch.phone.ui.components.SectionTitle
import com.aiwatch.phone.ui.components.StatusPill
import com.aiwatch.phone.ui.theme.LocalSemanticColors

@Composable
fun SyncScreen(state: PhoneUiState, viewModel: PhoneViewModel) {
    val semantic = LocalSemanticColors.current
    val context = LocalContext.current

    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                SectionTitle(
                    if (state.peers.isEmpty()) "No watch connected" else "Connected",
                )
                SectionBody(
                    if (state.peers.isEmpty()) {
                        "Pair the watch in the Wear OS app. The config is stored here " +
                            "until a watch shows up."
                    } else {
                        "Settings replicate automatically; the watch keeps them after a reboot."
                    },
                )
            }
            Spacer(Modifier.width(12.dp))
            StatusPill(
                text = if (state.peers.isEmpty()) "Offline" else "${state.peers.size}",
                tint = if (state.peers.isEmpty()) semantic.warning else semantic.success,
                icon = Icons.Outlined.Watch,
            )
        }

        if (state.peers.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Hairline()
            state.peers.forEachIndexed { index, peer ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Watch,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = peer.displayName.ifBlank { "Watch ${index + 1}" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = if (peer.isNearby) "Nearby" else "Connected remotely",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (index != state.peers.lastIndex) Hairline()
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = viewModel::syncToWatch,
                enabled = !state.isSyncing,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                if (state.isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Sending…")
                } else {
                    Icon(
                        imageVector = Icons.Outlined.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Send to watch")
                }
            }

            OutlinedButton(
                onClick = viewModel::refreshPeers,
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Outlined.Refresh, contentDescription = "Refresh devices")
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            text = if (state.lastSyncedAt > 0L) {
                "Last synced " + DateUtils.getRelativeTimeSpanString(
                    state.lastSyncedAt,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                )
            } else {
                "Never synced"
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    SectionCard {
        SectionTitle("Checklist")
        Spacer(Modifier.height(14.dp))
        CheckRow("API key set", state.config.apiKey.isNotBlank())
        Hairline(Modifier.padding(vertical = 4.dp))
        CheckRow("At least one model", state.config.models.isNotEmpty())
        Hairline(Modifier.padding(vertical = 4.dp))
        CheckRow("Watch reachable", state.peers.isNotEmpty())

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = viewModel::refreshPeers) { Text("Re-check") }
    }

    SectionCard {
        SectionTitle("How the watch uses this")
        SectionBody(
            "The watch calls OpenRouter itself over Wi-Fi or LTE, so replies do not " +
                "wait on your phone. Your key is stored in the watch's private app " +
                "storage and is only ever sent to openrouter.ai.",
        )
    }
}

@Composable
private fun CheckRow(label: String, done: Boolean) {
    val semantic = LocalSemanticColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (done) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (done) semantic.success else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (done) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}
