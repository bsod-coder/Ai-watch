package com.aiwatch.wear.ui

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.TimeText
import com.aiwatch.wear.WearUiState
import com.aiwatch.wear.WearViewModel
import com.aiwatch.wear.ui.components.RoundIconButton
import com.aiwatch.wear.ui.theme.WearPalette
import com.aiwatch.wear.ui.theme.WearType

@Composable
fun ChatScreen(state: WearUiState, viewModel: WearViewModel) {
    val conversation = state.activeConversation
    val context = LocalContext.current

    var typing by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val heard = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!heard.isNullOrBlank()) viewModel.send(heard)
        }
    }

    val speechIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
        }
    }
    // Needs the <queries> entry in the manifest to resolve on Android 11+.
    val speechAvailable = remember {
        speechIntent.resolveActivity(context.packageManager) != null
    }

    LaunchedEffect(typing) {
        if (typing) focusRequester.requestFocus()
    }

    Scaffold(timeText = { TimeText() }) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (conversation != null) {
                ConversationHeader(
                    modelLabel = conversation.modelLabel,
                    turnCount = conversation.turnCount,
                    onDelete = { viewModel.deleteConversation(conversation.id) },
                )
            }

            state.error?.let { message ->
                ErrorRow(
                    message = message,
                    onDismiss = viewModel::dismissError,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }

            ChatList(state = state, modifier = Modifier.weight(1f))

            if (typing) {
                InputRow(
                    input = state.input,
                    focusRequester = focusRequester,
                    onInputChange = viewModel::onInputChange,
                    onSend = {
                        viewModel.send(state.input)
                        typing = false
                    },
                    onClose = { typing = false },
                )
            } else {
                ComposerHint(
                    onSpeak = { speechLauncher.launch(speechIntent) },
                    onType = { typing = true },
                    speakEnabled = speechAvailable,
                    onStop = viewModel::stopStreaming,
                    isStreaming = state.isStreaming,
                )
            }
        }
    }
}

@Composable
private fun InputRow(
    input: String,
    focusRequester: FocusRequester,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = input,
            onValueChange = onInputChange,
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(50))
                .background(WearPalette.Surface)
                .padding(horizontal = 14.dp, vertical = 11.dp)
                .focusRequester(focusRequester),
            textStyle = WearType.Body.copy(color = WearPalette.OnSurface),
            cursorBrush = SolidColor(WearPalette.Accent),
            singleLine = false,
            maxLines = 3,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            decorationBox = { innerTextField ->
                if (input.isEmpty()) {
                    androidx.wear.compose.material.Text(
                        text = "Ask anything",
                        style = WearType.Body,
                        color = WearPalette.OnSurfaceMuted,
                    )
                }
                innerTextField()
            },
        )
        Spacer(Modifier.width(10.dp))
        RoundIconButton(
            icon = Icons.AutoMirrored.Outlined.Send,
            contentDescription = "Send",
            onClick = onSend,
            enabled = input.isNotBlank(),
            filled = true,
        )
        Spacer(Modifier.width(6.dp))
        RoundIconButton(
            icon = Icons.Outlined.Close,
            contentDescription = "Close keyboard",
            onClick = onClose,
            filled = false,
            tint = WearPalette.OnSurfaceMuted,
        )
    }
}
