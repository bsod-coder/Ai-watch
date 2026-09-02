package com.aiwatch.core.sync

/**
 * Trims an ordered collection so that its encoded form fits a transport budget.
 *
 * The Wear Data Layer caps both `DataItem` assets and messages at roughly 100 KB.
 * A user who pastes a few hundred models from the OpenRouter catalogue can blow
 * through that, so before sending we drop models from the tail until we fit.
 *
 * This object is deliberately free of Android and serialization imports: it
 * takes the measuring function as a parameter, which keeps it testable as plain
 * JVM code.
 */
object PayloadBudget {

    /**
     * Returns the longest prefix of [items] whose measured size is <= [maxBytes].
     *
     * Assumes `encodedSize` is monotonically non-decreasing as the prefix grows,
     * which holds for any "encode the whole list" measurement. Uses binary search
     * so a large pasted catalogue costs O(log n) encodes rather than O(n).
     */
    fun <T> largestPrefixThatFits(
        items: List<T>,
        maxBytes: Int,
        encodedSize: (List<T>) -> Int,
    ): List<T> {
        require(maxBytes > 0) { "maxBytes must be positive, was $maxBytes" }
        if (items.isEmpty()) return items
        if (encodedSize(items) <= maxBytes) return items

        var low = 0
        var high = items.size
        while (low < high) {
            val mid = low + (high - low + 1) / 2
            if (encodedSize(items.subList(0, mid)) <= maxBytes) {
                low = mid
            } else {
                high = mid - 1
            }
        }
        return items.subList(0, low).toList()
    }

    /**
     * Same as [largestPrefixThatFits] but reports how many entries were dropped,
     * so the UI can tell the user their list was shortened.
     */
    fun <T> fitReportingDropped(
        items: List<T>,
        maxBytes: Int,
        encodedSize: (List<T>) -> Int,
    ): FitResult<T> {
        val kept = largestPrefixThatFits(items, maxBytes, encodedSize)
        return FitResult(kept = kept, dropped = items.size - kept.size)
    }

    data class FitResult<T>(val kept: List<T>, val dropped: Int) {
        val wasTruncated: Boolean get() = dropped > 0
    }
}
