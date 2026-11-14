package io.legado.app.feature.reader.bookmarks

import io.legado.app.feature.reader.core.navigation.ReaderPageContext
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.*
import org.junit.Test

class ReaderExcerptSelectionTest {
    private fun page(text: String, start: Int = 0, chapter: Int = 1) = ReaderPageContext(chapter, "章", start, start + text.length, text, start, null)
    private fun state(current: ReaderPageContext, candidate: ReaderPageContext?): ReaderExcerptSelection {
        val next = excerptNextPage(current, candidate)
        return ReaderExcerptSelection("url", "书", "作者", 1, "章", current.startPosition,
            next?.endPosition ?: current.endPosition, current.text, next?.text, next?.startPosition, persistentListOf())
    }

    @Test fun `continuous text preserves exact whitespace across pages and supports partial sentences`() {
        val first = page("首段\nThis is ", 100)
        val next = page("a paragraph.\n下一段", first.endPosition)
        val result = state(first, next)
        assertEquals("首段\nThis is a paragraph.\n下一段", result.text)
        val start = result.text.indexOf("This is ") + 5
        val end = result.text.indexOf(".\n")
        assertEquals("is a paragraph", selectedExcerpt(result, start, end)?.text)
        assertEquals(100 + start, selectedExcerpt(result, end, start)?.position)
        assertEquals(selectedExcerpt(result, start, end), selectedExcerpt(result, end, start))
    }

    @Test fun `chapter boundary and stale windows never include next page`() {
        val first = page("当前段")
        assertNull(excerptNextPage(first, page("下一章", first.endPosition, chapter = 2)))
        assertNull(excerptNextPage(first, page("旧页", 0)))
        assertEquals("当前段", state(first, page("下一章", first.endPosition, 2)).text)
        assertFalse(state(first, null).hasNextPage)
    }

    @Test fun `source offsets follow selection on next page and preserve indentation`() {
        val result = state(page("  首段\r\n", 50), page("　次段\n", 70))
        val start = result.currentText.length + 1
        assertEquals(71, selectedExcerpt(result, start, start + 2)?.position)
        assertEquals("次段", selectedExcerpt(result, start, start + 2)?.text)
        assertEquals("  首段\r\n　次段\n", result.text)
    }

    @Test fun `empty invalid whitespace and split emoji selections cannot save`() {
        val result = state(page("　\nA😀B"), null)
        assertNull(selectedExcerpt(result, 0, 0))
        assertNull(selectedExcerpt(result, -1, 3))
        assertNull(selectedExcerpt(result, 0, Int.MAX_VALUE))
        assertNull(selectedExcerpt(result, 0, 2))
        assertNull(selectedExcerpt(result, 3, 4))
        assertEquals("😀", selectedExcerpt(result, 3, 5)?.text)
    }

    @Test fun `title only page can select next body page at its correct chapter position`() {
        val result = state(page("").copy(endPosition = 1), page("正文段落", 0))
        assertTrue(result.hasNextPage)
        assertEquals("正文段落", result.text)
        assertEquals(0, selectedExcerpt(result, 0, 2)?.position)
    }
}
