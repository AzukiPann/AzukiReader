package io.legado.app.platform.display

import android.app.Application
import android.view.Display
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class RefreshRatePolicyTest {
    @Test fun `static reading returns to reading rate after settings and bookmarks close`() {
        val reading = io.legado.app.ui.book.read.ReadBookUiState()
        assertFalse(reading.interactiveOverlayVisible)
        assertTrue(reading.copy(menuState = io.legado.app.ui.book.read.ReadBookMenuState(visible = true)).interactiveOverlayVisible)
        val sheet = io.legado.app.ui.book.read.ReadBookSheet.MoreConfig
        assertTrue(reading.withSheet(sheet).interactiveOverlayVisible)
        assertFalse(reading.withSheet(sheet).withSheet(null).interactiveOverlayVisible)
    }
    private fun mode(id: Int, width: Int, height: Int, rate: Float): Display.Mode =
        Display.Mode::class.java.getDeclaredConstructor(Int::class.javaPrimitiveType, Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType, Float::class.javaPrimitiveType).newInstance(id, width, height, rate)

    @Test fun `reading requests 30 even without a physical mode while system releases request`() {
        val current = mode(1, 1080, 2400, 120f)
        val requested = selectRefreshMode(arrayOf(current), current, 30)
        assertNull(requested)
        assertEquals(30f, requestedRefreshRate(requested, 30, true))
        assertEquals(0f, requestedRefreshRate(requested, 30, false))
        assertEquals(0f, requestedRefreshRate(null, -1, true))
        assertEquals(120f, requestedRefreshRate(current, 0, true))
    }

    @Test fun `automatic selects highest rate without changing resolution and system clears request`() {
        val current = mode(1, 1080, 2400, 60f)
        val fast = mode(2, 1080, 2400, 120f)
        val otherResolution = mode(3, 720, 1600, 144f)
        val modes = arrayOf(current, fast, otherResolution)
        assertEquals(2, selectRefreshMode(modes, current, 0)?.modeId)
        assertEquals(1, selectRefreshMode(modes, current, 60)?.modeId)
        assertNull(selectRefreshMode(modes, current, -1))
        assertNull(selectRefreshMode(modes, current, 90))
        assertNull(selectRefreshMode(modes, current, 144))
    }
}
