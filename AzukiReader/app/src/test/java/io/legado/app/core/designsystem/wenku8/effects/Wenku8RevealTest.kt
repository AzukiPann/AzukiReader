package io.legado.app.core.designsystem.wenku8.effects

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Wenku8RevealTest {
    @get:Rule val compose = createComposeRule()

    @Test fun `waiting for data never hides existing content`() {
        compose.setContent {
            Wenku8RevealScope("waiting", ready = false) {
                Box(Modifier.size(40.dp).archiveReveal().background(Color.Red).testTag("content"))
            }
        }
        assertEquals(Color.Red, compose.onNodeWithTag("content").captureToImage().toPixelMap()[10, 10])
    }

    @Test fun `reduced effects show complete content immediately`() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalWenku8Effects provides Wenku8Effects(false, false, false)) {
                Wenku8RevealScope("reduced") {
                    Box(Modifier.size(40.dp).archiveReveal(7).background(Color.Red).testTag("content"))
                }
            }
        }
        assertEquals(Color.Red, compose.onNodeWithTag("content").captureToImage().toPixelMap()[10, 10])
    }

    @Test fun `finite reveal does not replay on unrelated recomposition`() {
        var version by mutableIntStateOf(0)
        compose.setContent {
            Wenku8RevealScope("same-page") {
                Box(Modifier.size((40 + version).dp).archiveReveal(3).background(Color.Red).testTag("content"))
            }
        }
        compose.waitForIdle()
        compose.runOnIdle { version = 1 }
        assertEquals(Color.Red, compose.onNodeWithTag("content").captureToImage().toPixelMap()[10, 10])
    }

    @Test fun `paper deformation lands flat at both endpoints`() {
        for (row in listOf(0f, .2f, .5f, 1f)) {
            assertEquals(0f, ArchivePaperGeometry.depth(0f, row), .0001f)
            assertEquals(0f, ArchivePaperGeometry.depth(1f, row), .0001f)
        }
        assertTrue(kotlin.math.abs(ArchivePaperGeometry.depth(.5f, .3f)) > 1f)
    }
}
