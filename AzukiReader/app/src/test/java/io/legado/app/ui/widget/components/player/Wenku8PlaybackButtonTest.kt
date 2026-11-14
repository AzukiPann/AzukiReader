package io.legado.app.ui.widget.components.player

import android.app.Application
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class Wenku8PlaybackButtonTest {
    @get:Rule val compose = createComposeRule()

    @Test fun `archive playback keeps click long press and loading semantics`() {
        var clicks = 0
        var longClicks = 0
        var loading by mutableStateOf(false)
        compose.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                io.legado.app.ui.theme.AppTheme(io.legado.app.domain.model.settings.AppUiConfiguration(), darkTheme = false) {
                    AnimatedPlayPauseButton(false, "Playback", { clicks++ },
                        isLoading = loading, onLongClick = { longClicks++ })
                }
            }
        }
        val control = compose.onNodeWithContentDescription("Playback")
        control.performClick()
        control.performTouchInput { longClick() }
        compose.runOnIdle { assertEquals(1, clicks); assertEquals(1, longClicks); loading = true }
        control.assertHasClickAction().performClick()
        compose.runOnIdle { assertEquals(2, clicks) }
    }
}
