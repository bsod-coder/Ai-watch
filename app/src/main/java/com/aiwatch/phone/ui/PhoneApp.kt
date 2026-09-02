package com.aiwatch.phone.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aiwatch.phone.PhoneUiState
import com.aiwatch.phone.PhoneViewModel
import com.aiwatch.phone.ui.components.BannerBar

private enum class Tab(val label: String) {
    Key("Key"),
    Models("Models"),
    Watch("Watch"),
}

@Composable
fun PhoneApp(viewModel: PhoneViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var selected by remember { mutableIntStateOf(Tab.Key.ordinal) }
    val tab = Tab.entries[selected]

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                Tab.entries.forEachIndexed { index, entry ->
                    NavigationBarItem(
                        selected = index == selected,
                        onClick = { selected = index },
                        icon = {
                            Icon(
                                imageVector = when (entry) {
                                    Tab.Key -> Icons.Outlined.VpnKey
                                    Tab.Models -> Icons.Outlined.List
                                    Tab.Watch -> Icons.Outlined.Watch
                                },
                                contentDescription = entry.label,
                            )
                        },
                        label = { Text(entry.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                ScreenHeader(
                    title = when (tab) {
                        Tab.Key -> "OpenRouter key"
                        Tab.Models -> "Models"
                        Tab.Watch -> "Your watch"
                    },
                    subtitle = when (tab) {
                        Tab.Key -> "Stored on this device, then pushed to the watch. " +
                            "It is only ever sent to OpenRouter."
                        Tab.Models -> "These are the models your watch will offer. " +
                            "The first one becomes the default."
                        Tab.Watch -> "Settings travel over the Wear Data Layer, so the watch " +
                            "keeps working when this app is closed."
                    },
                )

                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    when (tab) {
                        Tab.Key -> KeyScreen(state = state, viewModel = viewModel)
                        Tab.Models -> ModelsScreen(state = state, viewModel = viewModel)
                        Tab.Watch -> SyncScreen(state = state, viewModel = viewModel)
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            BannerBar(banner = state.banner, onDismiss = viewModel::dismissBanner)
        }
    }
}

@Composable
private fun ScreenHeader(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 18.dp),
    ) {
        Text(
            text = "AiWatch",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
