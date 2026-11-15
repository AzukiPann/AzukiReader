package io.legado.app.ui.book.readaloud.cast

import io.legado.app.feature.settings.search.SearchableSetting
import io.legado.app.feature.settings.search.SearchableSettingsList
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.legado.app.R
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.theme.adaptiveContentPadding
import io.legado.app.ui.widget.components.AppScaffold
import io.legado.app.ui.widget.components.SplicedColumnGroup
import io.legado.app.ui.widget.components.settingItem.ClickableSettingItem
import io.legado.app.ui.widget.components.topbar.GlassMediumFlexibleTopAppBar
import io.legado.app.ui.widget.components.topbar.GlassTopAppBarDefaults
import io.legado.app.ui.widget.components.topbar.TopBarNavigationButton

/**
 * 朗读设置 hub（设置 → 朗读设置）。
 *
 * 只做菜单跳转：声音池、背景音乐池、变声器、胶囊外观、引擎、角色识别各自是独立页面。
 * 角色声音池 / 背景音乐池 / 朗读胶囊外观在阅读界面的「朗读设置」卡片里也各有一个入口
 * （角色与配乐 / 常规 tab）；这里是设置侧的完整入口，两处指向同一批页面。
 */
@Composable
fun MultiRoleRuleRouteScreen(
    onBackClick: () -> Unit,
    onNavigateToVoicePool: () -> Unit,
    onNavigateToRecognition: () -> Unit,
    onNavigateToEngines: () -> Unit,
    onNavigateToBgmPool: () -> Unit,
    onNavigateToVoiceEffect: () -> Unit,
    onNavigateToCapsuleStyle: () -> Unit,
    onNavigateToRegexCast: () -> Unit,
) {
    io.legado.app.feature.readaloud.settings.UnifiedReadAloudSettingsScreen(
        onBack = onBackClick, onEngines = onNavigateToEngines,
        onVoicePool = onNavigateToVoicePool, onBgmPool = onNavigateToBgmPool,
        onVoiceEffect = onNavigateToVoiceEffect, onCapsuleStyle = onNavigateToCapsuleStyle,
        onRecognition = onNavigateToRecognition, onRegexCast = onNavigateToRegexCast,
    )
}
