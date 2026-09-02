package com.aiwatch.core.net

/**
 * Reassembles Server-Sent Events frames from a line feed.
 *
 * OpenRouter streams `text/event-stream` bodies that look like:
 *
 * ```
 * data: {"choices":[{"delta":{"content":"Hel"}}]}
 *
 * data: {"choices":[{"delta":{"content":"lo"}}]}
 *
 * data: [DONE]
 * ```
 *
 * This class handles *framing only* — turning lines into `data:` payloads — and
 * has no JSON, OkHttp or Android imports. That keeps it exercisable from plain
 * JVM unit tests, which is where the fiddly multi-line and comment cases live.
 */
class SseFrameAssembler {

    private val data = StringBuilder()
    private var hasData = false

    /**
     * Feeds one line (without its line terminator). Returns the completed event
     * payload when a blank line closes the frame, otherwise null.
     */
    fun onLine(line: String): String? {
        if (line.isEmpty()) return dispatch()
        // A line beginning with ':' is an SSE comment / keep-alive.
        if (line.startsWith(":")) return null

        val colon = line.indexOf(':')
        val field = if (colon < 0) line else line.substring(0, colon)
        var value = if (colon < 0) "" else line.substring(colon + 1)
        // The spec strips exactly one leading space from the value.
        if (value.startsWith(" ")) value = value.substring(1)

        when (field) {
            "data" -> {
                if (hasData) data.append('\n')
                data.append(value)
                hasData = true
            }
            // event:, id:, retry: are irrelevant to a single-endpoint stream.
            else -> Unit
        }
        return null
    }

    /**
     * Emits any buffered payload at end-of-stream. Some servers close the
     * connection without the trailing blank line.
     */
    fun flush(): String? = dispatch()

    fun reset() {
        data.setLength(0)
        hasData = false
    }

    private fun dispatch(): String? {
        if (!hasData) return null
        val payload = data.toString()
        reset()
        return payload
    }

    companion object {
        /** The sentinel OpenRouter sends as its final frame. */
        const val DONE: String = "[DONE]"

        fun isDone(payload: String): Boolean = payload.trim() == DONE
    }
}
