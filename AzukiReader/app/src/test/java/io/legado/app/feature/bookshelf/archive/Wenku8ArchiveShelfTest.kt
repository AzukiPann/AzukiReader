package io.legado.app.feature.bookshelf.archive

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.legado.app.core.designsystem.wenku8.Wenku8Design
import io.legado.app.core.designsystem.wenku8.chrome.Wenku8ArchiveSection
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35], qualifiers = "w1000dp-h1000dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Wenku8ArchiveShelfTest {
    @get:Rule val compose = createComposeRule()

    @Test fun `fractional dragging does not place cover content again and only neighbors load`() {
        var placements = 0
        val loaded = mutableSetOf<Int>()
        compose.setContent {
            MaterialTheme {
                Box(Modifier.requiredSize(360.dp, 500.dp)) {
                    Wenku8ArchiveShelf(1000, 500, { it }, { "Book $it" }, {}) { index ->
                        SideEffect { loaded += index }
                        Box(Modifier.fillMaxSize().testTag("moving-cover-$index").onPlaced { placements++ })
                    }
                }
            }
        }
        compose.runOnIdle { assertEquals(setOf(499, 500, 501), loaded) }
        val shelf = compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions))
        shelf.performTouchInput { down(center); moveBy(Offset(-25f, 0f)) }
        compose.waitForIdle()
        val before = compose.onNodeWithTag("moving-cover-500", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.left
        compose.runOnIdle { placements = 0 }
        repeat(5) {
            shelf.performTouchInput { moveBy(Offset(-.5f, 0f)) }
            compose.waitForIdle()
        }
        compose.runOnIdle { assertEquals("Sub-lane motion must update layers without placing covers", 0, placements) }
        val after = compose.onNodeWithTag("moving-cover-500", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.left
        assertTrue("The gesture must actually move the cover", kotlin.math.abs(after - before) > .01f)
        shelf.performTouchInput { up() }
    }

    @Test fun `long title and absent author keep the same shelf height`() {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(360.dp)) {
                    Wenku8ArchiveShelf(2, 0, { it },
                        { if (it == 0) "Short" else "A very long title that wraps onto two lines and is truncated at its end" },
                        {}, Modifier.testTag("fixed-shelf"), author = { if (it == 0) "" else "Author" }) {
                        Box(Modifier.fillMaxSize())
                    }
                }
            }
        }
        val before = compose.onNodeWithTag("fixed-shelf").fetchSemanticsNode().boundsInRoot.height
        val actions = compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions))
            .fetchSemanticsNode().config[SemanticsActions.CustomActions]
        compose.runOnIdle { actions[1].action() }
        compose.waitForIdle()
        assertEquals(before, compose.onNodeWithTag("fixed-shelf").fetchSemanticsNode().boundsInRoot.height, .1f)
    }

    @Test fun `central book invokes original action and a thousand books stay lazy`() {
        var opened = -1
        val composed = mutableSetOf<Int>()
        compose.setContent {
            MaterialTheme {
                Box(Modifier.requiredSize(360.dp, 480.dp)) {
                    Wenku8ArchiveShelf(1000, 500, { "book-$it" }, { "Spine $it" }, {}) { index ->
                        SideEffect { composed += index }
                        TextButton(onClick = { opened = index }, modifier = Modifier.testTag("book-$index")) { Text("Open $index") }
                    }
                }
            }
        }
        compose.onNodeWithTag("book-500").performClick()
        compose.runOnIdle { assertEquals(500, opened); assertTrue(composed.size < 20) }
    }

    @Test fun `accessible next and previous navigate the bank without opening a book`() {
        var settled = -1
        compose.setContent {
            MaterialTheme {
                Box(Modifier.requiredSize(360.dp, 480.dp)) {
                    Wenku8ArchiveShelf(12, 4, { it }, { "Spine $it" }, { settled = it }) { Text("Book $it") }
                }
            }
        }
        val node = compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions))
        val actions = node.fetchSemanticsNode().config[SemanticsActions.CustomActions]
        compose.runOnIdle { assertTrue(actions[1].action()) }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(5, settled) }
        compose.runOnIdle { assertTrue(actions[0].action()) }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(4, settled) }
    }

    @Test fun `archive bank renders in dark light and expanded windows`() {
        var dark by mutableStateOf(false)
        var expanded by mutableStateOf(false)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                MaterialTheme(colorScheme = if (dark) Wenku8Design.darkScheme else Wenku8Design.lightScheme) {
                    Column(Modifier.requiredSize(if (expanded) 800.dp else 360.dp, 500.dp)
                        .background(MaterialTheme.colorScheme.background).testTag("archive-scene")) {
                        Wenku8ArchiveSection("阅读档案", "SHELF / 003", Modifier.padding(horizontal = 22.dp))
                        Wenku8ArchiveShelf(18, 8, { it }, { "档案 $it" }, {}, Modifier.weight(1f)) { index ->
                            Column(Modifier.width(128.dp)) {
                                Box(Modifier.fillMaxWidth().height(180.dp).background(Color(0xFF9BAFA7)))
                                Text("档案 $index", modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }
        listOf(false to false, true to false, false to true).forEach { (night, wide) ->
            compose.runOnIdle { dark = night; expanded = wide }
            val bitmap = compose.onNodeWithTag("archive-scene").captureToImage().asAndroidBitmap()
            assertEquals(if (wide) 800 else 360, bitmap.width)
            assertEquals(500, bitmap.height)
            System.getProperty("wenku8.effectsRenderDir")?.let { dir ->
                File(dir, "shelf-${if (night) "dark" else "light"}${if (wide) "-expanded" else ""}.png").outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            }
        }
    }
}
