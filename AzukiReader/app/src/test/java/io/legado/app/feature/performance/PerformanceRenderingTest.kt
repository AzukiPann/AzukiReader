package io.legado.app.feature.performance

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.legado.app.domain.model.settings.AppUiConfiguration
import io.legado.app.ui.theme.AppTheme
import kotlinx.collections.immutable.persistentListOf
import io.legado.app.platform.display.RefreshRateInfo
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
class PerformanceRenderingTest {
    @get:Rule val compose = createComposeRule()

    @Test fun `performance page exposes persisted navigation controls and renders at compact and expanded widths`() {
        var state by mutableStateOf(PerformanceUiState(shell = io.legado.app.domain.model.settings.AppShellSettings(showDiscovery = true, showRss = true)))
        var dark by mutableStateOf(false)
        var width by mutableStateOf(400.dp)
        val intents = mutableListOf<PerformanceIntent>()
        compose.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true, LocalDensity provides Density(1f)) {
                AppTheme(AppUiConfiguration(theme = state.theme), darkTheme = dark) {
                    Box(Modifier.requiredSize(width, 900.dp).testTag("performance-scene")) {
                        PerformanceScreen(state, RefreshRateInfo(120f, persistentListOf(60, 120)), {}, { intents += it })
                    }
                }
            }
        }
        compose.onNodeWithText("显示发现").assertIsDisplayed().performClick()
        compose.onNodeWithText("显示订阅").performClick()
        compose.runOnIdle {
            assertEquals(listOf(PerformanceIntent.Discovery(false), PerformanceIntent.Rss(false)), intents)
            state = state.copy(shell = state.shell.copy(showDiscovery = false, showRss = false))
        }
        compose.onNode(hasText("显示发现") and isToggleable()).assertIsOff()
        compose.onNode(hasText("显示订阅") and isToggleable()).assertIsOff()
        listOf(400.dp to false, 400.dp to true, 800.dp to false).forEach { (newWidth, newDark) ->
            compose.runOnIdle { width = newWidth; dark = newDark }
            val bitmap = compose.onNodeWithTag("performance-scene").captureToImage().asAndroidBitmap()
            assertEquals(newWidth.value.toInt(), bitmap.width)
            System.getProperty("wenku8.performanceRenderDir")?.let { directory ->
                File(directory).mkdirs()
                File(directory, "performance-${newWidth.value.toInt()}-${if (newDark) "dark" else "light"}.png").outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            }
        }
        compose.onNodeWithTag("performance-list").performScrollToNode(hasText("30 Hz"))
        compose.onNodeWithText("30 Hz").assertIsDisplayed()
        compose.onNode(hasText("30 Hz") and isSelectable()).assertIsSelected().performClick()
        compose.runOnIdle { assertEquals(PerformanceIntent.ReaderRefreshRate(30), intents.last()) }
    }
}
