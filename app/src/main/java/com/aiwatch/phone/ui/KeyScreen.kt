package com.aiwatch.phone.ui

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
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.aiwatch.phone.KeyCheck
import com.aiwatch.phone.PhoneUiState
import com.aiwatch.phone.PhoneViewModel
import com.aiwatch.phone.ui.components.Hairline
import com.aiwatch.phone.ui.components.SectionBody
import com.aiwatch.phone.ui.components.SectionCard
import com.aiwatch.phone.ui.components.SectionTitle
import com.aiwatch.phone.ui.components.StatusPill
import com.aiwatch.phone.ui.theme.LocalSemanticColors

@Composable
fun KeyScreen(state: PhoneUiState, viewModel: PhoneViewModel) {
    val semantic = LocalSemanticColors.current

    SectionCard {
        SectionTitle("API key")
        SectionBody("Create one at openrouter.ai/keys. Pasting it here saves it locally.")

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = state.draftKey,
            onValueChange = viewModel::onDraftKeyChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("sk-or-v1-…") },
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            visualTransformation = if (state.keyVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = viewModel::toggleKeyVisible) {
                    Icon(
                        imageVector = if (state.keyVisible) {
                            Icons.Outlined.VisibilityOff
                        } else {
                            Icons.Outlined.Visibility
                        },
                        contentDescription = if (state.keyVisible) "Hide key" else "Show key",
                    )
                }
            },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(),
        )

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = viewModel::saveKey,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
            ) { Text("Save") }

            OutlinedButton(
                onClick = viewModel::testKey,
                enabled = state.keyCheck !is KeyCheck.Checking,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                if (state.keyCheck is KeyCheck.Checking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (state.keyCheck is KeyCheck.Checking) "Checking" else "Test")
            }
        }

        when (val check = state.keyCheck) {
            is KeyCheck.Valid -> {
                Spacer(Modifier.height(14.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatusPill(
                        text = check.label?.takeIf { it.isNotBlank() } ?: "Key accepted",
                        tint = semantic.success,
                        icon = Icons.Outlined.CheckCircle,
                    )
                    if (check.isFreeTier != null) {
                        StatusPill(
                            text = if (check.isFreeTier) "Free tier" else "Paid tier",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                check.usage?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Spend so far: $${formatUsd(it)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            is KeyCheck.Invalid -> {
                Spacer(Modifier.height(14.dp))
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = semantic.danger,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = check.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = semantic.danger,
                    )
                }
            }

            else -> Unit
        }

        if (state.config.apiKey.isNotBlank()) {
            Spacer(Modifier.height(14.dp))
            Hairline()
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = viewModel::clearKey) {
                Text("Remove key", color = semantic.danger)
            }
        }
    }

    GenerationDefaultsCard(state = state, viewModel = viewModel)
}

@Composable
private fun GenerationDefaultsCard(state: PhoneUiState, viewModel: PhoneViewModel) {
    SectionCard {
        SectionTitle("Generation defaults")
        SectionBody("Sent with every conversation started on the watch.")

        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Temperature",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = String.format("%.2f", state.config.temperature),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = state.config.temperature,
            onValueChange = viewModel::setTemperature,
            valueRange = 0f..2f,
        )

        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Max tokens",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = state.config.maxTokens.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = state.config.maxTokens.toFloat(),
            onValueChange = { viewModel.setMaxTokens(it.toInt().coerceIn(64, 4096)) },
            valueRange = 64f..4096f,
            steps = 15,
        )

        Spacer(Modifier.height(14.dp))
        Hairline()
        Spacer(Modifier.height(14.dp))

        Text(
            text = "System prompt",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Optional. Prepended to every watch conversation.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = state.config.systemPrompt,
            onValueChange = viewModel::setSystemPrompt,
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
            placeholder = { Text("You are a concise assistant on a watch…") },
            shape = RoundedCornerShape(14.dp),
        )
    }
}

private fun formatUsd(value: Double): String =
    if (value >= 1.0) String.format("%.2f", value) else String.format("%.4f", value)
