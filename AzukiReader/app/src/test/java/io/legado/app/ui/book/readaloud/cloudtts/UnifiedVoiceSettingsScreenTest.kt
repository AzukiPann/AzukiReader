package io.legado.app.ui.book.readaloud.cloudtts

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import io.legado.app.domain.model.settings.AppUiConfiguration
import io.legado.app.ui.theme.AppTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import splitties.init.injectAsAppCtx
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35], qualifiers = "zh-rCN-w480dp-h1000dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class UnifiedVoiceSettingsScreenTest {
    @get:Rule val compose = createComposeRule()

    @Before fun initializeApplication() {
        RuntimeEnvironment.getApplication().injectAsAppCtx()
    }

    @Test fun selectingUsesVoiceAndDeletionRequiresExplicitConfirmation() {
        var state by mutableStateOf(CloudTtsUiState(loading = false,
            voices = persistentListOf(CloudTtsVoiceItemUi("v", "海星", "DeepSeek", true, true)),
            engines = persistentListOf(CloudTtsEngineItemUi("engine", "DeepSeek", "DeepSeek 语音"))))
        val intents = mutableListOf<CloudTtsIntent>()
        compose.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                AppTheme(AppUiConfiguration()) {
                    Box(Modifier.requiredSize(400.dp, 900.dp)) {
                        CloudTtsScreen(state, { intent ->
                            intents += intent
                            when (intent) {
                                is CloudTtsIntent.DeleteEngine -> state = state.copy(activeDialog =
                                    CloudTtsDialog.DeleteEngine(intent.id, "DeepSeek", false))
                                CloudTtsIntent.DismissError -> state = state.copy(activeDialog = null)
                                else -> Unit
                            }
                        }, emptyFlow(), {})
                    }
                }
            }
        }
        compose.onNodeWithText("海星").performClick()
        compose.runOnIdle { assertEquals(CloudTtsIntent.UseVoice("v"), intents.last()) }
        compose.runOnIdle { state = state.copy(selectedTab = CloudTtsTab.Engines) }
        compose.onNodeWithContentDescription("删除").performClick()
        compose.onNodeWithText("确认删除引擎？").assertExists()
        compose.runOnIdle { assertFalse(intents.contains(CloudTtsIntent.ConfirmDeleteEngine)) }
        compose.onNodeWithText("取消").performClick()
        compose.onNodeWithText("确认删除引擎？").assertDoesNotExist()
        compose.onNodeWithContentDescription("删除").performClick()
        compose.onNodeWithText("删除", useUnmergedTree = true).performClick()
        compose.runOnIdle { assertEquals(1, intents.count { it == CloudTtsIntent.ConfirmDeleteEngine }) }
    }
}
