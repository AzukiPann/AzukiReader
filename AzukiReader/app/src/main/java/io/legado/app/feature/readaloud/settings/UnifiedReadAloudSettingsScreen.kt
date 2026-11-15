package io.legado.app.feature.readaloud.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.legado.app.R
import io.legado.app.feature.readaloud.wenku8.Wenku8ListeningRoute
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.widget.components.AppScaffold
import io.legado.app.ui.widget.components.modalBottomSheet.AppModalBottomSheet
import io.legado.app.ui.widget.components.settingItem.ClickableSettingItem
import io.legado.app.ui.widget.components.topbar.GlassMediumFlexibleTopAppBar
import io.legado.app.ui.widget.components.topbar.TopBarActionButton
import io.legado.app.ui.widget.components.topbar.TopBarNavigationButton

/** Settings and the reader render the same controls and use the same gateway. */
@Composable
fun UnifiedReadAloudSettingsScreen(
    onBack: () -> Unit,
    onEngines: () -> Unit,
    onVoicePool: () -> Unit,
    onBgmPool: () -> Unit,
    onVoiceEffect: () -> Unit,
    onCapsuleStyle: () -> Unit,
    onRecognition: () -> Unit,
    onRegexCast: () -> Unit,
) {
    var advanced by remember { mutableStateOf(false) }
    AppScaffold(
        contentColor = LegadoTheme.colorScheme.onSurface,
        topBar = {
            GlassMediumFlexibleTopAppBar(
                title = stringResource(R.string.aloud_config),
                navigationIcon = { TopBarNavigationButton(onClick = onBack) },
                actions = {
                    TopBarActionButton(onClick = { advanced = true }, imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.aloud_unified_advanced))
                },
            )
        },
    ) { padding ->
        Wenku8ListeningRoute(reader = false, onDismiss = onBack, onOfflineSettings = onEngines,
            modifier = Modifier.fillMaxSize().padding(padding), showHeader = false)
    }
    AppModalBottomSheet(show = advanced, onDismissRequest = { advanced = false },
        title = stringResource(R.string.aloud_unified_advanced)) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            fun open(action: () -> Unit) { advanced = false; action() }
            ClickableSettingItem(title = stringResource(R.string.cast_voice_pool), onClick = { open(onVoicePool) })
            ClickableSettingItem(title = stringResource(R.string.cast_bgm_pool), onClick = { open(onBgmPool) })
            ClickableSettingItem(title = stringResource(R.string.voice_effect), onClick = { open(onVoiceEffect) })
            ClickableSettingItem(title = stringResource(R.string.capsule_style_title), onClick = { open(onCapsuleStyle) })
            ClickableSettingItem(title = stringResource(R.string.multi_role_recognition), onClick = { open(onRecognition) })
            ClickableSettingItem(title = stringResource(R.string.regex_cast_rule), onClick = { open(onRegexCast) })
        }
    }
}
