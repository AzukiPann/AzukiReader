package io.legado.app.core.designsystem.wenku8.effects

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.legado.app.domain.model.settings.AppUiConfiguration
import io.legado.app.ui.theme.AppTheme
import io.legado.app.ui.widget.components.text.AppText
import io.legado.app.core.designsystem.wenku8.chrome.Wenku8TopBar
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35], qualifiers = "zh-rCN-w400dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Wenku8PageMotionTest {
    @get:Rule val compose = createComposeRule()

    private fun advance(frames: Int) {
        repeat(frames) { compose.mainClock.advanceTimeByFrame(); compose.waitForIdle() }
    }

    private fun snapshot(tag: String, name: String): Bitmap {
        val bitmap = compose.onNodeWithTag(tag).captureToImage().asAndroidBitmap()
        System.getProperty("wenku8.effectsRenderDir")?.let { directory ->
            File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        return bitmap
    }

    @Test fun `default all subtitle never replaces archive code and named group remains accessible`() {
        var subtitle by mutableStateOf("全部")
        compose.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                AppTheme(AppUiConfiguration(), darkTheme = false) {
                    Box(Modifier.requiredSize(360.dp, 100.dp).testTag("header")) {
                        Wenku8TopBar("发现", subtitle = subtitle)
                    }
                }
            }
        }
        compose.onNodeWithText("全部").assertDoesNotExist()
        compose.onNodeWithText("发现").assertExists()
        snapshot("header", "header-discovery-code")
        compose.runOnIdle { subtitle = "收藏分组" }
        compose.onNodeWithText("收藏分组").assertExists()
    }

    @Test fun `text assembles progressively without changing its measured size`() {
        val clock = mutableFloatStateOf(0f)
        compose.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true, LocalDensity provides Density(1f)) {
                AppTheme(AppUiConfiguration(), darkTheme = false) {
                    CompositionLocalProvider(LocalArchiveReveal provides clock) {
                        Box(Modifier.requiredSize(300.dp, 64.dp).background(Color.White).testTag("text")) {
                            AppText("READING ARCHIVE / 逐渐出现的文字", color = Color.Black)
                        }
                    }
                }
            }
        }
        fun ink(name: String): Int {
            val bitmap = snapshot("text", name)
            assertEquals(300, bitmap.width)
            assertEquals(64, bitmap.height)
            return (0 until bitmap.width).sumOf { x ->
                (0 until bitmap.height).count { y -> android.graphics.Color.red(bitmap.getPixel(x, y)) < 200 }
            }
        }
        assertEquals(0, ink("text-start"))
        compose.runOnIdle { clock.floatValue = .14f }
        val early = ink("text-early")
        compose.runOnIdle { clock.floatValue = .35f }
        val middle = ink("text-middle")
        compose.runOnIdle { clock.floatValue = 1f }
        val full = ink("text-complete")
        assertTrue("early text must be partially revealed", early > 0)
        assertTrue("more text must appear over time: $early, $middle, $full", early < middle && middle < full)
    }

    @Test fun `main planes expose both sheets then settle and retain page state`() {
        lateinit var pager: PagerState
        var reduced by mutableStateOf(false)
        compose.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true, LocalDensity provides Density(1f)) {
                AppTheme(AppUiConfiguration(), darkTheme = false) {
                    CompositionLocalProvider(LocalWenku8Effects provides Wenku8Effects(!reduced, !reduced, !reduced)) {
                        pager = rememberPagerState { 2 }
                        Wenku8MainPager(pager, { it }, remember { object : NestedScrollConnection {} },
                            Modifier.requiredSize(320.dp, 240.dp).testTag("planes")) { page ->
                            var count by remember { mutableIntStateOf(0) }
                            Box(Modifier.fillMaxSize().background(if (page == 0) Color.Red else Color.Green)) {
                                Text("Page $page / $count", Modifier.clickable { count++ }.padding(20.dp))
                            }
                        }
                    }
                }
            }
        }
        compose.onNodeWithText("Page 0 / 0").performClick()
        compose.onNodeWithText("Page 0 / 1").assertExists()
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { pager.requestScrollToPage(1) }
        advance(5)
        val frame = snapshot("planes", "page-diagonal-middle")
        val colors = (0 until frame.width).flatMap { x -> (40 until frame.height).map { y -> frame.getPixel(x, y) } }
        assertTrue("outgoing sheet remains underneath", colors.any { android.graphics.Color.red(it) > 240 && android.graphics.Color.green(it) < 10 })
        assertTrue("incoming sheet reveals over the outgoing sheet", colors.any { android.graphics.Color.green(it) > 240 && android.graphics.Color.red(it) < 10 })
        compose.onNodeWithText("Page 0 / 1").assertDoesNotExist()
        advance(40)
        assertEquals(android.graphics.Color.GREEN, snapshot("planes", "page-diagonal-landed").getPixel(200, 180))
        compose.onNodeWithText("Page 1 / 0").performClick()
        compose.runOnIdle { pager.requestScrollToPage(0) }
        advance(40)
        compose.onNodeWithText("Page 0 / 1").assertExists()
        compose.runOnIdle { reduced = true; pager.requestScrollToPage(1) }
        advance(4)
        assertEquals(android.graphics.Color.GREEN, snapshot("planes", "page-reduced").getPixel(200, 180))
        compose.onNodeWithText("Page 1 / 1").assertExists()
    }

    @Test fun `rtl horizontal release keeps logical navigation direction`() {
        lateinit var pager: PagerState
        compose.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true, LocalLayoutDirection provides LayoutDirection.Rtl) {
                AppTheme(AppUiConfiguration(), darkTheme = false) {
                    pager = rememberPagerState { 2 }
                    Wenku8MainPager(pager, { it }, remember { object : NestedScrollConnection {} },
                        Modifier.requiredSize(320.dp, 240.dp).testTag("swipe")) { page ->
                        Box(Modifier.fillMaxSize()) { Text("Destination $page") }
                    }
                }
            }
        }
        compose.onNodeWithTag("swipe").performTouchInput { swipeRight() }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(1, pager.currentPage) }
        compose.onNodeWithText("Destination 1").assertExists()
        compose.onNodeWithText("Destination 0").assertDoesNotExist()
    }
}
