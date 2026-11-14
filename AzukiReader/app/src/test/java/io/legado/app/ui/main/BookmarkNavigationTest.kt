package io.legado.app.ui.main

import io.legado.app.domain.model.settings.AppShellSettings
import io.legado.app.core.designsystem.wenku8.chrome.numberedArchiveCode
import org.junit.Assert.*
import org.junit.Test

class BookmarkNavigationTest {
    @Test fun `default visible navigation includes independent bookmarks with continuous ordinals`() {
        val settings = AppShellSettings()
        assertFalse(settings.showDiscovery)
        assertFalse(settings.showRss)
        val visible = MainDestination.ordered(settings.mainNavigationOrder).filterNot {
            it == MainDestination.Explore || it == MainDestination.Rss
        }
        assertEquals(listOf("home", "bookshelf", "bookmarks", "my"), visible.map { it.route })
        assertEquals("04 / PERSONAL ARCHIVE", numberedArchiveCode("05 / PERSONAL ARCHIVE", visible.indexOf(MainDestination.My) + 1))
        assertEquals("03 / BOOKMARK ARCHIVE", numberedArchiveCode("BOOKMARK ARCHIVE", visible.indexOf(MainDestination.Bookmarks) + 1))
    }
    @Test fun `existing navigation order gains exactly one bookmark after bookshelf and reordered headers follow it`() {
        val upgraded = MainDestination.ordered("home,bookshelf,explore,rss,my")
        assertEquals(1, upgraded.count { it == MainDestination.Bookmarks })
        assertEquals(upgraded.indexOf(MainDestination.Bookshelf) + 1, upgraded.indexOf(MainDestination.Bookmarks))
        val reordered = MainDestination.ordered("my,bookmarks,bookshelf,home,rss,explore")
        assertEquals("02 / BOOKMARK ARCHIVE", numberedArchiveCode("BOOKMARK ARCHIVE", reordered.indexOf(MainDestination.Bookmarks) + 1))
        assertEquals("01 / PERSONAL ARCHIVE", numberedArchiveCode("05 / PERSONAL ARCHIVE", reordered.indexOf(MainDestination.My) + 1))
        assertEquals("READING ARCHIVE", numberedArchiveCode("READING ARCHIVE", null))
    }
}
