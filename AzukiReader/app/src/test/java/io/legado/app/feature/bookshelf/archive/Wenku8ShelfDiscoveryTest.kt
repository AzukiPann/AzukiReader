package io.legado.app.feature.bookshelf.archive

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.legado.app.core.designsystem.wenku8.wenku8ArchiveBackdrop
import io.legado.app.core.designsystem.wenku8.Wenku8Design
import io.legado.app.core.designsystem.wenku8.effects.LocalWenku8Effects
import io.legado.app.core.designsystem.wenku8.effects.Wenku8Effects
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35], qualifiers = "zh-rCN-w1000dp-h1200dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Wenku8ShelfDiscoveryTest {
    @get:Rule val compose = createComposeRule()

    @Test fun `entrance assembles for 720 milliseconds then leaves an idle scene`() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                Box(Modifier.requiredSize(390.dp, 700.dp).background(Color.White).testTag("animated-bank")) {
                    Wenku8ArchiveShelf(10, 3, { it }, { "Volume $it" }, {}) {
                        Box(Modifier.fillMaxSize().background(Color(0xFF8EAAA0)))
                    }
                }
            }
        }
        compose.mainClock.advanceTimeBy(32)
        val initial = compose.onNodeWithTag("animated-bank").captureToImage().asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, false)
        compose.mainClock.advanceTimeBy(360)
        val middle = compose.onNodeWithTag("animated-bank").captureToImage().asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, false)
        compose.mainClock.advanceTimeBy(500)
        val complete = compose.onNodeWithTag("animated-bank").captureToImage().asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, false)
        compose.mainClock.advanceTimeBy(1200)
        val idle = compose.onNodeWithTag("animated-bank").captureToImage().asAndroidBitmap()
        assertFalse(initial.sameAs(middle))
        assertFalse(middle.sameAs(complete))
        assertTrue(complete.sameAs(idle))
    }

    @Test fun `cyclic navigation crosses both ends and opens the selected original book`() {
        var selected = -1
        var opened = -1
        compose.setContent {
            CompositionLocalProvider(LocalWenku8Effects provides Wenku8Effects(false, false, false)) {
                MaterialTheme {
                    Box(Modifier.requiredSize(360.dp, 600.dp)) {
                        Wenku8ArchiveShelf(3, 2, { it }, { "Volume $it" }, { selected = it },
                            onOpen = { opened = it }) { Box(Modifier.fillMaxSize().background(Color.Gray)) }
                    }
                }
            }
        }
        val node = compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions))
        val actions = node.fetchSemanticsNode().config[SemanticsActions.CustomActions]
        compose.runOnIdle { actions[1].action() }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(0, selected) }
        compose.onNodeWithText("Volume 0").performClick()
        compose.runOnIdle { assertEquals(0, opened); actions[0].action() }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(2, selected) }
    }

    @Test fun `single volume stays still and lightweight carousel remains usable`() {
        var selected = -1
        var opened = -1
        compose.setContent {
            CompositionLocalProvider(LocalWenku8Effects provides Wenku8Effects(false, false, false, shelf3d = false)) {
                MaterialTheme {
                    Box(Modifier.requiredSize(360.dp, 500.dp)) {
                        Wenku8ArchiveShelf(1, 0, { it }, { "Single volume" }, { selected = it },
                            onOpen = { opened = it }) { Text("Cover") }
                    }
                }
            }
        }
        compose.onAllNodesWithText("NO.001").assertCountEquals(1)
        val actions = compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions))
            .fetchSemanticsNode().config[SemanticsActions.CustomActions]
        compose.runOnIdle { actions[1].action() }
        compose.waitForIdle()
        compose.onNodeWithText("Single volume").performClick()
        compose.runOnIdle { assertEquals(0, selected); assertEquals(0, opened) }
    }

    @Test fun `discovery components render together and hiding shelf preserves catalogue and sorting`() {
        var show by mutableStateOf(true)
        var dark by mutableStateOf(false)
        var width by mutableStateOf(390)
        var flat by mutableStateOf(false)
        var sortRequests = 0
        var opened = -1
        var details = -1
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f),
                LocalWenku8Effects provides Wenku8Effects(false, false, false, shelf3d = !flat)) {
                MaterialTheme(colorScheme = if (dark) Wenku8Design.darkScheme else Wenku8Design.lightScheme) {
                    LazyVerticalGrid(GridCells.Adaptive(340.dp),
                        modifier = Modifier.requiredSize(width.dp, 844.dp).background(MaterialTheme.colorScheme.background).wenku8ArchiveBackdrop(MaterialTheme.colorScheme.onSurface).testTag("discovery-scene"),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Wenku8ShelfDiscoveryHeader(show, "最近阅读", { sortRequests++ }) {
                                Wenku8ArchiveShelf(10, 3, { it }, { "轻小说档案 $it" }, {},
                                    modifier = Modifier.testTag("gallery"), author = { "档案作者" }) { index ->
                                    Box(Modifier.fillMaxSize().background(Color(0xFF8EAAA0))) { Text("BOOK $index") }
                                }
                            }
                        }
                        items(20) { index ->
                            Wenku8ShelfCatalogueRow("书籍 $index", "作者 $index", "FILE / %03d".format(index + 1),
                                "正在阅读的章节", order = index, onClick = { opened = index }, onLongClick = { details = index }) {
                                Box(it.background(Color(0xFF8EAAA0)))
                            }
                        }
                    }
                }
            }
        }
        listOf("compact-light", "compact-dark", "expanded", "flat", "hidden").forEach { variant ->
            compose.runOnIdle {
                dark = variant == "compact-dark"
                width = if (variant == "expanded") 800 else 390
                flat = variant == "flat"
                show = variant != "hidden"
            }
            if (variant == "compact-dark") {
                listOf("轻小说档案 3", "书籍列表", "书籍 0").forEach { label ->
                    val layout = mutableListOf<TextLayoutResult>()
                    compose.onNodeWithText(label, useUnmergedTree = true)
                        .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layout) }
                    assertEquals(Wenku8Design.darkScheme.onSurface, layout.single().layoutInput.style.color)
                }
            }
            val bitmap = compose.onNodeWithTag("discovery-scene").captureToImage().asAndroidBitmap()
            assertEquals(if (variant == "expanded") 800 else 390, bitmap.width)
            assertEquals(844, bitmap.height)
            System.getProperty("wenku8.shelfRenderDir")?.let { dir ->
                File(dir).mkdirs()
                File(dir, "$variant.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
        }
        compose.onNodeWithTag("gallery").assertDoesNotExist()
        compose.onNodeWithText("书籍列表").assertIsDisplayed()
        compose.onNodeWithText("最近阅读").performClick()
        compose.onNodeWithText("书籍 0").performClick()
        compose.onNodeWithText("书籍 1").performSemanticsAction(SemanticsActions.OnLongClick)
        compose.runOnIdle { assertEquals(1, sortRequests); assertEquals(0, opened); assertEquals(1, details) }
    }
}
