package io.legado.app.ui.book.read

import android.app.Application
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.Bookmark
import io.legado.app.data.repository.BookmarkRepository
import io.legado.app.feature.reader.core.navigation.ReaderPageContext
import io.legado.app.help.config.AppConfigStore
import io.legado.app.model.ReadBook
import io.legado.app.support.InMemoryAppDatabaseFixture
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import splitties.init.injectAsAppCtx

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class ReadBookmarkDelegateTest {
    private lateinit var database: InMemoryAppDatabaseFixture
    private lateinit var repository: BookmarkRepository
    private lateinit var scope: CoroutineScope
    private lateinit var delegate: ReadBookmarkDelegate
    private var previousBook: Book? = null
    private var previousChapter = 0
    private var previousPosition = 0
    @Volatile private var state = ReadBookUiState()
    private val sheet get() = state.activeSheet
    private val pageText = "当前一段\n当前二段\n"
    private val page = ReaderPageContext(3, "第三章", 100, 100 + pageText.length, pageText, 100, "当前一段")
    private var following: ReaderPageContext? = ReaderPageContext(3, "第三章", page.endPosition,
        page.endPosition + 12, "下一页一段\n下一页二段\n", page.endPosition, "下一页一段")

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = RuntimeEnvironment.getApplication().also { it.injectAsAppCtx() }
        AppConfigStore.init(context)
        database = InMemoryAppDatabaseFixture(context)
        repository = BookmarkRepository(database.database.bookmarkDao)
        previousBook = ReadBook.book
        previousChapter = ReadBook.durChapterIndex
        previousPosition = ReadBook.durChapterPos
        setSessionField("book", Book(bookUrl = "url", name = "书", author = "作者"))
        setSessionField("durChapterIndex", 3)
        setSessionField("durChapterPos", 140)
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        delegate = ReadBookmarkDelegate(scope, object : ReadBookmarkDelegate.Host {
            override val currentCanvasPage = page
            override val nextCanvasPage get() = following
            override val currentExcerptSelection get() = state.findSheet<ReadBookSheet.BookmarkExcerpt>()?.selection
            override fun setActiveSheet(sheet: ReadBookSheet?) { state = state.withSheet(sheet) }
            override fun emitEffect(effect: ReadBookEffect) = Unit
        }, repository, flowOf("书" to "作者"))
    }

    @After fun tearDown() {
        scope.cancel()
        setSessionField("book", previousBook)
        setSessionField("durChapterIndex", previousChapter)
        setSessionField("durChapterPos", previousPosition)
        database.close()
        Dispatchers.resetMain()
    }

    private fun setSessionField(name: String, value: Any?) {
        ReadBook::class.java.getDeclaredField(name).apply { isAccessible = true }.set(null, value)
    }
    private fun settle() = runBlocking {
        withTimeout(10_000) { scope.coroutineContext[Job]!!.children.toList().joinAll() }
    }
    private fun open(): io.legado.app.feature.reader.bookmarks.ReaderExcerptSelection {
        delegate.addForCurrentPage()
        settle()
        return (sheet as ReadBookSheet.BookmarkExcerpt).selection
    }
    private fun stored() = runBlocking { repository.getAll() }

    @Test fun `saving a precise cross page text range persists only chosen text once`() {
        val selection = open()
        assertTrue(selection.hasNextPage)
        val start = 2
        val end = selection.currentText.length + 4
        delegate.saveExcerpt(start, end, "保留笔记")
        delegate.saveExcerpt(start, end, "保留笔记")
        settle()
        val saved = stored().single()
        assertEquals(selection.text.substring(start, end), saved.bookText)
        assertEquals(page.startPosition + start, saved.chapterPos)
        assertEquals("保留笔记", saved.content)
        assertNull(sheet)
        val again = open()
        assertEquals(saved.time, again.saved.single().id)
        delegate.editSaved(saved.time); settle()
        assertFalse((sheet as ReadBookSheet.Bookmark).isNew)
        assertEquals(saved.time, (sheet as ReadBookSheet.Bookmark).bookmark.time)
        delegate.delete((sheet as ReadBookSheet.Bookmark).bookmark); settle()
        assertTrue(stored().isEmpty())
        assertTrue((sheet as ReadBookSheet.BookmarkExcerpt).selection.saved.isEmpty())
    }

    @Test fun `legacy multiple bookmarks are edited and deleted by real ID without deleting adjacent records`() {
        val one = Bookmark(time = 1, bookName = "书", bookAuthor = "作者", chapterIndex = 3, chapterPos = 100, content = "第一条")
        val two = one.copy(time = 2, chapterPos = 105, content = "第二条")
        val boundary = one.copy(time = 3, chapterPos = following!!.endPosition)
        val otherChapter = one.copy(time = 4, chapterIndex = 4)
        val otherBook = one.copy(time = 5, bookAuthor = "其他作者")
        runBlocking { repository.saveAll(listOf(one,two,boundary,otherChapter,otherBook)) }
        assertEquals(setOf(1L,2L), open().saved.map { it.id }.toSet())
        delegate.editSaved(2); settle()
        assertEquals(two.time, (sheet as ReadBookSheet.Bookmark).bookmark.time)
        delegate.delete((sheet as ReadBookSheet.Bookmark).bookmark); settle()
        assertEquals(listOf(1L), (sheet as ReadBookSheet.BookmarkExcerpt).selection.saved.map { it.id })
        delegate.editSaved(1); settle()
        delegate.delete((sheet as ReadBookSheet.Bookmark).bookmark); settle()
        assertEquals(setOf(3L,4L,5L), stored().map { it.time }.toSet())
    }

    @Test fun `chapter final page excludes following chapter and empty selection does not save`() {
        following = following!!.copy(chapterIndex = 4)
        val selection = open()
        assertFalse(selection.hasNextPage)
        assertEquals(page.text, selection.text)
        delegate.saveExcerpt(0, 0, ""); settle()
        assertTrue(stored().isEmpty())
        assertTrue(sheet is ReadBookSheet.BookmarkExcerpt)
        delegate.saveExcerpt(1, 3, ""); settle()
        assertEquals(page.text.substring(1, 3), stored().single().bookText)
    }

    @Test fun `new selected text draft cannot present as a persisted bookmark`() {
        delegate.openEditor(Bookmark(bookName = "书", chapterIndex = 3, bookText = "选中的句子"))
        assertTrue((sheet as ReadBookSheet.Bookmark).isNew)
        assertTrue(stored().isEmpty())
    }

    @Test fun `swipe add opens selection without saving whole page automatically`() {
        delegate.toggleForCurrentPage(); settle()
        assertTrue(sheet is ReadBookSheet.BookmarkExcerpt)
        assertTrue(stored().isEmpty())
    }
}
