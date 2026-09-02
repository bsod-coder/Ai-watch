package com.aiwatch.phone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aiwatch.core.model.ModelEntry
import com.aiwatch.core.net.CatalogModel
import com.aiwatch.phone.CatalogState
import com.aiwatch.phone.PhoneUiState
import com.aiwatch.phone.PhoneViewModel
import com.aiwatch.phone.ui.components.EmptyState
import com.aiwatch.phone.ui.components.Hairline
import com.aiwatch.phone.ui.components.SectionBody
import com.aiwatch.phone.ui.components.SectionCard
import com.aiwatch.phone.ui.components.SectionTitle
import com.aiwatch.phone.ui.components.StatusPill
import com.aiwatch.phone.ui.theme.LocalSemanticColors

@Composable
fun ModelsScreen(state: PhoneUiState, viewModel: PhoneViewModel) {
    var draft by remember { mutableStateOf("") }
    var showCatalog by remember { mutableStateOf(false) }

    SectionCard {
        SectionTitle("Add a model")
        SectionBody("Paste an OpenRouter slug, or pull the live catalogue.")

        Spacer(Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("deepseek/deepseek-v4-flash-0731") },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                shape = RoundedCornerShape(14.dp),
            )
            Button(
                onClick = {
                    viewModel.addModelById(draft)
                    draft = ""
                },
                enabled = draft.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
            ) { Text("Add") }
        }

        Spacer(Modifier.height(10.dp))

        TextButton(onClick = {
            showCatalog = true
            if (state.catalog is CatalogState.Idle) viewModel.loadCatalog()
        }) {
            Icon(
                imageVector = Icons.Outlined.CloudDownload,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text("Browse the OpenRouter catalogue")
        }
    }

    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionTitle("On your watch")
            Spacer(Modifier.weight(1f))
            StatusPill(
                text = "${state.config.models.size}",
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        if (state.config.models.isEmpty()) {
            EmptyState(
                title = "No models yet",
                body = "The watch needs at least one model to talk to.",
            )
        } else {
            Spacer(Modifier.height(14.dp))
            state.config.models.forEachIndexed { index, entry ->
                if (index > 0) {
                    Hairline(Modifier.padding(vertical = 4.dp))
                }
                ModelRow(
                    entry = entry,
                    isDefault = index == 0,
                    isFirst = index == 0,
                    isLast = index == state.config.models.lastIndex,
                    onUp = { viewModel.moveModel(entry.id, -1) },
                    onDown = { viewModel.moveModel(entry.id, 1) },
                    onDelete = { viewModel.removeModel(entry.id) },
                )
            }
        }
    }

    if (showCatalog) {
        CatalogDialog(
            catalog = state.catalog,
            selected = state.config.models.map { it.id }.toSet(),
            onSearch = viewModel::loadCatalog,
            onPick = { viewModel.addFromCatalog(it) },
            onDismiss = {
                showCatalog = false
                viewModel.closeCatalog()
            },
        )
    }
}

@Composable
private fun ModelRow(
    entry: ModelEntry,
    isDefault: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onDelete: () -> Unit,
) {
    val semantic = LocalSemanticColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = entry.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (isDefault) {
                    StatusPill(text = "Default", tint = semantic.success, icon = Icons.Outlined.Star)
                }
            }
            if (entry.id != entry.label) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = entry.id,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            entry.contextLength?.let {
                Text(
                    text = "${it / 1000}k context",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        IconButton(onClick = onUp, enabled = !isFirst) {
            Icon(Icons.Outlined.ArrowUpward, contentDescription = "Move up", modifier = Modifier.size(18.dp))
        }
        IconButton(onClick = onDown, enabled = !isLast) {
            Icon(Icons.Outlined.ArrowDownward, contentDescription = "Move down", modifier = Modifier.size(18.dp))
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = "Remove",
                tint = semantic.danger,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun CatalogDialog(
    catalog: CatalogState,
    selected: Set<String>,
    onSearch: () -> Unit,
    onPick: (CatalogModel) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(22.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("OpenRouter catalogue") },
        text = {
            Column(modifier = Modifier.height(420.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Search models") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    shape = RoundedCornerShape(14.dp),
                )

                Spacer(Modifier.height(12.dp))

                when (catalog) {
                    is CatalogState.Loading -> Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }

                    is CatalogState.Error -> EmptyState(
                        title = "Could not load",
                        body = catalog.message,
                    )

                    is CatalogState.Idle -> EmptyState(
                        title = "Nothing loaded",
                        body = "Save a key, then retry.",
                    )

                    is CatalogState.Ready -> {
                        val filtered = remember(query, catalog.models) {
                            if (query.isBlank()) {
                                catalog.models
                            } else {
                                catalog.models.filter {
                                    it.id.contains(query, ignoreCase = true) ||
                                        it.name.orEmpty().contains(query, ignoreCase = true)
                                }
                            }
                        }
                        LazyColumn {
                            items(filtered, key = { it.id }) { model ->
                                CatalogRow(
                                    model = model,
                                    isSelected = model.id in selected,
                                    onPick = { onPick(model) },
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSearch) { Text("Reload") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
    )
}

@Composable
private fun CatalogRow(
    model: CatalogModel,
    isSelected: Boolean,
    onPick: () -> Unit,
) {
    val semantic = LocalSemanticColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isSelected, onClick = onPick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = model.name?.takeIf { it.isNotBlank() } ?: model.id,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = model.id,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (isSelected) {
            StatusPill(text = "Added", tint = semantic.success)
        } else if (model.isFree) {
            StatusPill(text = "Free", tint = MaterialTheme.colorScheme.primary)
        }
    }
}
