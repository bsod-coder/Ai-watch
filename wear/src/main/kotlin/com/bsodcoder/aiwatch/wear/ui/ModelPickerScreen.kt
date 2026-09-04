package com.bsodcoder.aiwatch.wear.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.bsodcoder.aiwatch.shared.ModelEntry

@Composable
fun ModelPickerScreen(
    viewModel: ChatViewModel,
    onModelChosen: (Long) -> Unit
) {
    val models by viewModel.availableModels.collectAsState()
    val listState = rememberScalingLazyListState()

    ScreenScaffold(scrollState = listState) { contentPadding ->
        ScalingLazyColumn(
            modifier = Modifier.fillMaxWidth(),
            state = listState,
            contentPadding = contentPadding
        ) {
            item {
                ListHeader { Text("Choose a model") }
            }
            items(models, key = { it.id }) { model: ModelEntry ->
                FilledTonalButton(
                    onClick = {
                        viewModel.createChat(model) { chatId -> onModelChosen(chatId) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(model.id, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                )
            }
        }
    }
}
