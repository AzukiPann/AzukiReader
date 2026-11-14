package io.legado.app.model.readingdata

import android.app.Application
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.readRecord.ReadRecordSession
import io.legado.app.feature.reader.core.source.ReaderChapterSource
import io.legado.app.help.book.BookContent
import io.legado.app.model.ReadBook
import io.legado.app.model.reader.ReaderChapterInput
import io.legado.app.model.reader.ReaderChapterInputWindow
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import splitties.init.injectAsAppCtx

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class ReadBookReadingProgressTest {
    private val names = listOf("book", "durChapterIndex", "durChapterPos", "readerChapterInputWindow", "currentActiveSession")
    private lateinit var previous: Map<String, Any?>
    private val book = Book(bookUrl = "progress-copy", name = "book", author = "author")
    private fun field(name: String) = ReadBook::class.java.getDeclaredField(name).apply { isAccessible = true }
    private fun session() = field("currentActiveSession").get(null) as ReadRecordSession

    @Before fun setup() {
        RuntimeEnvironment.getApplication().injectAsAppCtx()
        previous = names.associateWith { field(it).get(null) }
        field("book").set(null, book)
        field("currentActiveSession").set(null, null)
    }
    @After fun restore() { previous.forEach { (name, value) -> field(name).set(null, value) } }

    private fun position(chapter: Int, offset: Int, contentBook: Book = book) {
        field("durChapterIndex").setInt(null, chapter)
        field("durChapterPos").setInt(null, offset)
        val input = ReaderChapterInput(contentBook, null,
            BookChapter(bookUrl = contentBook.bookUrl, index = chapter, title = "chapter", url = "chapter-$chapter"),
            "chapter", BookContent(false, listOf("文".repeat(1000)), null),
            ReaderChapterSource(chapter, "chapter", emptyList(), 1000, "文".repeat(1000)),
            0, 0, 0, 0, 0)
        field("readerChapterInputWindow").set(null, ReaderChapterInputWindow(current = input))
    }

    @Test fun `reader captures current offset and retains maximum after reviewing earlier chapter`() {
        position(17, 420)
        ReadBook.initReadTime()
        position(2, 900)
        ReadBook.upReadTime()
        assertEquals(2L, session().words)
        assertEquals(0.9, session().chapterProgress!!, 0.00001)
        assertEquals(17, session().highestChapterIndex)
        assertEquals(0.42, session().highestChapterProgress!!, 0.00001)
    }

    @Test fun `stale input from another book never supplies chapter percentage`() {
        position(17, 420, book.copy(bookUrl = "other-copy"))
        ReadBook.initReadTime()
        assertEquals(17L, session().words)
        assertNull(session().chapterProgress)
        assertNull(session().highestChapterProgress)
    }
}
