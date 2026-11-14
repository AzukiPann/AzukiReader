package io.legado.app.feature.reader.bookmarks

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import io.legado.app.domain.model.settings.AppUiConfiguration
import io.legado.app.ui.theme.AppTheme
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.Implements
import org.robolectric.annotation.Implementation
import splitties.init.injectAsAppCtx
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35], qualifiers = "zh-rCN-w900dp-h1100dp-mdpi", shadows = [HostMagnifierShadow::class])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReaderExcerptRenderingTest {
    @get:Rule val compose = createComposeRule()
    @Before fun initializeAppContext() { RuntimeEnvironment.getApplication().injectAsAppCtx() }
    private val current = "　　风从窗前吹过，书页轻轻翻动。\n\n　　他沿着小路，走向远处的"
    private val next = "灯火。\n\n　　每个平凡的日子，都藏着值得记住的片刻。"
    private fun selection() = ReaderExcerptSelection("url", "夏日来信", "作者", 1, "第二章 · 在晚风中", 100,
        100 + current.length + next.length, current, next, 100 + current.length,
        persistentListOf(SavedExcerpt(42, "旧摘录", "旧笔记")))

    private fun select(start: Int, end: Int) {
        compose.onNodeWithTag("excerpt-body").performSemanticsAction(SemanticsActions.SetSelection) { action ->
            assertTrue(action(start, end, false))
        }
    }

    @Test fun `long pressing read only book text selects a passage without changing the source`() {
        val selection = selection()
        var range by mutableStateOf(TextRange.Zero)
        compose.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                AppTheme(AppUiConfiguration(), darkTheme = false) {
                    ReaderExcerptContent(selection, range, "", { range = it }, {}, {})
                }
            }
        }
        val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        compose.onNodeWithTag("excerpt-body").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
            assertTrue(action(layouts))
        }
        val word = layouts.single().getBoundingBox(4).center
        compose.onNodeWithTag("excerpt-body").performTouchInput { longClick(word) }
        compose.runOnIdle { assertFalse(range.collapsed) }
        compose.onNodeWithTag("excerpt-body").assertTextEquals(selection.text)
    }

    @Test fun `save requires a selected passage and passes exact cross page range to host`() {
        val selection = selection()
        var saved: Pair<Int, Int>? = null
        compose.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                AppTheme(AppUiConfiguration(), darkTheme = false) {
                    ReaderExcerptSheet(true, selection, {}, { start, end, _ -> saved = start to end }, {})
                }
            }
        }
        compose.onNodeWithText("保存摘录").assertIsNotEnabled()
        select(2, current.length + 3)
        compose.onNodeWithText("保存摘录").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(2 to current.length + 3, saved) }
        compose.onNodeWithTag("excerpt-body").assertTextEquals(selection.text)
    }

    @Test fun `paper text retains selection in light dark and expanded layouts and edits saved excerpts`() {
        var selection by mutableStateOf(selection())
        var range by mutableStateOf(TextRange(2, 12))
        var dark by mutableStateOf(false)
        var width by mutableStateOf(400.dp)
        var edited: Long? = null
        compose.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                AppTheme(AppUiConfiguration(), darkTheme = dark, applyBackground = false) {
                    Box(Modifier.requiredSize(width, 780.dp).background(MaterialTheme.colorScheme.background).testTag("excerpt-preview")) {
                        ReaderExcerptContent(selection, range, "", { range = it }, {}, { edited = it })
                    }
                }
            }
        }
        compose.onNodeWithText("当前页 + 下一页").assertIsDisplayed()
        select(5, 15)
        compose.runOnIdle { assertEquals(TextRange(5, 15), range) }
        compose.onNodeWithText("已选 10 字").assertIsDisplayed()
        for ((newWidth, newDark) in listOf(400.dp to false, 400.dp to true, 760.dp to false)) {
            compose.runOnIdle { width = newWidth; dark = newDark }
            compose.onNodeWithTag("excerpt-body").assertTextEquals(selection.text)
            System.getProperty("wenku8.excerptRenderDir")?.let { directory ->
                File(directory).mkdirs()
                val bitmap = compose.onNodeWithTag("excerpt-preview").captureToImage().asAndroidBitmap()
                File(directory, "text-selection-${newWidth.value.toInt()}-${if (newDark) "dark" else "light"}.png").outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            }
        }
        compose.onNodeWithTag("excerpt-list").performScrollToNode(hasTestTag("excerpt-show-saved"))
        compose.onNodeWithTag("excerpt-show-saved").performClick()
        compose.onNodeWithTag("excerpt-list").performScrollToNode(hasTestTag("saved-excerpt-42"))
        compose.onNodeWithTag("saved-excerpt-42").performClick()
        compose.runOnIdle {
            assertEquals(42L, edited)
            range = TextRange.Zero
            selection = selection.copy(nextText = null, nextPageStart = null)
        }
        compose.onNodeWithTag("excerpt-list").performScrollToIndex(0)
        compose.onNodeWithText("当前页 + 下一页").assertDoesNotExist()
        compose.onNodeWithTag("excerpt-body").assertTextEquals(current)
    }
}

/** Robolectric has no window Surface for the platform magnifying lens; keep text gestures real. */
@Implements(android.widget.Magnifier::class)
class HostMagnifierShadow {
    @Implementation fun show(x: Float, y: Float) = Unit
    @Implementation fun show(x: Float, y: Float, lensX: Float, lensY: Float) = Unit
    @Implementation fun update() = Unit
    @Implementation fun dismiss() = Unit
}
