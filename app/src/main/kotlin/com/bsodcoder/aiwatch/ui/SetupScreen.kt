package com.bsodcoder.aiwatch.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material.icons.outlined.WatchOff
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(viewModel: SetupViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        topBar = { StatusHeader(state) },
        bottomBar = {
            SendBar(
                state = state,
                onSend = viewModel::sendToWatch
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ApiKeyCard(
                    apiKey = state.apiKey,
                    onApiKeyChanged = viewModel::onApiKeyChanged
                )
            }
            item {
                ModelsCard(
                    models = state.models,
                    onAdd = viewModel::addModel,
                    onRemove = viewModel::removeModel
                )
            }
        }
    }
}

@Composable
private fun StatusHeader(state: SetupUiState) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Text(
                text = "AI Watch",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "Configure models your watch can use",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ConnectionStatusCard(
                    connected = state.watchConnected,
                    names = state.watchNames,
                    modifier = Modifier.weight(1f)
                )
                DeliveryStatusCard(
                    delivery = state.delivery,
                    errorMessage = state.errorMessage,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ConnectionStatusCard(
    connected: Boolean,
    names: List<String>,
    modifier: Modifier = Modifier
) {
    val container by animateColorAsState(
        if (connected) MaterialTheme.colorScheme.surfaceContainerLowest
        else MaterialTheme.colorScheme.errorContainer,
        label = "watch-container"
    )
    val content by animateColorAsState(
        if (connected) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onErrorContainer,
        label = "watch-content"
    )
    val pulse = rememberInfiniteTransition(label = "pulse")
    val animatedDot by pulse.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot"
    )
    val dotAlpha = if (connected) animatedDot else 1f

    StatusMiniCard(
        modifier = modifier,
        containerColor = container,
        contentColor = content,
        icon = if (connected) Icons.Outlined.Watch else Icons.Outlined.WatchOff,
        title = if (connected) "Watch connected" else "Watch disconnected",
        subtitle = when {
            connected && names.isNotEmpty() -> names.joinToString()
            connected -> "Wear OS nearby"
            else -> "Pair a watch to sync"
        },
        leading = {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        (if (connected) Color(0xFF1B8A4C) else MaterialTheme.colorScheme.error)
                            .copy(alpha = if (connected) dotAlpha else 1f)
                    )
            )
        }
    )
}

@Composable
private fun DeliveryStatusCard(
    delivery: DeliveryStatus,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val (container, content, icon) = when (delivery) {
        DeliveryStatus.Empty -> Triple(
            scheme.surfaceContainerHighest,
            scheme.onSurfaceVariant,
            Icons.Outlined.Schedule
        )
        DeliveryStatus.Partial -> Triple(
            scheme.secondaryContainer,
            scheme.onSecondaryContainer,
            Icons.Outlined.Schedule
        )
        DeliveryStatus.Ready -> Triple(
            scheme.tertiaryContainer,
            scheme.onTertiaryContainer,
            Icons.Filled.CheckCircle
        )
        DeliveryStatus.Sending -> Triple(
            scheme.primaryContainer,
            scheme.onPrimaryContainer,
            Icons.Outlined.Sync
        )
        DeliveryStatus.Success -> Triple(
            scheme.tertiaryContainer,
            scheme.onTertiaryContainer,
            Icons.Filled.CheckCircle
        )
        DeliveryStatus.Failed -> Triple(
            scheme.errorContainer,
            scheme.onErrorContainer,
            Icons.Outlined.ErrorOutline
        )
        DeliveryStatus.NotConnected -> Triple(
            scheme.errorContainer,
            scheme.onErrorContainer,
            Icons.Outlined.WatchOff
        )
    }

    val subtitle = when (delivery) {
        DeliveryStatus.Empty -> "API key and model needed"
        DeliveryStatus.Partial -> "One more field to go"
        DeliveryStatus.Ready -> "Tap send to sync"
        DeliveryStatus.Sending -> "Pushing config to watch"
        DeliveryStatus.Success -> "Watch has the new config"
        DeliveryStatus.Failed -> errorMessage ?: "Could not sync"
        DeliveryStatus.NotConnected -> "Connect a watch first"
    }

    StatusMiniCard(
        modifier = modifier,
        containerColor = container,
        contentColor = content,
        icon = icon,
        title = delivery.label,
        subtitle = subtitle,
        progress = delivery == DeliveryStatus.Sending
    )
}

@Composable
private fun StatusMiniCard(
    modifier: Modifier,
    containerColor: Color,
    contentColor: Color,
    icon: ImageVector,
    title: String,
    subtitle: String,
    leading: @Composable (() -> Unit)? = null,
    progress: Boolean = false
) {
    ElevatedCard(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leading != null) {
                    leading()
                    Spacer(Modifier.width(8.dp))
                }
                if (progress) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = contentColor
                    )
                } else {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            AnimatedContent(
                targetState = title,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                label = "status-title"
            ) { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = contentColor.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun ApiKeyCard(
    apiKey: String,
    onApiKeyChanged: (String) -> Unit
) {
    var showKey by rememberSaveable { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.Key,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "OpenRouter API key",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Stored on this phone, synced to the watch",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = apiKey,
                onValueChange = onApiKeyChanged,
                placeholder = { Text("sk-or-v1-…") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showKey = !showKey }) {
                        Icon(
                            if (showKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (showKey) "Hide key" else "Show key"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ModelsCard(
    models: List<com.bsodcoder.aiwatch.shared.ModelEntry>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    var newModelId by rememberSaveable { mutableStateOf("") }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.Memory,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Models",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        if (models.isEmpty()) "Add at least one OpenRouter model ID"
                        else "${models.size} model${if (models.size == 1) "" else "s"} on the watch",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newModelId,
                    onValueChange = { newModelId = it },
                    placeholder = { Text("deepseek/deepseek-v4-flash-0731") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                )
                FilledTonalIconButton(
                    onClick = {
                        onAdd(newModelId)
                        newModelId = ""
                    },
                    enabled = newModelId.isNotBlank()
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add model")
                }
            }
            if (models.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    models.forEach { model ->
                        InputChip(
                            selected = false,
                            onClick = { onRemove(model.id) },
                            label = {
                                Text(
                                    model.id,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove ${model.id}",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = InputChipDefaults.inputChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SendBar(
    state: SetupUiState,
    onSend: () -> Unit
) {
    val sending = state.delivery == DeliveryStatus.Sending

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (sending) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(CircleShape),
                    strokeCap = StrokeCap.Round
                )
            }
            Button(
                onClick = onSend,
                enabled = state.canSend && !sending,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors()
            ) {
                if (sending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Sending!")
                } else {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        when (state.delivery) {
                            DeliveryStatus.Success -> "Synced — send again"
                            DeliveryStatus.Failed -> "Retry send"
                            DeliveryStatus.NotConnected -> "Not connected"
                            DeliveryStatus.Empty, DeliveryStatus.Partial -> "Fill in both fields"
                            else -> "Send to Watch"
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
