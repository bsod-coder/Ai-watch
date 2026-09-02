package com.bsodcoder.aiwatch.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.bsodcoder.aiwatch.wear.ui.AiWatchWearTheme
import com.bsodcoder.aiwatch.wear.ui.ChatListScreen
import com.bsodcoder.aiwatch.wear.ui.ChatScreen
import com.bsodcoder.aiwatch.wear.ui.ChatViewModel
import com.bsodcoder.aiwatch.wear.ui.ModelPickerScreen

private object Routes {
    const val CHAT_LIST = "chatList"
    const val MODEL_PICKER = "modelPicker"
    const val CHAT = "chat/{chatId}"
    fun chat(chatId: Long) = "chat/$chatId"
}

class WearMainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AiWatchWearTheme {
                AiWatchApp(viewModel)
            }
        }
    }
}

@Composable
private fun AiWatchApp(viewModel: ChatViewModel) {
    val navController: NavHostController = rememberSwipeDismissableNavController()

    Scaffold {
        SwipeDismissableNavHost(
            navController = navController,
            startDestination = Routes.CHAT_LIST,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(Routes.CHAT_LIST) {
                ChatListScreen(
                    viewModel = viewModel,
                    onOpenChat = { chatId -> navController.navigate(Routes.chat(chatId)) },
                    onNewChat = { navController.navigate(Routes.MODEL_PICKER) }
                )
            }
            composable(Routes.MODEL_PICKER) {
                ModelPickerScreen(
                    viewModel = viewModel,
                    onModelChosen = { chatId ->
                        navController.navigate(Routes.chat(chatId)) {
                            popUpTo(Routes.CHAT_LIST)
                        }
                    }
                )
            }
            composable(Routes.CHAT) { backStackEntry ->
                val chatId = backStackEntry.arguments?.getString("chatId")?.toLongOrNull() ?: return@composable
                ChatScreen(viewModel = viewModel, chatId = chatId)
            }
        }
    }
}
