package io.legado.app.data.repository

import android.app.Application
import androidx.room.Room
import io.legado.app.R
import io.legado.app.data.AppDatabase
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.readRecord.ReadRecordSession
import io.legado.app.data.entities.readRecord.recordPosition
import io.legado.app.ui.book.readRecord.summarizeReadingProgress
import io.legado.app.ui.book.readRecord.withChapterReference
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35], qualifiers = "zh-rCN")
class ReadingChapterReferenceTest {
    private lateinit var db: AppDatabase
    private lateinit var books: BookRepository
    private val book = Book(bookUrl = "copy-a", name = "书", author = "作者", totalChapterNum = 120)
    private val session = ReadRecordSession(bookName = "书", bookAuthor = "作者", bookUrl = "copy-a",
        startTime = 1000, endTime = 61000).recordPosition(17, 420, 1000).recordPosition(2, 900, 1000)

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        books = BookRepository(db.bookDao, db.bookChapterDao, db)
    }
    @After fun close() { db.close() }
    private fun chapter(url: String = "copy-a", title: String = "归途") =
        BookChapter(url = "$url-17", bookUrl = url, index = 17, title = title)

    @Test fun `highest title and total come from its exact copy after backtracking`() = runBlocking {
        db.bookDao.insert(book, book.copy(bookUrl = "copy-b", totalChapterNum = 300))
        db.bookChapterDao.insert(chapter(), chapter("copy-b", "另一版章节"),
            BookChapter(url = "copy-a-2", bookUrl = "copy-a", index = 2, title = "故乡"))
        val reference = books.observeReadingChapterReferences(listOf(session)).first().getValue("书" to "作者")
        val progress = summarizeReadingProgress(listOf(session))!!.withChapterReference(reference)
        assertEquals(2L, progress.currentChapterIndex)
        assertEquals(17, progress.highestChapterIndex)
        assertEquals("归途", progress.highestChapterTitle)
        assertEquals(120, progress.totalChapters)
        assertEquals("故乡", progress.currentChapterTitle)
        assertEquals(120, progress.currentTotalChapters)
        assertEquals(0.42, progress.highestChapterProgress!!, 0.00001)
        assertEquals("最高：归途（18/120）· 章内 42%", RuntimeEnvironment.getApplication().getString(
            R.string.reading_furthest_progress, progress.highestChapterTitle, "18/120", "42%"))
    }

    @Test fun `missing imported directory stays unknown and ambiguous copies are not guessed`() = runBlocking {
        val flow = books.observeReadingChapterReferences(listOf(session.copy(bookUrl = "debug-only-uri")))
        assertNull(flow.first().getValue("书" to "作者").title)
        db.bookDao.insert(book, book.copy(bookUrl = "copy-b", totalChapterNum = 300))
        db.bookChapterDao.insert(chapter(), chapter("copy-b", "另一版章节"))
        val unknown = flow.first().getValue("书" to "作者")
        assertNull(unknown.title)
        assertNull(unknown.totalChapters)
    }

    @Test fun `unique identity fallback resolves imported data and invalid total remains unknown`() = runBlocking {
        db.bookDao.insert(book.copy(totalChapterNum = 5))
        db.bookChapterDao.insert(chapter())
        val reference = books.observeReadingChapterReferences(listOf(session.copy(bookUrl = "debug-only-uri")))
            .first().getValue("书" to "作者")
        assertEquals("归途", reference.title)
        assertNull(reference.totalChapters)
    }

    @Test fun `directory title changes refresh existing observation`() = runBlocking {
        db.bookDao.insert(book)
        db.bookChapterDao.insert(chapter())
        val first = CompletableDeferred<Unit>()
        val refreshed = async {
            withTimeout(10_000) {
                books.observeReadingChapterReferences(listOf(session)).onEach { first.complete(Unit) }
                    .first { it["书" to "作者"]?.title == "新的标题" }
            }
        }
        first.await()
        db.bookChapterDao.update(chapter(title = "新的标题"))
        assertEquals("新的标题", refreshed.await().getValue("书" to "作者").title)
    }
}
