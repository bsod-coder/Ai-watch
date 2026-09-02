package com.bsodcoder.aiwatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.bsodcoder.aiwatch.ui.AiWatchTheme
import com.bsodcoder.aiwatch.ui.SendState
import com.bsodcoder.aiwatch.ui.SetupViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AiWatchTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    SetupScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(viewModel: SetupViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val apiKey by viewModel.apiKey.collectAsState()
    val models by viewModel.models.collectAsState()
    val sendState by viewModel.sendState.collectAsState()

    var newModelId by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            text = "AI Watch",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Configure the models your watch can use",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        Text("OpenRouter API key", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = apiKey,
            onValueChange = viewModel::onApiKeyChanged,
            placeholder = { Text("sk-or-v1-...") },
            singleLine = true,
            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = { showKey = !showKey }) {
                    Text(if (showKey) "Hide" else "Show")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(28.dp))
        Text("Models", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newModelId,
                onValueChange = { newModelId = it },
                placeholder = { Text("deepseek/deepseek-v4-flash-0731") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                viewModel.addModel(newModelId)
                newModelId = ""
            }) {
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
            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                items(models, key = { it.id }) { model ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(model.id, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        IconButton(onClick = { viewModel.removeModel(model.id) }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove")
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = viewModel::sendToWatch,
            enabled = sendState != SendState.Sending,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (sendState == SendState.Sending) "Sending..." else "Send to Watch")
        }

        Spacer(Modifier.height(8.dp))
        when (val state = sendState) {
            is SendState.Success -> Text("Synced to watch", color = MaterialTheme.colorScheme.primary)
            is SendState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error)
            else -> {}
        }
    }
}
