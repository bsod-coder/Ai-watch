package com.aiwatch.wear.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aiwatch.wear.Screen
import com.aiwatch.wear.WearViewModel

/**
 * Root of the watch UI.
 *
 * Navigation is an explicit stack in the view model rather than the navigation
 * library, which keeps the back-swipe behaviour predictable and avoids the
 * swipe-to-dismiss/SwipeDismissableNavHost interaction that is easy to get wrong
 * on a round screen.
 */
@Composable
fun WearApp(viewModel: WearViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Enabled only when there is something to pop, so the system back-swipe
    // still exits the app from the home screen.
    BackHandler(enabled = state.canGoBack) { viewModel.back() }

    when (state.screen) {
        Screen.Home -> HomeScreen(state = state, viewModel = viewModel)
        Screen.Chat -> ChatScreen(state = state, viewModel = viewModel)
        Screen.History -> HistoryScreen(state = state, viewModel = viewModel)
    }
}
