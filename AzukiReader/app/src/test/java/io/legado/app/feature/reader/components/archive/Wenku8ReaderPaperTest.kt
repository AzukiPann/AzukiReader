package io.legado.app.feature.reader.components.archive

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.rememberGraphicsLayer
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
class Wenku8ReaderPaperTest {
    @get:Rule val compose = createComposeRule()

    @Test fun `forward and reverse preserve endpoints and expose both pages midway`() {
        var progress by mutableFloatStateOf(0f)
        var next by mutableStateOf(true)
        compose.setContent {
            val current = rememberGraphicsLayer()
            val target = rememberGraphicsLayer()
            val redraw = remember { mutableStateOf(0L) }
            Box(Modifier.size(100.dp).testTag("paper")) {
                Wenku8ReaderPaper(next, { progress }, current, target, redraw, Color.White,
                    { Box(Modifier.fillMaxSize().background(Color.Red)) },
                    { Box(Modifier.fillMaxSize().background(Color.Blue)) })
            }
        }
        for (forward in listOf(true, false)) {
            compose.runOnIdle { next = forward; progress = 0f }
            var pixels = compose.onNodeWithTag("paper").captureToImage().toPixelMap()
            assertEquals(Color.Red, pixels[pixels.width / 2, pixels.height / 2])
            compose.runOnIdle { progress = .5f }
            pixels = compose.onNodeWithTag("paper").captureToImage().toPixelMap()
            val left = pixels[pixels.width / 8, pixels.height / 2]
            val right = pixels[pixels.width * 7 / 8, pixels.height / 2]
            assertEquals(if (forward) Color.Red else Color.Blue, left)
            assertEquals(if (forward) Color.Blue else Color.Red, right)
            compose.runOnIdle { progress = 1f }
            pixels = compose.onNodeWithTag("paper").captureToImage().toPixelMap()
            assertEquals(Color.Blue, pixels[pixels.width / 2, pixels.height / 2])
        }
    }
}
