package com.aiwatch.core.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SseFrameAssemblerTest {

    private fun feed(vararg lines: String): List<String> {
        val assembler = SseFrameAssembler()
        val out = mutableListOf<String>()
        lines.forEach { line -> assembler.onLine(line)?.let(out::add) }
        assembler.flush()?.let(out::add)
        return out
    }

    @Test
    fun `a blank line closes the frame`() {
        val frames = feed(
            """data: {"choices":[{"delta":{"content":"Hi"}}]}""",
            "",
        )
        assertEquals(1, frames.size)
        assertEquals("""{"choices":[{"delta":{"content":"Hi"}}]}""", frames[0])
    }

    @Test
    fun `multiple events arrive in order`() {
        val frames = feed(
            "data: one", "",
            "data: two", "",
            "data: three", "",
        )
        assertEquals(listOf("one", "two", "three"), frames)
    }

    @Test
    fun `only one leading space is stripped from the value`() {
        assertEquals(listOf("  padded"), feed("data:   padded", ""))
    }

    @Test
    fun `a data line with no space after the colon is kept verbatim`() {
        assertEquals(listOf("tight"), feed("data:tight", ""))
    }

    @Test
    fun `multi-line data fields are joined with newlines`() {
        val frames = feed("data: first", "data: second", "")
        assertEquals(listOf("first\nsecond"), frames)
    }

    @Test
    fun `comment lines are ignored`() {
        val frames = feed(": keep-alive", "data: real", "")
        assertEquals(listOf("real"), frames)
    }

    @Test
    fun `non-data fields are ignored`() {
        val frames = feed("event: message", "id: 42", "retry: 1000", "data: payload", "")
        assertEquals(listOf("payload"), frames)
    }

    @Test
    fun `a field with no colon is treated as an empty-valued field`() {
        // "data" alone means an empty data line, which still opens a frame.
        val frames = feed("data", "")
        assertEquals(listOf(""), frames)
    }

    @Test
    fun `blank lines with nothing buffered emit nothing`() {
        assertTrue(feed("", "", "").isEmpty())
    }

    @Test
    fun `flush recovers a frame with no trailing blank line`() {
        val assembler = SseFrameAssembler()
        assertNull(assembler.onLine("data: tail"))
        assertEquals("tail", assembler.flush())
    }

    @Test
    fun `flush is a no-op when nothing is buffered`() {
        assertNull(SseFrameAssembler().flush())
    }

    @Test
    fun `state resets after dispatch`() {
        val assembler = SseFrameAssembler()
        assembler.onLine("data: one")
        assertEquals("one", assembler.onLine(""))
        assertNull(assembler.onLine(""))
    }

    @Test
    fun `the DONE sentinel is recognised`() {
        assertTrue(SseFrameAssembler.isDone("[DONE]"))
        assertTrue(SseFrameAssembler.isDone("  [DONE]  "))
        assertTrue(!SseFrameAssembler.isDone("""{"done":true}"""))
    }
}
