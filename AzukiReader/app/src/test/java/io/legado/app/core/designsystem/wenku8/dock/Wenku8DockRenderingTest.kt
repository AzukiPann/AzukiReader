package io.legado.app.core.designsystem.wenku8.dock

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import io.legado.app.core.designsystem.wenku8.Wenku8Design
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Offscreen component evidence, not a claim of application or device visual acceptance. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35], qualifiers = "w400dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Wenku8DockRenderingTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `committed selection recoils once and reduced motion stays still`() {
        var selected by mutableStateOf(0)
        var reduced by mutableStateOf(false)
        val items = persistentListOf(
            Wenku8DockItem("home", "Discover", Icons.Outlined.Home),
            Wenku8DockItem("shelf", "Bookshelf", Icons.AutoMirrored.Outlined.MenuBook),
            Wenku8DockItem("settings", "Settings", Icons.Outlined.Settings),
        )
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                MaterialTheme(colorScheme = Wenku8Design.lightScheme) {
                    Box(Modifier.requiredSize(360.dp, 200.dp).background(Color.Magenta).testTag("recoil-scene")) {
                        Wenku8GlassDock(items, selected, { selected = it },
                            Modifier.align(Alignment.Center).width(196.dp), reduceMotion = reduced)
                    }
                }
            }
        }
        fun center(name: String): Float {
            val bitmap = compose.onNodeWithTag("recoil-scene").captureToImage().asAndroidBitmap()
            System.getProperty("wenku8.renderDir")?.let { dir ->
                File(dir, "dock-recoil-$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
            val occupied = (0 until bitmap.width).filter { x ->
                val pixel = bitmap.getPixel(x, 100)
                android.graphics.Color.green(pixel) > 100
            }
            assertTrue("capsule must be visible", occupied.isNotEmpty())
            return (occupied.first() + occupied.last()) / 2f
        }
        val resting = center("rest")
        compose.runOnIdle { selected = 2 }
        fun advance(frames: Int) {
            repeat(frames) {
                compose.mainClock.advanceTimeByFrame()
                compose.waitForIdle()
            }
        }
        advance(3)
        compose.onNodeWithContentDescription("Settings").assertIsSelected()
        advance(4)
        assertTrue("switch must visibly displace the whole capsule", center("peak") > resting + 1f)
        advance(40)
        assertEquals(resting, center("settled"), .5f)
        advance(40)
        assertEquals(resting, center("idle"), .5f)
        compose.runOnIdle { reduced = true; selected = 0 }
        advance(7)
        assertEquals(resting, center("reduced"), .5f)
    }

    @Test
    fun `glass dock renders in both color schemes with a sampled background`() {
        var dark by mutableStateOf(false)
        val items = persistentListOf(
            Wenku8DockItem("home", "发现", Icons.Outlined.Home),
            Wenku8DockItem("shelf", "书架", Icons.AutoMirrored.Outlined.MenuBook),
            Wenku8DockItem("settings", "设置", Icons.Outlined.Settings),
        )
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                MaterialTheme(colorScheme = if (dark) Wenku8Design.darkScheme else Wenku8Design.lightScheme) {
                    val backdrop = rememberLayerBackdrop { drawContent() }
                    Box(Modifier.requiredSize(360.dp, 200.dp).testTag("scene")) {
                        Column(Modifier.fillMaxSize().layerBackdrop(backdrop).background(MaterialTheme.colorScheme.background)) {
                            Row(Modifier.padding(22.dp).height(156.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                listOf(Color(0xFFACBBB7), Color(0xFFBBA999), Color(0xFFC3C3B1)).forEach { color ->
                                    Box(Modifier.weight(1f).height(156.dp).background(color))
                                }
                            }
                        }
                        Wenku8DockUnderlay(backdrop, Modifier.align(Alignment.BottomCenter))
                        Wenku8GlassDock(items, 1, {}, Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp).width(196.dp), backdrop, reduceMotion = true)
                    }
                }
            }
        }
        listOf(false, true).forEach { night ->
            compose.runOnIdle { dark = night }
            compose.onNodeWithContentDescription("书架").assertIsDisplayed().assertIsSelected()
            val bitmap = compose.onNodeWithTag("scene").captureToImage().asAndroidBitmap()
            assertEquals(360, bitmap.width)
            assertEquals(200, bitmap.height)
            System.getProperty("wenku8.renderDir")?.let { dir ->
                File(dir, if (night) "dock-dark.png" else "dock-light.png").outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            }
        }
    }
}
