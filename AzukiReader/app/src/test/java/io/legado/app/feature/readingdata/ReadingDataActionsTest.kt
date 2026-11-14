package io.legado.app.feature.readingdata

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import io.legado.app.R
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

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35], qualifiers = "zh-rCN-w1000dp-h1200dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReadingDataActionsTest {
    @get:Rule val compose = createComposeRule()
    @Test fun `actions remain usable at compact and expanded widths and disable during transfer`() {
        var busy by mutableStateOf(false)
        var width by mutableStateOf(320.dp)
        var dark by mutableStateOf(false)
        var imports = 0
        var exports = 0
        compose.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                AppTheme(AppUiConfiguration(), darkTheme = dark) {
                    Column(Modifier.requiredSize(width, 260.dp).padding(16.dp).testTag("reading-data")) {
                        ReadingDataActions(busy, R.string.reading_data_imported, { imports++ }, { exports++ })
                    }
                }
            }
        }
        compose.onNodeWithText("导入阅读数据").assertIsDisplayed().performClick()
        compose.onNodeWithText("导出阅读数据").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, imports); assertEquals(1, exports); busy = true }
        compose.onNodeWithText("导入阅读数据").assertIsNotEnabled()
        compose.onNodeWithText("导出阅读数据").assertIsNotEnabled()
        compose.onNodeWithText("正在处理阅读数据…").assertIsDisplayed()
        listOf(320.dp to false, 800.dp to true).forEach { (w, d) ->
            compose.runOnIdle { width = w; dark = d; busy = false }
            compose.onNodeWithText("导入阅读数据").assertIsDisplayed()
            compose.onNodeWithText("导出阅读数据").assertIsDisplayed()
            System.getProperty("wenku8.readingDataRenderDir")?.let { directory ->
                File(directory).mkdirs()
                val image = compose.onNodeWithTag("reading-data").captureToImage().asAndroidBitmap()
                File(directory, "reading-data-${w.value.toInt()}-${if (d) "dark" else "light"}.png")
                    .outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
        }
    }
}
