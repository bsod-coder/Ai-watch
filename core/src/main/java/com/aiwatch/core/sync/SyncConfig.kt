package com.aiwatch.core.sync

import com.aiwatch.core.model.ModelEntry
import kotlinx.serialization.Serializable

/**
 * Everything the phone hands to the watch. This is the single object that
 * crosses the Data Layer boundary, so adding a field here is the only change
 * needed to ship a new setting to the watch.
 */
@Serializable
data class SyncConfig(
    val apiKey: String = "",
    val models: List<ModelEntry> = emptyList(),
    val defaultModelId: String = "",
    val temperature: Float = 0.7f,
    val maxTokens: Int = 512,
    val systemPrompt: String = "",
    val updatedAt: Long = 0L,
) {
    /** True once the watch has both a key and at least one model to talk to. */
    val isReady: Boolean
        get() = apiKey.isNotBlank() && models.isNotEmpty()

    val preferredModel: ModelEntry?
        get() = models.firstOrNull { it.id == defaultModelId } ?: models.firstOrNull()

    fun withModels(next: List<ModelEntry>): SyncConfig = copy(
        models = next,
        defaultModelId = if (next.any { it.id == defaultModelId }) defaultModelId else next.firstOrNull()?.id.orEmpty(),
    )
}
