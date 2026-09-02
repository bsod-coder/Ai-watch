package com.bsodcoder.aiwatch.wear.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.*
import com.bsodcoder.aiwatch.wear.data.ChatEntity

@Composable
fun ChatListScreen(
    viewModel: ChatViewModel,
    onOpenChat: (Long) -> Unit,
    onNewChat: () -> Unit
) {
    val chats by viewModel.chats.collectAsState()
    val models by viewModel.availableModels.collectAsState()

    ScalingLazyColumn(modifier = Modifier.fillMaxWidth()) {
        item {
            Text(
                text = "Chats",
                style = MaterialTheme.typography.title3,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        item {
            Chip(
                label = { Text("New chat") },
                onClick = onNewChat,
                enabled = models.isNotEmpty(),
                colors = ChipDefaults.primaryChipColors(),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (models.isEmpty()) {
            item {
                Text(
                    text = "No models yet. Add one in the phone app and send it to your watch.",
                    style = MaterialTheme.typography.caption2,
                    color = MaterialTheme.colors.onSurface,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        if (chats.isEmpty()) {
            item {
                Text(
                    text = "No conversations yet",
                    style = MaterialTheme.typography.caption1,
                    color = MaterialTheme.colors.onSurface,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        } else {
            items(chats, key = { it.id }) { chat: ChatEntity ->
                Chip(
                    label = {
                        Text(chat.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    secondaryLabel = {
                        Text(chat.modelId, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    onClick = { onOpenChat(chat.id) },
                    colors = ChipDefaults.secondaryChipColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
