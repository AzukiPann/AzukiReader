package io.legado.app.core.designsystem.wenku8.effects

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import io.legado.app.domain.model.settings.AppUiConfiguration
import io.legado.app.ui.theme.AppTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35], qualifiers = "w400dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Wenku8NavigationContinuityTest {
    @get:Rule val compose = createComposeRule()
    private fun advance() { compose.mainClock.advanceTimeByFrame(); compose.waitForIdle() }

    @Test fun `a newly selected page starts hidden rather than painting a completed frame before reset`() {
        lateinit var pager: PagerState
        val seen = mutableListOf<Pair<Int, Float>>()
        compose.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                AppTheme(AppUiConfiguration(), darkTheme = false) {
                    pager = rememberPagerState { 3 }
                    Wenku8MainPager(pager, { it }, remember { object : NestedScrollConnection {} },
                        Modifier.requiredSize(320.dp, 240.dp).testTag("pages")) { page ->
                        val p = LocalArchiveReveal.current!!.value
                        val active = pager.currentPage == page
                        SideEffect { if (active) seen.add(page to p) }
                        Box(Modifier.fillMaxSize().background(listOf(Color.Red, Color.Green, Color.Yellow)[page]))
                    }
                }
            }
        }
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { seen.clear(); pager.requestScrollToPage(1) }
        repeat(3) { advance() }
        compose.runOnIdle { assertEquals("Incoming page was painted complete before its reset", 0f, seen.first { it.first == 1 }.second) }
        // Retarget before the first animation finishes. The cancelled clock cannot reset the new page.
        compose.runOnIdle { seen.clear(); pager.requestScrollToPage(2) }
        repeat(3) { advance() }
        compose.runOnIdle { assertEquals(0f, seen.first { it.first == 2 }.second) }
        repeat(30) { advance() }
        val bitmap = compose.onNodeWithTag("pages").captureToImage().asAndroidBitmap()
        assertEquals(android.graphics.Color.YELLOW, bitmap.getPixel(160, 120))
    }
    @Test fun `fallback canvas follows theme and leaves a custom wallpaper visible`() {
        lateinit var pager: PagerState
        var wallpaper by mutableStateOf(true)
        var dark by mutableStateOf(false)
        compose.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                val theme = io.legado.app.domain.model.settings.ThemeSettings(
                    backgroundImageLight = if (wallpaper) "wallpaper" else null,
                    backgroundImageDark = if (wallpaper) "wallpaper" else null)
                AppTheme(AppUiConfiguration(theme = theme), darkTheme = dark, applyBackground = false) {
                    Box(Modifier.requiredSize(320.dp, 240.dp).background(Color.Blue).testTag("wallpaper")) {
                        pager = rememberPagerState { 2 }
                        Wenku8MainPager(pager, { it }, remember { object : NestedScrollConnection {} }) {
                            Box(Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }
        fun pixel() = compose.onNodeWithTag("wallpaper").captureToImage().asAndroidBitmap().getPixel(160, 120)
        assertEquals(android.graphics.Color.BLUE, pixel())
        compose.runOnIdle { pager.requestScrollToPage(1); dark = true }
        compose.waitForIdle()
        assertEquals(android.graphics.Color.BLUE, pixel())
        compose.runOnIdle { wallpaper = false }
        assertNotEquals(android.graphics.Color.BLUE, pixel())
    }

}
