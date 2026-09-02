package com.aiwatch.core.sync

import com.aiwatch.core.model.ModelEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncContractTest {

    private val config = SyncConfig(
        apiKey = "sk-or-v1-secret",
        models = listOf(
            ModelEntry(id = "deepseek/deepseek-v4-flash-0731"),
            ModelEntry(id = "anthropic/claude-sonnet-4", label = "Claude Sonnet 4"),
        ),
        defaultModelId = "deepseek/deepseek-v4-flash-0731",
        temperature = 0.4f,
        maxTokens = 256,
        systemPrompt = "Be brief.",
        updatedAt = 1_700_000_000_000L,
    )

    @Test
    fun `encode then decode round-trips every field`() {
        val restored = SyncContract.decode(SyncContract.encode(config))
        assertEquals(config, restored)
    }

    @Test
    fun `decode tolerates unknown fields from a newer phone app`() {
        val json = SyncContract.encode(config)
            .replaceFirst("{", """{"aFieldFromTheFuture":123,""")
        val restored = SyncContract.decode(json)
        assertEquals(config, restored)
    }

    @Test
    fun `decode returns null rather than throwing on garbage`() {
        assertNull(SyncContract.decode("not json at all"))
        assertNull(SyncContract.decode(""))
        assertNull(SyncContract.decode(null))
    }

    @Test
    fun `a small config is left untouched by fit`() {
        val result = SyncContract.fit(config)
        assertEquals(config.models.size, result.kept.size)
        assertEquals(0, result.dropped)
    }

    @Test
    fun `an oversized catalogue is trimmed to the budget`() {
        val huge = SyncConfig(
            apiKey = "k",
            // Long ids so the encoded form definitely exceeds the byte budget.
            models = (1..4000).map { index ->
                ModelEntry(
                    id = "provider/model-${index}-${"x".repeat(120)}",
                    label = "Model $index",
                )
            },
        )
        val result = SyncContract.fit(huge)

        assertTrue("expected some models to be dropped", result.dropped > 0)
        assertTrue("expected some models to survive", result.kept.isNotEmpty())

        val encoded = SyncContract.encode(huge.copy(models = result.kept))
        assertTrue(
            "trimmed payload must fit the budget",
            encoded.toByteArray(Charsets.UTF_8).size <= SyncContract.MAX_PAYLOAD_BYTES,
        )
    }

    @Test
    fun `encodeForTransport always produces a payload within the budget`() {
        val huge = config.copy(
            models = (1..4000).map { ModelEntry(id = "p/m-$it-${"y".repeat(120)}") },
        )
        val payload = SyncContract.encodeForTransport(huge)
        assertTrue(payload.toByteArray(Charsets.UTF_8).size <= SyncContract.MAX_PAYLOAD_BYTES)
    }

    @Test
    fun `isReady requires both a key and a model`() {
        assertTrue(!SyncConfig().isReady)
        assertTrue(!SyncConfig(apiKey = "k").isReady)
        assertTrue(!SyncConfig(models = listOf(ModelEntry("a/b"))).isReady)
        assertTrue(SyncConfig(apiKey = "k", models = listOf(ModelEntry("a/b"))).isReady)
    }

    @Test
    fun `preferredModel falls back to the first entry`() {
        val noDefault = config.copy(defaultModelId = "")
        assertEquals("deepseek/deepseek-v4-flash-0731", noDefault.preferredModel?.id)

        val staleDefault = config.copy(defaultModelId = "gone/model")
        assertEquals("deepseek/deepseek-v4-flash-0731", staleDefault.preferredModel?.id)
    }

    @Test
    fun `withModels repoints a default that was removed`() {
        val next = config.withModels(listOf(ModelEntry(id = "only/one")))
        assertEquals("only/one", next.defaultModelId)
    }

    @Test
    fun `model parse normalises pasted slugs`() {
        assertEquals("deepseek/deepseek-v4-flash-0731", ModelEntry.parse("  deepseek/deepseek-v4-flash-0731  ")?.id)
        assertEquals(
            "deepseek/deepseek-v4-flash-0731",
            ModelEntry.parse("https://openrouter.ai/models/deepseek/deepseek-v4-flash-0731")?.id,
        )
        assertNull(ModelEntry.parse("   "))
    }

    @Test
    fun `model id is split into provider and short id`() {
        val entry = ModelEntry(id = "deepseek/deepseek-v4-flash-0731")
        assertEquals("deepseek", entry.provider)
        assertEquals("deepseek-v4-flash-0731", entry.shortId)
    }
}
