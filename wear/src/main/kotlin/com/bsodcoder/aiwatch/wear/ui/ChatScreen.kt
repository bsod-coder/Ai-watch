package com.bsodcoder.aiwatch.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import com.bsodcoder.aiwatch.wear.data.MessageEntity

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    chatId: Long
) {
    val messages by viewModel.messagesFor(chatId).collectAsState()
    val sendState by viewModel.sendState.collectAsState()
    var showInput by remember { mutableStateOf(false) }
    val listState: ScalingLazyListState = rememberScalingLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex + 1)
        }
    }

    if (showInput) {
        MessageInput(
            onDismiss = { showInput = false },
            onSend = { text ->
                viewModel.sendMessage(chatId, text)
                showInput = false
            }
        )
        return
    }

    ScalingLazyColumn(modifier = Modifier.fillMaxWidth(), state = listState) {
        item { Spacer(modifier = Modifier.height(4.dp)) }
        items(messages, key = { it.id }) { message: MessageEntity ->
            MessageBubble(message)
        }
        item {
            Chip(
                label = { Text(if (sendState is SendState.Sending) "Sending…" else "Reply") },
                onClick = { showInput = true },
                enabled = sendState !is SendState.Sending,
                colors = ChipDefaults.primaryChipColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun MessageBubble(message: MessageEntity) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            onClick = {},
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.body2,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

/**
 * Simple in-app text entry. Wear OS will surface its own on-screen
 * keyboard / voice-to-text / handwriting chooser automatically when
 * this field is focused, so no extra wiring is required for basic use.
 */
@Composable
private fun MessageInput(
    onDismiss: () -> Unit,
    onSend: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Text(
            text = "Message",
            style = MaterialTheme.typography.caption1,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            textStyle = TextStyle(color = MaterialTheme.colors.onSurface, fontSize = MaterialTheme.typography.body2.fontSize),
            cursorBrush = SolidColor(MaterialTheme.colors.primary),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colors.surface)
                .padding(10.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Chip(label = { Text("Cancel") }, onClick = onDismiss, colors = ChipDefaults.secondaryChipColors())
            Chip(
                label = { Text("Send") },
                onClick = { onSend(text) },
                enabled = text.isNotBlank(),
                colors = ChipDefaults.primaryChipColors()
            )
        }
    }
}
