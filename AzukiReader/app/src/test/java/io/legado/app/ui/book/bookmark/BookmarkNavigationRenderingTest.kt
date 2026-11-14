package io.legado.app.ui.book.bookmark

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.legado.app.core.designsystem.wenku8.chrome.LocalWenku8NavigationOrdinal
import io.legado.app.core.designsystem.wenku8.dock.*
import io.legado.app.domain.model.settings.AppUiConfiguration
import io.legado.app.ui.theme.AppTheme
import io.legado.app.ui.widget.components.topbar.TopBarActionButton
import kotlinx.collections.immutable.persistentListOf
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
class BookmarkNavigationRenderingTest {
    @get:Rule val compose = createComposeRule()
    @Test fun `root bookmark destination has export action and its own dock tab in both themes`() {
        var dark by mutableStateOf(false)
        var width by mutableStateOf(400.dp)
        var exports = 0
        var selected = -1
        compose.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true, LocalDensity provides Density(1f)) {
                AppTheme(AppUiConfiguration(), darkTheme = dark) {
                    CompositionLocalProvider(LocalWenku8NavigationOrdinal provides 3) {
                        Box(Modifier.requiredSize(width, 800.dp).testTag("bookmarks-scene")) {
                            AllBookmarkScreen(BookmarkUiState(isLoading = false), {}, { _, _ -> }, {},
                                embeddedInNavigation = true,
                                exportAllAction = { TopBarActionButton(
                                    onClick = { exports++ }, imageVector = Icons.Outlined.FileDownload,
                                    contentDescription = "导出全部摘录为 Markdown",
                                ) })
                            Wenku8GlassDock(items = persistentListOf(
                                Wenku8DockItem("home", "首页", Icons.Outlined.Home),
                                Wenku8DockItem("bookshelf", "书架", Icons.AutoMirrored.Outlined.MenuBook),
                                Wenku8DockItem("bookmarks", "书签", Icons.Outlined.BookmarkBorder),
                                Wenku8DockItem("my", "设置", Icons.Outlined.Settings),
                            ), selectedIndex = 2, onSelected = { selected = it },
                                modifier = Modifier.align(Alignment.BottomCenter).width(280.dp).padding(bottom = 16.dp), reduceMotion = true)
                        }
                    }
                }
            }
        }
        compose.onNodeWithContentDescription("导出全部摘录为 Markdown").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, exports) }
        compose.onNodeWithContentDescription("书签").assertIsSelected().performClick()
        compose.runOnIdle { assertEquals(2, selected) }
        listOf(400.dp to false, 400.dp to true, 800.dp to false).forEach { (newWidth, newDark) ->
            compose.runOnIdle { dark = newDark; width = newWidth }
            val bitmap = compose.onNodeWithTag("bookmarks-scene").captureToImage().asAndroidBitmap()
            System.getProperty("wenku8.bookmarkRenderDir")?.let { directory ->
                File(directory).mkdirs()
                File(directory, "bookmarks-${newWidth.value.toInt()}-${if (newDark) "dark" else "light"}.png").outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            }
        }
    }
}
