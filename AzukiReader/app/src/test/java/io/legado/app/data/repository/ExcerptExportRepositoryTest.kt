package io.legado.app.data.repository

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import io.legado.app.data.dao.BookmarkDao
import io.legado.app.data.entities.Bookmark
import io.legado.app.data.entities.BookMarking
import io.legado.app.domain.gateway.BookMarkingGateway
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Proxy

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class ExcerptExportRepositoryTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun `actual writer exports all stored books and disabled highlights and ignores broken anchors`() = runTest {
        val bookmarks = listOf(
            Bookmark(bookName = "甲书", bookAuthor = "甲", bookText = "第一句\n第二句", chapterName = "第一章"),
            Bookmark(bookName = "无正文定位", bookText = ""),
        )
        val dao = Proxy.newProxyInstance(BookmarkDao::class.java.classLoader, arrayOf(BookmarkDao::class.java)) { _, method, _ ->
            when (method.name) { "getAll" -> bookmarks; else -> error("Export must query all bookmarks: ${method.name}") }
        } as BookmarkDao
        val marks = listOf(
            BookMarking("m1", "url", bookName = "乙书", chapterName = "第二章", enabled = false,
                anchorJson = """{"chapterIndex":1,"selectedText":"高亮摘录","normalizedTextHash":"x"}"""),
            BookMarking("bad", "url", bookName = "损坏", anchorJson = "not json"),
            BookMarking("missing", "url", bookName = "没有句子", anchorJson = "{}"),
        )
        val gateway = Proxy.newProxyInstance(BookMarkingGateway::class.java.classLoader, arrayOf(BookMarkingGateway::class.java)) { _, method, _ ->
            when (method.name) { "flowAll" -> flowOf(marks); else -> error("Export must not depend on selected book: ${method.name}") }
        } as BookMarkingGateway
        val repository = ExcerptExportRepository(ApplicationProvider.getApplicationContext<Context>(), BookmarkRepository(dao), gateway)
        val file = temporary.newFile("全部摘录.md")
        file.writeText("OLD CONTENT THAT MUST BE TRUNCATED")
        assertEquals(2, repository.exportAll(Uri.fromFile(file).toString()))
        val text = file.readText(Charsets.UTF_8)
        assertTrue(text.contains("甲书"))
        assertTrue(text.contains("乙书"))
        assertTrue(text.contains("> 第一句\n> 第二句"))
        assertTrue(text.contains("高亮摘录"))
        assertFalse(text.contains("OLD CONTENT"))
        assertFalse(text.contains("没有句子"))
    }
}
