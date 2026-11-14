package io.legado.app.ui.book.read

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.legado.app.constant.ReadMenuBlurMode
import io.legado.app.constant.ReadMenuBlurStyle
import io.legado.app.data.repository.ReadPreferences
import io.legado.app.domain.model.settings.AppUiConfiguration
import io.legado.app.ui.theme.AppTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import splitties.init.injectAsAppCtx
import androidx.test.core.app.ApplicationProvider
import io.legado.app.help.config.AppConfigStore
import io.legado.app.domain.gateway.AppShellSettingsGateway
import io.legado.app.domain.gateway.ThemeSettingsGateway
import io.legado.app.data.repository.AppShellSettingsRepository
import io.legado.app.data.repository.ThemeSettingsRepository
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
class ReaderToolbarReadabilityTest {
    @get:Rule val compose = createComposeRule()
    @Before fun setup() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        app.injectAsAppCtx()
        AppConfigStore.init(app)
        stopKoin()
        startKoin { modules(module {
            single<AppShellSettingsGateway> { AppShellSettingsRepository() }
            single<ThemeSettingsGateway> { ThemeSettingsRepository() }
        }) }
    }
    @After fun teardown() { stopKoin() }


    @Test fun `settings is visible on first page and bottom panels occlude contrasting book text`() {
        var slots by mutableIntStateOf(1)
        var background by mutableStateOf(Color.White)
        var dark by mutableStateOf(false)
        var route by mutableStateOf<ReadBookMenuRoute>(ReadBookMenuRoute.Main)
        val intents = mutableListOf<ReadBookIntent>()
        val seek = MutableStateFlow(ReadSeekUiState())
        compose.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true, LocalDensity provides Density(1f)) {
                AppTheme(AppUiConfiguration(), darkTheme = dark) {
                    Box(Modifier.requiredSize(390.dp, 844.dp).background(background).testTag("reader-toolbar-scene")) {
                        Column { repeat(40) { Text("正文 BODY TEXT 正文 BODY TEXT", color = if (background == Color.White) Color.Black else Color.White) } }
                        ReadBookMenuBar(
                            state = ReadBookUiState(
                                menuState = ReadBookMenuState(visible = true, routeStack = persistentListOf(ReadBookMenuRoute.Main, route)),
                                menuConfig = ReadMenuConfig(
                                    readMenuIconItemsPerRow = slots, readSliderMode = "1", readMenuBlurAlpha = 0,
                                    readMenuBottomBarBlurMode = ReadMenuBlurMode.LiquidGlass,
                                    readMenuBottomBarBlurStyle = ReadMenuBlurStyle.Progressive,
                                    readMenuBottomBarLiquidGlassButtons = true,
                                    bottomBarButtons = persistentListOf(
                                        ReadBookButtonConfigItem("search", true), ReadBookButtonConfigItem("auto_page", true),
                                        ReadBookButtonConfigItem("catalog", true), ReadBookButtonConfigItem("addBookmark", true),
                                        ReadBookButtonConfigItem("read_aloud", true), ReadBookButtonConfigItem("setting", true),
                                    ),
                                ),
                            ),
                            preferences = ReadPreferences(), eyeProtectionActive = false,
                            onIntent = { intents += it }, onBrightnessPreview = {}, seekState = seek,
                        )
                    }
                }
            }
        }
        compose.onNodeWithContentDescription("设置").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(ReadBookIntent.OpenReadMenuRoute(ReadBookMenuRoute.ReadStyle), intents.last()); slots = 5 }
        for (night in listOf(false, true)) for (current in listOf(ReadBookMenuRoute.Main, ReadBookMenuRoute.ReadStyle)) {
            compose.runOnIdle { dark = night; route = current; background = Color.White }
            val lightBook = compose.onNodeWithTag("reader-toolbar-scene").captureToImage().asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, false)
            compose.runOnIdle { background = Color.Black }
            val darkBook = compose.onNodeWithTag("reader-toolbar-scene").captureToImage().asAndroidBitmap()
            // The center strip is inside the bottom panel for both toolbar and expanded settings.
            for (y in 776..790) for (x in 185..205) assertEquals(lightBook.getPixel(x, y), darkBook.getPixel(x, y))
            val dir = File(checkNotNull(System.getProperty("wenku8.readerToolbarRenderDir")))
            dir.mkdirs()
            File(dir, "${if (night) "dark" else "light"}-${if (current == ReadBookMenuRoute.Main) "toolbar" else "settings"}.png")
                .outputStream().use { darkBook.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
