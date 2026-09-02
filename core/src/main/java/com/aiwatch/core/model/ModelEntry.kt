package com.aiwatch.core.model

import kotlinx.serialization.Serializable

/**
 * One selectable model, as configured on the phone and mirrored to the watch.
 *
 * [id] is the OpenRouter slug, e.g. `deepseek/deepseek-v4-flash-0731`.
 * [label] is what the human sees; it defaults to the id when the user typed the
 * model in by hand rather than picking it from the catalogue.
 */
@Serializable
data class ModelEntry(
    val id: String,
    val label: String = id,
    val contextLength: Long? = null,
) {
    /** `deepseek/deepseek-v4-flash-0731` -> `deepseek-v4-flash-0731` */
    val shortId: String
        get() = id.substringAfterLast('/')

    /** `deepseek/deepseek-v4-flash-0731` -> `deepseek` */
    val provider: String
        get() = id.substringBefore('/', missingDelimiterValue = "")

    companion object {
        /**
         * Accepts pasted slugs with stray whitespace or a leading `openrouter/`
         * style prefix, and normalises them. Returns null for blank input.
         */
        fun parse(raw: String): ModelEntry? {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return null
            val id = trimmed.removePrefix("openrouter.ai/models/")
                .removePrefix("https://openrouter.ai/models/")
                .trim()
            if (id.isEmpty()) return null
            return ModelEntry(id = id, label = id)
        }
    }
}
