package com.bsodcoder.aiwatch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bsodcoder.aiwatch.R
import com.bsodcoder.aiwatch.shared.ModelEntry

@Composable
fun SetupScreen(viewModel: SetupViewModel = viewModel()) {
    val apiKey by viewModel.apiKey.collectAsState()
    val models by viewModel.models.collectAsState()
    val sendState by viewModel.sendState.collectAsState()
    val watchLink by viewModel.watchLink.collectAsState()
    val delivery = deliveryStatus(apiKey, models, sendState, watchLink.connected)
    val canSend = delivery == DeliveryStatus.Ready ||
        delivery == DeliveryStatus.Failed ||
        delivery == DeliveryStatus.Success

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            StatusHeader(
                connected = watchLink.connected,
                watchNames = watchLink.names,
                delivery = delivery,
                errorMessage = (sendState as? SendState.Error)?.message
            )
        },
        bottomBar = {
            SendBar(
                delivery = delivery,
                enabled = canSend && delivery != DeliveryStatus.Sending,
                onSend = viewModel::sendToWatch
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ApiKeyCard(
                    apiKey = apiKey,
                    onApiKeyChanged = viewModel::onApiKeyChanged
                )
            }
            item {
                ModelsCard(
                    models = models,
                    onAdd = viewModel::addModel,
                    onRemove = viewModel::removeModel
                )
            }
        }
    }
}

@Composable
private fun StatusHeader(
    connected: Boolean,
    watchNames: List<String>,
    delivery: DeliveryStatus,
    errorMessage: String?
) {
    Surface(color = MaterialTheme.colorScheme.primary) {
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
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                text = "Configure models your watch can use",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatusMiniCard(
                    modifier = Modifier.weight(1f),
                    connectedDot = true,
                    connected = connected,
                    icon = {
                        Icon(
                            painter = painterResource(
                                if (connected) R.drawable.ic_watch else R.drawable.ic_watch_off
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    title = if (connected) "Watch connected" else "Watch disconnected",
                    subtitle = when {
                        connected && watchNames.isNotEmpty() -> watchNames.joinToString()
                        connected -> "Wear OS nearby"
                        else -> "Pair a watch to sync"
                    },
                    error = !connected
                )
                StatusMiniCard(
                    modifier = Modifier.weight(1f),
                    connectedDot = false,
                    connected = false,
                    icon = {
                        when (delivery) {
                            DeliveryStatus.Sending -> CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            DeliveryStatus.Ready, DeliveryStatus.Success -> Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            DeliveryStatus.Failed, DeliveryStatus.NotConnected -> Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            else -> Icon(
                                Icons.Default.Send,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    title = delivery.label,
                    subtitle = when (delivery) {
                        DeliveryStatus.Empty -> "API key and model needed"
                        DeliveryStatus.Partial -> "One more field to go"
                        DeliveryStatus.Ready -> "Tap send to sync"
                        DeliveryStatus.Sending -> "Pushing config to watch"
                        DeliveryStatus.Success -> "Watch has the new config"
                        DeliveryStatus.Failed -> errorMessage ?: "Could not sync"
                        DeliveryStatus.NotConnected -> "Connect a watch first"
                    },
                    error = delivery == DeliveryStatus.Failed ||
                        delivery == DeliveryStatus.NotConnected
                )
            }
        }
    }
}

@Composable
private fun StatusMiniCard(
    modifier: Modifier,
    connectedDot: Boolean,
    connected: Boolean,
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    error: Boolean
) {
    val container = if (error) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.surface
    }
    val content = if (error) {
        MaterialTheme.colorScheme.onError
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = container,
            contentColor = content
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (connectedDot) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (connected) Color(0xFF1B8A4C) else content
                            )
                    )
                    Spacer(Modifier.width(8.dp))
                }
                icon()
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = content.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun ApiKeyCard(
    apiKey: String,
    onApiKeyChanged: (String) -> Unit
) {
    var showKey by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
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
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = apiKey,
                onValueChange = onApiKeyChanged,
                placeholder = { Text("sk-or-v1-…") },
                singleLine = true,
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { showKey = !showKey }) {
                        Text(if (showKey) "Hide" else "Show")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ModelsCard(
    models: List<ModelEntry>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    var newModelId by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
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
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newModelId,
                    onValueChange = { newModelId = it },
                    placeholder = { Text("deepseek/deepseek-v4-flash-0731") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        onAdd(newModelId)
                        newModelId = ""
                    },
                    enabled = newModelId.isNotBlank()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add")
                }
            }
            Spacer(Modifier.height(12.dp))
            if (models.isEmpty()) {
                Text(
                    "No models added yet",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                models.forEach { model ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            model.id,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        IconButton(onClick = { onRemove(model.id) }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove")
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SendBar(
    delivery: DeliveryStatus,
    enabled: Boolean,
    onSend: () -> Unit
) {
    val sending = delivery == DeliveryStatus.Sending
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Button(
                onClick = onSend,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
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
                        when (delivery) {
                            DeliveryStatus.Success -> "Synced — send again"
                            DeliveryStatus.Failed -> "Retry send"
                            DeliveryStatus.NotConnected -> "Not connected"
                            DeliveryStatus.Empty, DeliveryStatus.Partial -> "Fill in both fields"
                            else -> "Send to Watch"
                        }
                    )
                }
            }
        }
    }
}
