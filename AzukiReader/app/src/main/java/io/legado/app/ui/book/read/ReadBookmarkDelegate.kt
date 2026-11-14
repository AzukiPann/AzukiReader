package io.legado.app.ui.book.read

import io.legado.app.feature.reader.bookmarks.*
import kotlinx.collections.immutable.toImmutableList
import io.legado.app.R
import io.legado.app.data.entities.Bookmark
import io.legado.app.data.repository.BookmarkRepository
import io.legado.app.feature.reader.core.navigation.ReaderPageContext
import io.legado.app.model.ReadBook
import io.legado.app.model.ReaderBookmarkState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import splitties.init.appCtx
import kotlin.math.abs

/**
 * 书签域（R2.2 续批）。
 *
 * **无自持状态**：书签编辑器就是 `ReadBookSheet.Bookmark`，草稿随 sheet 参数走，
 * `activeSheet` 是单一持有者，故读写经 [Host]。
 */
class ReadBookmarkDelegate(
    private val scope: CoroutineScope,
    private val host: Host,
    private val bookmarkRepository: BookmarkRepository,
    /**
     * 当前书的 `书名 to 作者`（未开书时为 null）——`bookmarks` 表没有 bookUrl 列，
     * 这两个字段就是关联键。[start] 用它当切换键重订书签流。
     */
    private val bookKey: Flow<Pair<String, String>?>,
) {

    /**
     * 串行化摘录保存、编辑、删除及手势查找，避免重复保存与旧记录状态竞争。
     */
    private val toggleMutex = Mutex()

    interface Host {
        val currentCanvasPage: ReaderPageContext?
        val nextCanvasPage: ReaderPageContext? get() = null
        val currentExcerptSelection: ReaderExcerptSelection? get() = null

        /** 打开/关闭书签弹层的同时收起阅读菜单。 */
        fun setActiveSheet(sheet: ReadBookSheet?)

        fun emitEffect(effect: ReadBookEffect)
    }

    /**
     * 维护 [ReaderBookmarkState]：渲染层要在主线程热路径上同步判定「本页是否有书签」画角标，
     * 不能起协程查库，所以把当前书的书签位置整份缓存进会话快照。VM 构造时调一次。
     *
     * 收集随 [scope] 结束，`finally` 里清掉快照——否则下一本书会读到上一本的书签位置。
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun start() {
        scope.launch {
            try {
                bookKey
                    .distinctUntilChanged()
                    .flatMapLatest { key ->
                        if (key == null) {
                            flowOf(key to emptyList())
                        } else {
                            bookmarkRepository.flowByBook(key.first, key.second).map { key to it }
                        }
                    }
                    .collect { (key, bookmarks) ->
                        if (key == null) {
                            ReaderBookmarkState.clear()
                        } else {
                            ReaderBookmarkState.update(key.first, key.second, bookmarks)
                        }
                        host.emitEffect(ReadBookEffect.UpBookmarkBadge)
                    }
            } finally {
                ReaderBookmarkState.clear()
            }
        }
    }

    /**
     * 下滑手势：本页无书签则选择摘录文字，已有则删掉离当前阅读位置最近的一条。
     *
     * 页范围取 `[页首位置, 下一页页首位置)`；Canvas 就绪后直接使用 reader core
     * 从当前页面元素计算的范围与正文。
     * 与文字选区保存的章节字符位置使用同一坐标系。
     * 整体串行化：先查后写不是原子的，快速连滑会双双看到「空」而重复插入。
     */
    fun toggleForCurrentPage() {
        scope.launch(IO) {
            toggleMutex.withLock {
                val book = ReadBook.book ?: return@withLock
                val page = currentPage() ?: return@withLock
                val existing = bookmarkRepository.getByChapterRange(
                    bookName = book.name,
                    bookAuthor = book.author,
                    chapterIndex = page.chapterIndex,
                    startPos = page.startPosition,
                    endPos = page.endPosition,
                )
                if (existing.isEmpty()) {
                    showExcerptSelection(book.bookUrl, book.name, book.author, page)
                } else {
                    // 只删离当前阅读位置最近的一条：同一页可能有多条书签，不应整页误删。
                    // 书签与划线笔记完全独立，这里只删书签本身。
                    val nearest = existing.minByOrNull { abs(it.chapterPos - ReadBook.durChapterPos) }
                        ?: return@withLock
                    bookmarkRepository.delete(nearest)
                    host.emitEffect(
                        ReadBookEffect.ShowToast(appCtx.getString(R.string.bookmark_removed))
                    )
                }
            }
        }
    }

    /** Reading bookmark entry now starts with current/next-page text selection. */
    fun addForCurrentPage() {
        scope.launch(IO) {
            toggleMutex.withLock {
                val book = ReadBook.book ?: return@withLock
                val page = currentPage() ?: return@withLock
                showExcerptSelection(book.bookUrl, book.name, book.author, page)
            }
        }
    }

    private suspend fun showExcerptSelection(url: String, name: String, author: String, page: ReaderPageContext) {
        val next = excerptNextPage(page, host.nextCanvasPage)
        val end = next?.endPosition ?: page.endPosition
        val saved = bookmarkRepository.getByChapterRange(name, author, page.chapterIndex, page.startPosition, end)
        val selection = ReaderExcerptSelection(url, name, author, page.chapterIndex, page.chapterTitle,
            page.startPosition, end, page.text, next?.text, next?.startPosition,
            saved.map { SavedExcerpt(it.time, it.bookText, it.content) }.toImmutableList())
        withContext(Main) { host.setActiveSheet(ReadBookSheet.BookmarkExcerpt(selection)) }
    }

    fun saveExcerpt(start: Int, end: Int, note: String) {
        val selection = host.currentExcerptSelection ?: return
        val excerpt = selectedExcerpt(selection, start, end) ?: return
        scope.launch(IO) {
            toggleMutex.withLock {
                // Repeated save taps after the first completed save cannot create duplicates.
                if (host.currentExcerptSelection != selection) return@withLock
                bookmarkRepository.save(Bookmark(bookName = selection.bookName, bookAuthor = selection.bookAuthor,
                    bookUrl = selection.bookUrl, chapterIndex = selection.chapterIndex,
                    chapterName = selection.chapterTitle, chapterPos = excerpt.position,
                    bookText = excerpt.text, content = note))
                withContext(Main) { host.setActiveSheet(null) }
                host.emitEffect(ReadBookEffect.ShowToast(appCtx.getString(R.string.bookmark_added)))
            }
        }
    }

    fun editSaved(id: Long) {
        val selection = host.currentExcerptSelection ?: return
        if (selection.saved.none { it.id == id }) return
        scope.launch(IO) {
            toggleMutex.withLock {
                val bookmark = bookmarkRepository.getByBook(selection.bookName, selection.bookAuthor)
                    .firstOrNull { it.time == id } ?: return@withLock
                withContext(Main) { openEditor(bookmark, isNew = false) }
            }
        }
    }

    private suspend fun closeEditorAndRefresh() {
        val selection = host.currentExcerptSelection
        if (selection == null) withContext(Main) { host.setActiveSheet(null) }
        else {
            val saved = bookmarkRepository.getByChapterRange(selection.bookName, selection.bookAuthor,
                selection.chapterIndex, selection.pageStart, selection.endPosition)
            withContext(Main) {
                host.setActiveSheet(ReadBookSheet.BookmarkExcerpt(selection.copy(
                    saved = saved.map { SavedExcerpt(it.time, it.bookText, it.content) }.toImmutableList())))
            }
        }
    }

    /** 从划词菜单「加书签」进入：草稿已由调用方按选中文本构造好。 */
    fun openEditor(bookmark: Bookmark, isNew: Boolean = true) {
        host.setActiveSheet(ReadBookSheet.Bookmark(bookmark.copy(), isNew = isNew))
    }

    fun save(bookmark: Bookmark) {
        scope.launch(IO) {
            toggleMutex.withLock {
                bookmarkRepository.save(bookmark.copy())
                closeEditorAndRefresh()
            }
        }
    }

    fun delete(bookmark: Bookmark) {
        scope.launch(IO) {
            toggleMutex.withLock {
                bookmarkRepository.delete(bookmark)
                closeEditorAndRefresh()
            }
        }
    }

    private fun currentPage(): ReaderPageContext? = host.currentCanvasPage
        ?.takeIf { it.chapterIndex == ReadBook.durChapterIndex }

}
