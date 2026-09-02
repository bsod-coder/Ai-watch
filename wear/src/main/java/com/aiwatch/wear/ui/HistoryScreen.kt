package com.aiwatch.wear.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import androidx.wear.compose.material.rememberScalingLazyListState
import com.aiwatch.core.chat.Conversation
import com.aiwatch.wear.WearUiState
import com.aiwatch.wear.WearViewModel
import com.aiwatch.wear.ui.components.RoundIconButton
import com.aiwatch.wear.ui.components.WatchEmpty
import com.aiwatch.wear.ui.components.WatchHeading
import com.aiwatch.wear.ui.components.WatchRow
import com.aiwatch.wear.ui.theme.WearPalette
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryScreen(state: WearUiState, viewModel: WearViewModel) {
    val listState = rememberScalingLazyListState()

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(start = 10.dp, top = 36.dp, end = 10.dp, bottom = 28.dp),
        ) {
            item { WatchHeading("History") }

            if (state.conversations.isEmpty()) {
                item {
                    WatchEmpty(
                        title = "Nothing saved",
                        body = "Conversations you start will collect here.",
                    )
                }
            } else {
                state.conversations.forEach { conversation ->
                    item {
                        HistoryRow(
                            conversation = conversation,
                            onOpen = { viewModel.openConversation(conversation.id) },
                            onDelete = { viewModel.deleteConversation(conversation.id) },
                        )
                    }
                }

                item { Spacer(Modifier.height(8.dp)) }

                item {
                    WatchRow(
                        title = "Clear all",
                        subtitle = "${state.conversations.size} conversations",
                        icon = Icons.Outlined.DeleteSweep,
                        tint = WearPalette.Danger,
                        onClick = viewModel::clearHistory,
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    conversation: Conversation,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val stamp = remember(conversation.updatedAt) {
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
            .format(Date(conversation.updatedAt))
    }

    WatchRow(
        title = conversation.title,
        subtitle = buildString {
            append(conversation.modelLabel)
            append(" · ")
            append(stamp)
            conversation.lastMessage?.let {
                append("\n")
                append(it)
            }
        },
        icon = Icons.Outlined.Chat,
        onClick = onOpen,
        trailing = {
            RoundIconButton(
                icon = Icons.Outlined.Delete,
                contentDescription = "Delete conversation",
                onClick = onDelete,
                tint = WearPalette.OnSurfaceMuted,
            )
        },
    )
}
