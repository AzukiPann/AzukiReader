package io.legado.app.ui.book.readRecord

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import io.legado.app.domain.model.settings.AppUiConfiguration
import io.legado.app.ui.theme.AppTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35], qualifiers = "zh-rCN-w400dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReadRecordProgressLayoutTest {
    @get:Rule val compose = createComposeRule()
    @Test fun `both labels share style and wrapped titles keep a hanging indent`() {
        compose.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                AppTheme(AppUiConfiguration(), darkTheme = false) {
                    Box(Modifier.width(200.dp).testTag("progress")) {
                        ReadRecordProgressLabels(ReadRecordProgressUi(2, 0.9, 17, 0.42,
                            highestChapterTitle = "这是一个足够长的最高章节标题用于确认换行对齐",
                            totalChapters = 120, currentChapterTitle = "回到故乡", currentTotalChapters = 120))
                    }
                }
            }
        }
        val current = compose.onNodeWithText("回到故乡", substring = true)
        val highest = compose.onNodeWithText("这是一个足够长", substring = true)
        val currentResults = mutableListOf<TextLayoutResult>()
        val highestResults = mutableListOf<TextLayoutResult>()
        current.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(currentResults) }
        highest.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(highestResults) }
        val layout = highestResults.single()
        assertTrue(layout.lineCount >= 2)
        assertEquals(layout.getLineLeft(0), layout.getLineLeft(1), 0.01f)
        assertEquals(current.fetchSemanticsNode().boundsInRoot.left, highest.fetchSemanticsNode().boundsInRoot.left, 0.01f)
        assertEquals(currentResults.single().layoutInput.style, layout.layoutInput.style)
        assertTrue(highest.fetchSemanticsNode().boundsInRoot.left >
            compose.onNodeWithText("最高：").fetchSemanticsNode().boundsInRoot.left)
        System.getProperty("reading.daily.renderDir")?.let { folder ->
            File(folder).mkdirs()
            File(folder, "progress-labels.png").outputStream().use {
                compose.onNodeWithTag("progress").captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }
}
