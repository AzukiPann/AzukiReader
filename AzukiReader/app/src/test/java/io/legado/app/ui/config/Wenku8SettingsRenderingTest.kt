package io.legado.app.ui.config

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Actual settings destination rendered offscreen, not device acceptance. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35], qualifiers = "zh-rCN-w400dp-h900dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Wenku8SettingsRenderingTest {
    @get:Rule val compose = createComposeRule()

    @Test fun `settings destination has separate paper cards and preserves navigation in both themes`() {
        var dark by mutableStateOf(false)
        var clicks = 0
        compose.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true, LocalDensity provides Density(1f)) {
                AppTheme(AppUiConfiguration(), darkTheme = dark) {
                    Box(Modifier.requiredSize(400.dp, 900.dp).testTag("settings-scene")) {
                        ConfigNavScreen(
                            onBackClick = {}, onNavigateToOther = {}, onNavigateToRead = {},
                            onNavigateToManga = {}, onNavigateToReadAloud = {}, onNavigateToCover = {},
                            onNavigateToTheme = { clicks++ }, onNavigateToBackup = {}, onNavigateToAi = {},
                            onNavigateToDownloadCache = {}, onNavigateToTranslation = {}, onNavigateToLab = {},
                            onNavigateToPrivate = {}, onNavigateToBubble = {}, onSearchSettings = {},
                        )
                    }
                }
            }
        }
        listOf(false, true).forEach { night ->
            compose.runOnIdle { dark = night }
            val cards = compose.onAllNodes(hasClickAction() and hasText("外观"))
            cards[0].assertIsDisplayed().performClick()
            val bitmap = compose.onNodeWithTag("settings-scene").captureToImage().asAndroidBitmap()
            System.getProperty("wenku8.effectsRenderDir")?.let { dir ->
                File(dir, if (night) "settings-dark.png" else "settings-light.png").outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            }
        }
        compose.runOnIdle { assertEquals(2, clicks) }
    }
}
