package io.legado.app.data.repository

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import io.legado.app.help.config.ReadBookConfig
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonArray
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class ReaderComfortPresetTest {
    @Test fun `first shipped preset is the default green and keeps five protected slots`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val configs = context.assets.open("defaultData/readConfig.json").bufferedReader().use {
            GSON.fromJsonArray<ReadBookConfig.Config>(it.readText()).getOrThrow()
        }
        assertEquals("#E0EDDC", configs.first().bgStr)
        assertEquals(0, io.legado.app.domain.model.settings.ReadSettings().readStyleSelect)
        assertTrue(configs.size >= io.legado.app.domain.model.settings.READ_STYLE_PRESET_COUNT)
    }

    @Test fun `shipped preset and fallback use requested typography and margins`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val shipped = context.assets.open("defaultData/readConfig.json").bufferedReader().use {
            GSON.fromJsonArray<ReadBookConfig.Config>(it.readText()).getOrThrow().first()
        }
        for (config in listOf(shipped, ReadBookConfig.Config())) {
            assertEquals("#354231", config.getTextColor())
            assertEquals(22, config.textSize)
            assertEquals(.1f, config.letterSpacing, .0001f)
            assertEquals(.2f, (config.lineSpacingExtra - 10) / 10f, .0001f)
            assertEquals(.3f, config.paragraphSpacing / 10f, .0001f)
            assertEquals(io.legado.app.help.BUNDLED_READING_FONT, config.textFont)
            assertEquals(900, config.titleBold)
            assertTrue(config.isTitleSizeRelative())
            assertEquals(0, config.titleSize)
            assertEquals(22, config.resolvedTitleSizeSp())
            assertEquals(io.legado.app.constant.ReadTipType.tipChapterTitle, config.tipFooterLeft)
            assertEquals(listOf(6,6,16,16), listOf(config.paddingTop,config.paddingBottom,config.paddingLeft,config.paddingRight))
            assertEquals(listOf(6,6,16,16), listOf(config.headerPaddingTop,config.headerPaddingBottom,config.headerPaddingLeft,config.headerPaddingRight))
            assertEquals(listOf(6,6,16,16), listOf(config.footerPaddingTop,config.footerPaddingBottom,config.footerPaddingLeft,config.footerPaddingRight))
        }
        // Old imported absolute sizes must retain the legacy auto-detection rule.
        assertFalse(ReadBookConfig.Config(titleSize = 30).isTitleSizeRelative())
    }

    @Test fun `upgrade preserves layout custom backgrounds and all indices`() {
        val old = ReadBookConfig.Config(name = "微信读书", bgStr = "#ffc0edc6", bgType = 0, textSize = 31)
        val custom = old.copy(name = "我的排版", bgStr = "#123456")
        val upgraded = upgradeDefaultReadingBackground(listOf(old, custom))
        assertEquals(old.copy(name = "浅绿护眼", bgStr = "#E0EDDC"), upgraded.first())
        assertEquals(custom, upgraded[1])
        assertEquals(listOf(custom, old), upgradeDefaultReadingBackground(listOf(custom, old)))
        assertEquals(upgraded, upgradeDefaultReadingBackground(upgraded))
        assertEquals(listOf(old.copy(bgStr = "#ffffff")), upgradeDefaultReadingBackground(listOf(old.copy(bgStr = "#ffffff"))))
    }
}
