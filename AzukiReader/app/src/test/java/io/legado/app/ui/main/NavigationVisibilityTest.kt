package io.legado.app.ui.main

import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationVisibilityTest {
    @Test fun `hiding preceding tabs keeps my destination selected`() {
        val visible = listOf(MainDestination.Home, MainDestination.Bookshelf, MainDestination.My)
        assertEquals(2, preservedDestinationIndex(MainDestination.My, visible))
        assertEquals(1, preservedDestinationIndex(MainDestination.Bookshelf, visible))
    }
    @Test fun `hiding the active destination returns to bookshelf and restoring respects order`() {
        val visible = listOf(MainDestination.My, MainDestination.Bookshelf, MainDestination.Home)
        assertEquals(1, preservedDestinationIndex(MainDestination.Rss, visible))
        val restored = MainDestination.ordered("my,rss,bookshelf,explore,home")
        assertEquals(0, preservedDestinationIndex(MainDestination.My, restored))
        assertEquals(2, preservedDestinationIndex(MainDestination.Bookshelf, restored))
    }
}
