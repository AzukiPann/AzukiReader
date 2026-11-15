package io.legado.app.feature.readaloud.wenku8

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
import io.legado.app.domain.gateway.*
import io.legado.app.domain.model.settings.AppUiConfiguration
import io.legado.app.ui.theme.AppTheme
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
@Config(application=Application::class,sdk=[35],qualifiers="zh-rCN-w1000dp-h1200dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Wenku8ListeningPanelTest {
    @get:Rule val compose=createComposeRule()
    @Test fun transportVoicesOfflineSettingsAndResponsiveRendering() {
        var dark by mutableStateOf(false)
        var width by mutableStateOf(400.dp)
        var state by mutableStateOf(ListeningUiState(playback=Wenku8ListeningState(
            bookName="测试书名",chapterIndex=2,chapterTitle="第三章",active=true,paused=false,
            currentText="这是当前正在朗读的正文。",segmentCount=10,clipPositionMs=1200,clipDurationMs=5200,
            voices=persistentListOf(ListeningVoice("voice","晓晓 · 中文女声","edge",true)),
        )))
        val intents=mutableListOf<ListeningIntent>()
        var offlineSettings=0
        compose.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true,LocalDensity provides Density(1f)) {
                AppTheme(AppUiConfiguration(),darkTheme=dark) {
                    Box(Modifier.requiredSize(width,1000.dp).testTag("listening-scene")) {
                        Wenku8ListeningPanel(state,{intents+=it},{},{offlineSettings++})
                    }
                }
            }
        }
        compose.onNodeWithText("朗读设置").assertExists()
        compose.onNodeWithText("听书").assertDoesNotExist()
        compose.onNodeWithContentDescription("暂停听书").performClick()
        compose.runOnIdle { assertEquals(ListeningIntent.PlayPause,intents.last()) }
        for ((w,d) in listOf(400.dp to false,400.dp to true,800.dp to false)) {
            compose.runOnIdle {width=w;dark=d}
            val bitmap=compose.onNodeWithTag("listening-scene").captureToImage().asAndroidBitmap()
            assertEquals(w.value.toInt(),bitmap.width)
            val pixel=bitmap.getPixel(5,5)
            val light=android.graphics.Color.red(pixel)+android.graphics.Color.green(pixel)+android.graphics.Color.blue(pixel)
            assertTrue("Panel must provide an opaque theme background",if(d)light<384 else light>384)
            System.getProperty("wenku8.listeningRenderDir")?.let { path ->
                File(path).mkdirs()
                File(path,"listening-${w.value.toInt()}-${if(d)"dark" else "light"}.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
            }
        }
        compose.onNodeWithTag("listening-panel").performScrollToNode(hasTestTag("listening-voice-picker"))
        compose.onNodeWithTag("listening-voice-picker").performClick()
        compose.onNode(hasText("晓晓 · 中文女声 · edge") and isSelectable()).assertIsSelected().performClick()
        compose.runOnIdle { assertEquals(ListeningIntent.Voice("voice"),intents.last()) }
        compose.onNodeWithTag("listening-panel").performScrollToNode(hasTestTag("listening-manage-voices"))
        compose.onNodeWithTag("listening-manage-voices").performClick()
        compose.runOnIdle { assertEquals(1,offlineSettings) }
        compose.onNodeWithText("自定义").assertDoesNotExist()

    }
    @Test fun embeddedSettingsHaveOneHostTitleAndExposeSleepAndFollowActions() {
        val intents=mutableListOf<ListeningIntent>()
        compose.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true,LocalDensity provides Density(1f)) {
                AppTheme(AppUiConfiguration()) {
                    Box(Modifier.requiredSize(360.dp,640.dp)) {
                        Wenku8ListeningPanel(ListeningUiState(playback=Wenku8ListeningState(bookName="测试书籍")),
                            {intents+=it},{},{},showHeader=false)
                    }
                }
            }
        }
        compose.onNodeWithText("朗读设置").assertDoesNotExist()
        compose.onNodeWithText("自定义").assertDoesNotExist()
        compose.onNodeWithTag("listening-panel").performScrollToNode(hasContentDescription("跟随朗读段落"))
        compose.onNodeWithContentDescription("跟随朗读段落").performClick()
        compose.runOnIdle { assertEquals(ListeningIntent.Follow(false),intents.last()) }
        compose.onNodeWithTag("listening-panel").performScrollToNode(hasText("15 分钟"))
        compose.onNodeWithText("15 分钟").performClick()
        compose.runOnIdle { assertEquals(ListeningIntent.Sleep(15),intents.last()) }
    }

}
