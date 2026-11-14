package io.legado.app.data.repository

import io.legado.app.constant.PreferKey
import io.legado.app.data.entities.BookGroup
import io.legado.app.domain.model.settings.BookshelfSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class BookshelfSettingsMappingTest {
    @Test fun `archive shelf count defaults to twelve persists independently and clamps invalid backups`() {
        assertEquals(12, emptyMap<String, Any?>().toTestPreferences().toBookshelfSettings().wenku8ArchiveShelfBookCount)
        assertEquals(12, BookshelfSettings().wenku8ArchiveShelfBookCount)
        for (count in listOf(1, 12, 24, 100)) {
            val values = captureAtomicUpdateValues(
                current = BookshelfSettings(wenku8ArchiveShelfBookCount = 7),
                read = { it.toBookshelfSettings() }, toPrefMap = BookshelfSettings::toPrefMap,
                transform = { it.copy(wenku8ArchiveShelfBookCount = count) })
            assertEquals(mapOf(PreferKey.wenku8ArchiveShelfBookCount to count), values)
            assertEquals(count, BookshelfSettings(wenku8ArchiveShelfBookCount = count).toPrefMap()
                .toTestPreferences().toBookshelfSettings().wenku8ArchiveShelfBookCount)
        }
        assertEquals(1, mapOf(PreferKey.wenku8ArchiveShelfBookCount to -9).toTestPreferences().toBookshelfSettings().wenku8ArchiveShelfBookCount)
        assertEquals(100, mapOf(PreferKey.wenku8ArchiveShelfBookCount to Int.MAX_VALUE).toTestPreferences().toBookshelfSettings().wenku8ArchiveShelfBookCount)
    }


    @Test fun `fresh bookshelf defaults to list but explicitly selected grids survive`() {
        val defaults = emptyMap<String, Any?>().toTestPreferences().toBookshelfSettings()
        assertEquals(0, defaults.bookshelfLayoutModePortrait)
        assertEquals(0, defaults.bookshelfLayoutModeLandscape)
        assertEquals(defaults, BookshelfSettings())
        val grids = mapOf(PreferKey.bookshelfLayoutModePortrait to 1, PreferKey.bookshelfLayoutModeLandscape to 1)
            .toTestPreferences().toBookshelfSettings()
        assertEquals(1, grids.bookshelfLayoutModePortrait)
        assertEquals(1, grids.bookshelfLayoutModeLandscape)
    }

    @Test fun `default shelf places the last opened book first regardless of update time`() {
        val settings = emptyMap<String, Any?>().toTestPreferences().toBookshelfSettings()
        assertEquals(0, settings.bookshelfSort)
        assertEquals(1, settings.bookshelfSortOrder)
        fun book(url: String, opened: Long, updated: Long) = io.legado.app.ui.main.bookshelf.BookShelfItem(
            bookUrl = url, name = url, author = "author", origin = "source", originName = "source",
            coverUrl = null, customCoverUrl = null, durChapterTitle = null,
            durChapterTime = opened, durChapterPos = 0, latestChapterTitle = null,
            latestChapterTime = updated, lastCheckCount = 0, totalChapterNum = 1,
            durChapterIndex = 0, type = 0, group = 0, order = 0,
        )
        val first = book("older", 100, 9000)
        val second = book("last-opened", 300, 1)
        val third = book("middle", 200, 5000)
        val repository = BookshelfRepository()
        fun sorted(books: List<io.legado.app.ui.main.bookshelf.BookShelfItem>) =
            repository.sortBooks(books, null, settings.bookshelfSort, settings.bookshelfSortOrder).map { it.bookUrl }
        assertEquals(listOf("last-opened", "middle", "older"), sorted(listOf(first, second, third)))
        assertEquals(listOf("older", "last-opened", "middle"), sorted(listOf(first.copy(durChapterTime = 400), second, third)))
    }


    @Test
    fun `drawer style round trips without modifying group selection or book display`() {
        val original = BookshelfSettings(
            saveTabPosition = 987654321L,
            bookshelfSort = 4,
            bookshelfLayoutModePortrait = 0,
            bookshelfLayoutModeLandscape = 1,
        )
        val drawer = original.copy(bookGroupStyle = 3)
        val restored = drawer.toPrefMap().toTestPreferences().toBookshelfSettings()
        assertEquals(drawer, restored)
        assertEquals(original, restored.copy(bookGroupStyle = original.bookGroupStyle))
    }

    @Test
    fun `旧分组 ID 可读取但写入使用独立键`() {
        val legacy = mapOf(PreferKey.saveTabPosition to 987654321L).toTestPreferences()
        val settings = legacy.toBookshelfSettings()

        assertEquals(987654321L, settings.saveTabPosition)
        assertEquals(987654321L, settings.toPrefMap()[PreferKey.bookshelfSelectedGroupId])
        assertEquals(false, settings.toPrefMap().containsKey(PreferKey.saveTabPosition))
        assertEquals(BookGroup.IdAll,
            mapOf(PreferKey.saveTabPosition to 2).toTestPreferences()
                .toBookshelfSettings().saveTabPosition
        )
    }

    @Test
    fun `Bookshelf 55 键写读映射逐字段对应`() {
        assertEquals(55, BookshelfSettings().toPrefMap().size)
        bookshelfMappingSamples().forEach { expected ->
            assertEquals(expected.expectedPrefMap(), expected.toPrefMap())
            assertEquals(
                expected,
                expected.expectedPrefMap().toTestPreferences().toBookshelfSettings(),
            )
        }
    }

    @Test
    fun `隐藏空分组通过真实原子路径只写对应键`() {
        val values = captureAtomicUpdateValues(
            current = BookshelfSettings(hideEmptyGroups = false),
            read = { it.toBookshelfSettings() },
            toPrefMap = BookshelfSettings::toPrefMap,
            transform = { it.copy(hideEmptyGroups = true) },
        )

        assertEquals(mapOf(PreferKey.hideEmptyGroups to true), values)
    }

    @Test
    fun `旧配置默认显示所有标签来源`() {
        val settings = emptyMap<String, Any?>().toTestPreferences().toBookshelfSettings()
        assertEquals(true, settings.bookshelfShowCustomTags)
        assertEquals(true, settings.bookshelfShowSourceTags)
        assertEquals(true, settings.bookshelfShowWordCount)
    }

    @Test
    fun `标签来源通过原子路径一起保存且不改变总开关`() {
        val values = captureAtomicUpdateValues(
            current = BookshelfSettings(),
            read = { it.toBookshelfSettings() },
            toPrefMap = BookshelfSettings::toPrefMap,
            transform = {
                it.copy(
                    bookshelfShowCustomTags = false,
                    bookshelfShowSourceTags = false,
                    bookshelfShowWordCount = false,
                )
            },
        )
        assertEquals(
            mapOf(
                PreferKey.bookshelfShowCustomTags to false,
                PreferKey.bookshelfShowSourceTags to false,
                PreferKey.bookshelfShowWordCount to false,
            ),
            values,
        )
    }
}

private fun bookshelfMappingSamples(): List<BookshelfSettings> {
    val base = BookshelfSettings(
        wenku8ArchiveShelfBookCount = 24,
        bookGroupStyle = 101,
        hideEmptyGroups = false,
        bookshelfSort = 102,
        bookshelfSortOrder = 103,
        showUnread = false,
        showUnreadNew = false,
        showTip = false,
        showBookCount = false,
        showLastUpdateTime = false,
        showBookIntro = false,
        bookshelfShowIntro = false,
        bookshelfShowTag = false,
        bookshelfShowCustomTags = false,
        bookshelfShowSourceTags = false,
        bookshelfShowWordCount = false,
        bookshelfShowLatestChapter = false,
        bookshelfIntroMaxLines = 104,
        showWaitUpCount = false,
        showBookshelfFastScroller = false,
        shouldShowExpandButton = false,
        bookshelfRefreshingLimit = 105,
        bookshelfLayoutModePortrait = 106,
        bookshelfLayoutGridPortrait = 107,
        bookshelfLayoutModeLandscape = 108,
        bookshelfLayoutGridLandscape = 109,
        bookshelfLayoutListPortrait = 110,
        bookshelfLayoutListLandscape = 111,
        bookshelfFolderLayoutModePortrait = 112,
        bookshelfFolderLayoutGridPortrait = 113,
        bookshelfFolderLayoutModeLandscape = 114,
        bookshelfFolderLayoutGridLandscape = 115,
        bookshelfFolderLayoutListPortrait = 116,
        bookshelfFolderLayoutListLandscape = 117,
        bookshelfGridLayout = 118,
        bookshelfLayoutCompact = false,
        bookshelfListCoverCenter = false,
        bookshelfListIntroBelowContent = false,
        bookshelfShowDivider = false,
        bookshelfTitleSmallFont = false,
        bookshelfTitleCenter = false,
        bookshelfTitleMaxLines = 119,
        bookshelfCoverShadow = false,
        bookshelfCardColor = 120,
        bookshelfCardColorDark = 121,
        bookshelfGroupListStyle = 122,
        bookshelfGroupCoverCount = 123,
        bookshelfListCoverWidth = 124,
        bookshelfGridCoverWidth = 125,
        bookshelfGridFixedColumns = false,
        bookshelfGridFixedCoverWidth = false,
        bookshelfSearchActionDirectToSearch = false,
        autoRefreshBook = false,
        saveTabPosition = 987654321L,
    )
    return listOf(
        base,
        base.copy(hideEmptyGroups = true),
        base.copy(wenku8ShowArchiveShelf = false),
        base.copy(showUnread = true),
        base.copy(showUnreadNew = true),
        base.copy(showTip = true),
        base.copy(showBookCount = true),
        base.copy(showLastUpdateTime = true),
        base.copy(showBookIntro = true),
        base.copy(bookshelfShowIntro = true),
        base.copy(bookshelfShowTag = true),
        base.copy(bookshelfShowCustomTags = true),
        base.copy(bookshelfShowSourceTags = true),
        base.copy(bookshelfShowWordCount = true),
        base.copy(bookshelfShowLatestChapter = true),
        base.copy(showWaitUpCount = true),
        base.copy(showBookshelfFastScroller = true),
        base.copy(shouldShowExpandButton = true),
        base.copy(bookshelfLayoutCompact = true),
        base.copy(bookshelfListCoverCenter = true),
        base.copy(bookshelfListIntroBelowContent = true),
        base.copy(bookshelfShowDivider = true),
        base.copy(bookshelfTitleSmallFont = true),
        base.copy(bookshelfTitleCenter = true),
        base.copy(bookshelfCoverShadow = true),
        base.copy(bookshelfGridFixedColumns = true),
        base.copy(bookshelfGridFixedCoverWidth = true),
        base.copy(bookshelfSearchActionDirectToSearch = true),
        base.copy(autoRefreshBook = true),
    )
}

private fun BookshelfSettings.expectedPrefMap(): Map<String, Any?> = mapOf(
    PreferKey.bookGroupStyle to bookGroupStyle,
    PreferKey.hideEmptyGroups to hideEmptyGroups,
    PreferKey.wenku8ShowArchiveShelf to wenku8ShowArchiveShelf,
    PreferKey.wenku8ArchiveShelfBookCount to wenku8ArchiveShelfBookCount,
    PreferKey.bookshelfSort to bookshelfSort,
    PreferKey.bookshelfSortOrder to bookshelfSortOrder,
    PreferKey.showUnread to showUnread,
    PreferKey.showUnreadNew to showUnreadNew,
    PreferKey.showTip to showTip,
    PreferKey.showBookCount to showBookCount,
    PreferKey.showLastUpdateTime to showLastUpdateTime,
    PreferKey.showBookIntro to showBookIntro,
    PreferKey.bookshelfShowIntro to bookshelfShowIntro,
    PreferKey.bookshelfShowTag to bookshelfShowTag,
    PreferKey.bookshelfShowCustomTags to bookshelfShowCustomTags,
    PreferKey.bookshelfShowSourceTags to bookshelfShowSourceTags,
    PreferKey.bookshelfShowWordCount to bookshelfShowWordCount,
    PreferKey.bookshelfShowLatestChapter to bookshelfShowLatestChapter,
    PreferKey.bookshelfIntroMaxLines to bookshelfIntroMaxLines,
    PreferKey.showWaitUpCount to showWaitUpCount,
    PreferKey.showBookshelfFastScroller to showBookshelfFastScroller,
    PreferKey.shouldShowExpandButton to shouldShowExpandButton,
    PreferKey.bookshelfRefreshingLimit to bookshelfRefreshingLimit,
    PreferKey.bookshelfLayoutModePortrait to bookshelfLayoutModePortrait,
    PreferKey.bookshelfLayoutGridPortrait to bookshelfLayoutGridPortrait,
    PreferKey.bookshelfLayoutModeLandscape to bookshelfLayoutModeLandscape,
    PreferKey.bookshelfLayoutGridLandscape to bookshelfLayoutGridLandscape,
    PreferKey.bookshelfLayoutListPortrait to bookshelfLayoutListPortrait,
    PreferKey.bookshelfLayoutListLandscape to bookshelfLayoutListLandscape,
    PreferKey.bookshelfFolderLayoutModePortrait to bookshelfFolderLayoutModePortrait,
    PreferKey.bookshelfFolderLayoutGridPortrait to bookshelfFolderLayoutGridPortrait,
    PreferKey.bookshelfFolderLayoutModeLandscape to bookshelfFolderLayoutModeLandscape,
    PreferKey.bookshelfFolderLayoutGridLandscape to bookshelfFolderLayoutGridLandscape,
    PreferKey.bookshelfFolderLayoutListPortrait to bookshelfFolderLayoutListPortrait,
    PreferKey.bookshelfFolderLayoutListLandscape to bookshelfFolderLayoutListLandscape,
    PreferKey.bookshelfGridLayout to bookshelfGridLayout,
    PreferKey.bookshelfLayoutCompact to bookshelfLayoutCompact,
    PreferKey.bookshelfListCoverCenter to bookshelfListCoverCenter,
    PreferKey.bookshelfListIntroBelowContent to bookshelfListIntroBelowContent,
    PreferKey.bookshelfShowDivider to bookshelfShowDivider,
    PreferKey.bookshelfTitleSmallFont to bookshelfTitleSmallFont,
    PreferKey.bookshelfTitleCenter to bookshelfTitleCenter,
    PreferKey.bookshelfTitleMaxLines to bookshelfTitleMaxLines,
    PreferKey.bookshelfCoverShadow to bookshelfCoverShadow,
    PreferKey.bookshelfCardColor to bookshelfCardColor,
    PreferKey.bookshelfCardColorDark to bookshelfCardColorDark,
    PreferKey.bookshelfGroupListStyle to bookshelfGroupListStyle,
    PreferKey.bookshelfGroupCoverCount to bookshelfGroupCoverCount,
    PreferKey.bookshelfListCoverWidth to bookshelfListCoverWidth,
    PreferKey.bookshelfGridCoverWidth to bookshelfGridCoverWidth,
    PreferKey.bookshelfGridFixedColumns to bookshelfGridFixedColumns,
    PreferKey.bookshelfGridFixedCoverWidth to bookshelfGridFixedCoverWidth,
    PreferKey.bookshelfSearchActionDirectToSearch to bookshelfSearchActionDirectToSearch,
    PreferKey.autoRefresh to autoRefreshBook,
    PreferKey.bookshelfSelectedGroupId to saveTabPosition,
)
