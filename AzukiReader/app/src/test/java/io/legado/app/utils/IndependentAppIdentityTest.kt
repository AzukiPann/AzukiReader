package io.legado.app.utils

import android.app.Application
import com.google.firebase.FirebaseApp
import io.legado.app.BuildConfig
import io.legado.app.constant.AppConst
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class IndependentAppIdentityTest {
    @Test fun `installed identity and providers use the independent package`() {
        val context = RuntimeEnvironment.getApplication()
        assertEquals("io.azukireader.app.debug", BuildConfig.APPLICATION_ID)
        assertEquals(BuildConfig.APPLICATION_ID, context.packageName)
        assertEquals("io.azukireader.app.debug.fileProvider", AppConst.authority)
        for (suffix in listOf("fileProvider", "readerProvider", "androidx-startup")) {
            val provider = context.packageManager.resolveContentProvider("${context.packageName}.$suffix", 0)
            assertNotNull(suffix, provider)
            assertEquals(context.packageName, provider!!.packageName)
        }
        assertNull(context.packageManager.resolveContentProvider("io.legato.kazusa.fileProvider", 0))
    }

    @Test fun `startup without upstream Firebase configuration does not initialize analytics or crash`() {
        val context = RuntimeEnvironment.getApplication()
        assertEquals(0, context.resources.getIdentifier("google_app_id", "string", context.packageName))
        FirebaseManager.setEnabled(context, true)
        assertTrue(FirebaseApp.getApps(context).isEmpty())
        FirebaseManager.setEnabled(context, false)
        assertTrue(FirebaseApp.getApps(context).isEmpty())
    }
}
