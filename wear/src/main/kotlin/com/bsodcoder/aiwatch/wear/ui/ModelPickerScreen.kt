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
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.bsodcoder.aiwatch.shared.ModelEntry

@Composable
fun ModelPickerScreen(
    viewModel: ChatViewModel,
    onModelChosen: (Long) -> Unit
) {
    val models by viewModel.availableModels.collectAsState()

    ScalingLazyColumn(modifier = Modifier.fillMaxWidth()) {
        item {
            Text(
                text = "Choose a model",
                style = MaterialTheme.typography.title3,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        items(models, key = { it.id }) { model: ModelEntry ->
            Chip(
                label = { Text(model.id, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                onClick = {
                    viewModel.createChat(model) { chatId -> onModelChosen(chatId) }
                },
                colors = ChipDefaults.secondaryChipColors(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
