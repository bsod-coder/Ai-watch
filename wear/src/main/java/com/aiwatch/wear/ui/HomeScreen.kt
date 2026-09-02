package com.aiwatch.wear.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.ScalingLazyListState
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import androidx.wear.compose.material.rememberScalingLazyListState
import com.aiwatch.wear.Screen
import com.aiwatch.wear.WearUiState
import com.aiwatch.wear.WearViewModel
import com.aiwatch.wear.ui.components.RoundIconButton
import com.aiwatch.wear.ui.components.WatchEmpty
import com.aiwatch.wear.ui.components.WatchHeading
import com.aiwatch.wear.ui.components.WatchRow
import com.aiwatch.wear.ui.theme.WearPalette

@Composable
fun HomeScreen(state: WearUiState, viewModel: WearViewModel) {
    val listState: ScalingLazyListState = rememberScalingLazyListState()

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
            item { WatchHeading("New chat") }

            when {
                !state.configLoaded -> {
                    item {
                        WatchEmpty(
                            title = "Loading",
                            body = "Reading the config sent from your phone.",
                        )
                    }
                }

                !state.isConfigured -> {
                    item { SetupHint(state = state, viewModel = viewModel) }
                }

                else -> {
                    state.config.models.forEach { model ->
                        item {
                            WatchRow(
                                title = model.label,
                                subtitle = model.shortId,
                                icon = Icons.Outlined.Chat,
                                onClick = { viewModel.startChat(model) },
                            )
                        }
                    }

                    item { Spacer(Modifier.height(8.dp)) }

                    item {
                        WatchRow(
                            title = "History",
                            subtitle = "${state.conversations.size} saved",
                            icon = Icons.Outlined.History,
                            tint = WearPalette.OnSurfaceMuted,
                            onClick = { viewModel.goTo(Screen.History) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupHint(state: WearUiState, viewModel: WearViewModel) {
    WatchEmpty(
        title = if (state.config.apiKey.isBlank()) "Not set up yet" else "No models",
        body = "Open AiWatch on your phone, add a key and some models, " +
            "then tap Send to watch.",
        action = {
            RoundIconButton(
                icon = if (state.isRequestingConfig) {
                    Icons.Outlined.PhoneAndroid
                } else {
                    Icons.Outlined.Refresh
                },
                contentDescription = "Ask the phone for the config",
                enabled = !state.isRequestingConfig,
                filled = true,
                onClick = viewModel::requestConfig,
            )
        },
    )
    if (state.isRequestingConfig) {
        WatchEmpty(
            title = "Asking your phone…",
            body = "Keep the two devices near each other.",
        )
    }
}
