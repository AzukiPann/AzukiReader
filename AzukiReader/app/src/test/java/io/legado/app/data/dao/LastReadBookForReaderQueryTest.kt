package io.legado.app.data.dao

import android.app.Application
import androidx.room.Room
import io.legado.app.constant.BookType
import io.legado.app.data.AppDatabase
import io.legado.app.data.entities.Book
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class LastReadBookForReaderQueryTest {
    @Test
    fun openingBookImmediatelyUpdatesRecencyWithoutChangingReadingPosition() {
        val database = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        try {
            val books = database.bookDao
            books.insert(Book(bookUrl = "older", durChapterTime = 100, durChapterIndex = 7, durChapterPos = 321),
                Book(bookUrl = "newer", durChapterTime = 200))
            assertEquals("newer", books.lastReadBookForReader?.bookUrl)
            books.markBookOpened("older", 300)
            assertEquals("older", books.lastReadBookForReader?.bookUrl)
            val opened = books.getBook("older")!!
            assertEquals(300L, opened.durChapterTime)
            assertEquals(7, opened.durChapterIndex)
            assertEquals(321, opened.durChapterPos)
            books.markBookOpened("missing", 400)
            assertEquals("older", books.lastReadBookForReader?.bookUrl)
        } finally { database.close() }
    }

    @Test
    fun `reader shortcut includes local comics and keeps text-only lookup separate`() {
        val database = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        try {
            val books = database.bookDao
            val comic = Book(
                bookUrl = "comic",
                type = BookType.local or BookType.image,
                durChapterTime = 100,
            )
            books.insert(comic)
            assertNull(books.lastReadBook)
            assertEquals(comic.bookUrl, books.lastReadBookForReader?.bookUrl)

            val text = Book(bookUrl = "text", type = BookType.text, durChapterTime = 200)
            books.insert(text, Book(bookUrl = "online-images", type = BookType.image, durChapterTime = 400))
            assertEquals(text.bookUrl, books.lastReadBookForReader?.bookUrl)

            val legacy = Book(
                bookUrl = "legacy",
                origin = BookType.localTag,
                originName = "legacy.CBZ",
                type = 0,
                durChapterTime = 300,
            )
            books.insert(legacy)
            assertEquals(legacy.bookUrl, books.lastReadBookForReader?.bookUrl)
            assertEquals(text.bookUrl, books.lastReadBook?.bookUrl)
        } finally {
            database.close()
        }
    }
}
