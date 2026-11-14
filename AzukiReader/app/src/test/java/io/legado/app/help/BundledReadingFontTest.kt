package io.legado.app.help

import android.app.Application
import splitties.init.injectAsAppCtx
import android.graphics.Typeface
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BundledReadingFontTest {
    @Test fun `default reader font is rendered with the bundled face`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        context.injectAsAppCtx()
        val actual = io.legado.app.feature.reader.platform.ReaderAndroidPaintFactory.loadTypeface(BUNDLED_READING_FONT, 400, false)
        val expected = Typeface.create(Typeface.createFromAsset(context.assets, "font/fz_jingkai.ttf"), 400, false)
        val actualPaint = android.graphics.Paint().apply { typeface = actual; textSize = 22f }
        val expectedPaint = android.graphics.Paint().apply { typeface = expected; textSize = 22f }
        assertEquals(expectedPaint.measureText("Azuk1P4N 阅读楷体"), actualPaint.measureText("Azuk1P4N 阅读楷体"), .001f)
    }

    @Test fun `offline scanner exposes real bundled font without folder grants and keeps it reusable`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        File(context.filesDir, "bundled-fonts").deleteRecursively()
        val first = scanFontFiles(context, null)
        val font = first.fontFiles.single { it.name == "方正精楷简体.ttf" }
        assertEquals("楷体", fontDisplayName(font.name))
        assertEquals("自选字体.ttf", fontDisplayName("自选字体.ttf"))
        val file = bundledReadingFont(context)!!
        assertEquals(5197860L, file.length())
        val packaged = context.assets.open("font/fz_jingkai.ttf").use { it.readBytes() }
        val hash = MessageDigest.getInstance("SHA-256")
        assertArrayEquals(hash.digest(packaged), hash.digest(file.readBytes()))
        assertNotNull(Typeface.createFromAsset(context.assets, "font/fz_jingkai.ttf"))
        assertEquals(FontFolderRead.NotConfigured, first.folder)
        val modified = file.lastModified()
        assertEquals(font.name, scanFontFiles(context, null).fontFiles.single { it.name == font.name }.name)
        assertEquals(modified, file.lastModified())
    }
}
