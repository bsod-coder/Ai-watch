package com.aiwatch.wear.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import com.aiwatch.core.chat.ChatTurn
import com.aiwatch.wear.WearUiState
import com.aiwatch.wear.ui.components.RoundIconButton
import com.aiwatch.wear.ui.components.WatchDivider
import com.aiwatch.wear.ui.components.WatchEmpty
import com.aiwatch.wear.ui.components.WatchPill
import com.aiwatch.wear.ui.theme.WearPalette
import com.aiwatch.wear.ui.theme.WearType

/**
 * Small indeterminate arc.
 *
 * Drawn by hand rather than using Wear's CircularProgressIndicator so the chat
 * does not depend on which overloads that widget happens to expose.
 */
@Composable
fun Spinner(
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = WearPalette.Accent,
    size: Int = 12,
) {
    val transition = rememberInfiniteTransition(label = "aiwatch-spinner")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "angle",
    )

    Canvas(modifier = modifier.size(size.dp)) {
        val stroke = 2.dp.toPx()
        drawArc(
            color = color,
            startAngle = angle,
            sweepAngle = 250f,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
            size = Size(this.size.width - stroke, this.size.height - stroke),
            topLeft = androidx.compose.ui.geometry.Offset(stroke / 2f, stroke / 2f),
        )
    }
}

/** One message bubble. Parameter-driven so only the changed one recomposes. */
@Composable
fun MessageBubble(
    text: String,
    isUser: Boolean,
    modifier: Modifier = Modifier,
    pending: Boolean = false,
) {
    val background = if (isUser) WearPalette.UserBubble else WearPalette.AssistantBubble

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .clip(RoundedCornerShape(16.dp))
                .background(background)
                .padding(horizontal = 12.dp, vertical = 9.dp),
        ) {
            Text(
                text = text,
                style = WearType.Body,
                color = WearPalette.OnSurface,
            )
            if (pending) {
                Spacer(Modifier.height(6.dp))
                Spinner(size = 11)
            }
        }
    }
}

@Composable
fun StreamingIndicator() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(WearPalette.AssistantBubble)
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spinner()
            Spacer(Modifier.width(9.dp))
            Text(
                text = "Thinking…",
                style = WearType.BodySmall,
                color = WearPalette.OnSurfaceMuted,
            )
        }
    }
}

@Composable
fun ErrorRow(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(WearPalette.Danger.copy(alpha = 0.14f))
            .padding(start = 12.dp, top = 9.dp, bottom = 9.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            style = WearType.BodySmall,
            color = WearPalette.Danger,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Dismiss error",
                tint = WearPalette.Danger,
                modifier = Modifier.size(15.dp),
            )
        }
    }
}

@Composable
fun ConversationHeader(
    modelLabel: String,
    turnCount: Int,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WatchPill(
                text = modelLabel,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (turnCount == 1) "1 turn" else "$turnCount turns",
                style = WearType.BodySmall,
                color = WearPalette.OnSurfaceMuted,
            )
            Spacer(Modifier.width(4.dp))
            RoundIconButton(
                icon = Icons.Outlined.Delete,
                contentDescription = "Delete conversation",
                onClick = onDelete,
                tint = WearPalette.Danger,
            )
        }
        Spacer(Modifier.height(6.dp))
        WatchDivider(Modifier.padding(horizontal = 20.dp))
    }
}

@Composable
fun ComposerHint(
    onSpeak: () -> Unit,
    onType: () -> Unit,
    speakEnabled: Boolean,
    onStop: () -> Unit,
    isStreaming: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (isStreaming) {
            RoundIconButton(
                icon = Icons.Outlined.Stop,
                contentDescription = "Stop generating",
                onClick = onStop,
                filled = true,
                tint = WearPalette.Danger,
            )
        } else {
            RoundIconButton(
                icon = Icons.Outlined.Mic,
                contentDescription = "Speak",
                onClick = onSpeak,
                enabled = speakEnabled,
                filled = true,
            )
            Spacer(Modifier.width(14.dp))
            RoundIconButton(
                icon = Icons.Outlined.Keyboard,
                contentDescription = "Type",
                onClick = onType,
            )
        }
    }
}

@Composable
fun SendRow(
    input: String,
    onSend: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(50))
                .background(WearPalette.Surface)
                .padding(horizontal = 14.dp, vertical = 11.dp),
        ) {
            Text(
                text = input.ifBlank { "Ask anything" },
                style = WearType.Body,
                color = if (input.isBlank()) {
                    WearPalette.OnSurfaceMuted
                } else {
                    WearPalette.OnSurface
                },
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(10.dp))
        RoundIconButton(
            icon = Icons.AutoMirrored.Outlined.Send,
            contentDescription = "Send",
            onClick = onSend,
            enabled = input.isNotBlank(),
            filled = true,
        )
    }
}

/** The scrolling transcript. Newest entry sits at the bottom. */
@Composable
fun ChatList(
    state: WearUiState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
) {
    val turns = state.activeConversation?.turns.orEmpty()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        reverseLayout = true,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (turns.isEmpty() && !state.isStreaming) {
            item(key = "empty") {
                WatchEmpty(
                    title = "Say something",
                    body = "Tap the microphone, or the keyboard to type.",
                )
            }
        }

        // reverseLayout lays index 0 out at the bottom, so walk backwards.
        for (index in turns.indices.reversed()) {
            val turn: ChatTurn = turns[index]
            item(key = "turn-$index-${turn.createdAt}") {
                MessageBubble(text = turn.content, isUser = turn.isUser)
            }
        }

        if (state.isStreaming) {
            item(key = "streaming") {
                if (state.streamingText.isBlank()) {
                    StreamingIndicator()
                } else {
                    MessageBubble(
                        text = state.streamingText,
                        isUser = false,
                        pending = true,
                    )
                }
            }
        }
    }
}
