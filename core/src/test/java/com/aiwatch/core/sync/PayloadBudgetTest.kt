package com.aiwatch.core.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PayloadBudgetTest {

    /** Stand-in measurement: 10 bytes per entry, like a fixed-width encoding. */
    private val measure: (List<Int>) -> Int = { it.size * 10 }

    @Test
    fun `keeps everything when it already fits`() {
        val items = listOf(1, 2, 3)
        assertEquals(items, PayloadBudget.largestPrefixThatFits(items, maxBytes = 100, encodedSize = measure))
    }

    @Test
    fun `trims to the longest prefix that fits`() {
        val items = listOf(1, 2, 3, 4, 5)
        // 35 bytes allows three entries (30) but not four (40).
        val kept = PayloadBudget.largestPrefixThatFits(items, maxBytes = 35, encodedSize = measure)
        assertEquals(listOf(1, 2, 3), kept)
    }

    @Test
    fun `exact boundary is inclusive`() {
        val items = listOf(1, 2, 3, 4)
        val kept = PayloadBudget.largestPrefixThatFits(items, maxBytes = 30, encodedSize = measure)
        assertEquals(listOf(1, 2, 3), kept)
    }

    @Test
    fun `returns empty when even one entry is too large`() {
        val kept = PayloadBudget.largestPrefixThatFits(listOf(1, 2), maxBytes = 5, encodedSize = measure)
        assertTrue(kept.isEmpty())
    }

    @Test
    fun `empty input is passed through`() {
        assertTrue(PayloadBudget.largestPrefixThatFits(emptyList(), maxBytes = 1, encodedSize = measure).isEmpty())
    }

    @Test
    fun `reports how many entries were dropped`() {
        val result = PayloadBudget.fitReportingDropped(
            items = listOf(1, 2, 3, 4, 5),
            maxBytes = 25,
            encodedSize = measure,
        )
        assertEquals(listOf(1, 2), result.kept)
        assertEquals(3, result.dropped)
        assertTrue(result.wasTruncated)
    }

    @Test
    fun `no truncation means no drop reported`() {
        val result = PayloadBudget.fitReportingDropped(
            items = listOf(1, 2),
            maxBytes = 999,
            encodedSize = measure,
        )
        assertFalse(result.wasTruncated)
        assertEquals(0, result.dropped)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects a non-positive budget`() {
        PayloadBudget.largestPrefixThatFits(listOf(1), maxBytes = 0, encodedSize = measure)
    }

    @Test
    fun `matches a linear scan across many sizes`() {
        // The binary search must agree with the obvious O(n) answer everywhere.
        // Starts at 7, not 0: a zero budget violates the documented precondition
        // and is covered by `rejects a non-positive budget` instead.
        val items = (1..40).toList()
        for (budget in 7..420 step 7) {
            var count = 0
            while (count < items.size && (count + 1) * 10 <= budget) count++
            val expected = items.take(count)
            val actual = PayloadBudget.largestPrefixThatFits(items, budget, measure)
            assertEquals("budget=$budget", expected, actual)
        }
    }
}
