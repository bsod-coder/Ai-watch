package com.bsodcoder.aiwatch.wear.ui

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
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.bsodcoder.aiwatch.wear.data.ChatEntity

@Composable
fun ChatListScreen(
    viewModel: ChatViewModel,
    onOpenChat: (Long) -> Unit,
    onNewChat: () -> Unit
) {
    val chats by viewModel.chats.collectAsState()
    val models by viewModel.availableModels.collectAsState()
    val listState = rememberScalingLazyListState()

    ScreenScaffold(scrollState = listState) { contentPadding ->
        ScalingLazyColumn(
            modifier = Modifier.fillMaxWidth(),
            state = listState,
            contentPadding = contentPadding
        ) {
            item {
                ListHeader { Text("Chats") }
            }

            item {
                Button(
                    onClick = onNewChat,
                    enabled = models.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("New chat")
                }
            }

            if (models.isEmpty()) {
                item {
                    Text(
                        text = "No models yet. Add one in the phone app and send it to your watch.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            if (chats.isEmpty()) {
                item {
                    Text(
                        text = "No conversations yet",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            } else {
                items(chats, key = { it.id }) { chat: ChatEntity ->
                    FilledTonalButton(
                        onClick = { onOpenChat(chat.id) },
                        colors = ButtonDefaults.filledTonalButtonColors(),
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text(chat.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        secondaryLabel = {
                            Text(chat.modelId, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    )
                }
            }
        }
    }
}
