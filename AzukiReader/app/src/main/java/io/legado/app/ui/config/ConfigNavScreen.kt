package io.legado.app.ui.config

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import io.legado.app.ui.theme.LegadoTheme
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import io.legado.app.R
import io.legado.app.ui.theme.adaptiveContentPadding
import io.legado.app.ui.widget.components.AppScaffold
import io.legado.app.ui.widget.components.icon.AppIcons
import io.legado.app.ui.widget.components.SplicedColumnGroup
import io.legado.app.ui.widget.components.settingItem.ClickableSettingItem
import io.legado.app.ui.widget.components.topbar.GlassMediumFlexibleTopAppBar
import io.legado.app.ui.widget.components.topbar.GlassTopAppBarDefaults
import io.legado.app.ui.widget.components.topbar.TopBarNavigationButton
import io.legado.app.ui.widget.components.topbar.TopBarActionButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigNavScreen(
    onBackClick: () -> Unit,
    onNavigateToOther: () -> Unit,
    onNavigateToRead: () -> Unit,
    onNavigateToManga: () -> Unit,
    onNavigateToReadAloud: () -> Unit,
    onNavigateToCover: () -> Unit,
    onNavigateToTheme: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToAi: () -> Unit,
    onNavigateToDownloadCache: () -> Unit,
    onNavigateToTranslation: () -> Unit,
    onNavigateToLab: () -> Unit,
    onNavigateToPrivate: () -> Unit,
    onNavigateToBubble: () -> Unit,
    onSearchSettings: () -> Unit,
    onNavigateToPerformance: () -> Unit = {},
) {
    val scrollBehavior = GlassTopAppBarDefaults.defaultScrollBehavior()

    AppScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            GlassMediumFlexibleTopAppBar(
                title = stringResource(R.string.setting),
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    TopBarNavigationButton(onClick = onBackClick)
                },
                actions = {
                    TopBarActionButton(
                        onClick = dropUnlessResumed(block = onSearchSettings),
                        imageVector = AppIcons.Search,
                        contentDescription = stringResource(R.string.settings_search_title),
                    )
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = adaptiveContentPadding(
                top = paddingValues.calculateTopPadding(),
                bottom = 120.dp
            )
        ) {
            item {
                SplicedColumnGroup {
                    ClickableSettingItem(
                        title = stringResource(R.string.wenku8_performance_title),
                        imageVector = Icons.Outlined.Tune,
                        onClick = onNavigateToPerformance,
                    )
                    ClickableSettingItem(
                        title = stringResource(R.string.theme_setting),
                        imageVector = if (LegadoTheme.isWenku8) Icons.Outlined.Palette else null,
                        onClick = onNavigateToTheme
                    )
                    ClickableSettingItem(
                        title = stringResource(R.string.other_setting),
                        imageVector = if (LegadoTheme.isWenku8) Icons.Outlined.Tune else null,
                        onClick = onNavigateToOther
                    )
                    ClickableSettingItem(
                        title = stringResource(R.string.read_config),
                        imageVector = if (LegadoTheme.isWenku8) Icons.Outlined.MenuBook else null,
                        onClick = onNavigateToRead
                    )
                    ClickableSettingItem(
                        title = stringResource(R.string.manga_config),
                        imageVector = if (LegadoTheme.isWenku8) Icons.Outlined.CollectionsBookmark else null,
                        onClick = onNavigateToManga
                    )
                    ClickableSettingItem(
                        title = stringResource(R.string.aloud_config),
                        imageVector = if (LegadoTheme.isWenku8) Icons.Outlined.VolumeUp else null,
                        description = stringResource(R.string.multi_role_rule_summary),
                        onClick = onNavigateToReadAloud
                    )
                    ClickableSettingItem(
                        title = stringResource(R.string.cover_config),
                        imageVector = if (LegadoTheme.isWenku8) Icons.Outlined.Image else null,
                        onClick = onNavigateToCover
                    )
                    ClickableSettingItem(
                        title = stringResource(R.string.bubble_config),
                        imageVector = if (LegadoTheme.isWenku8) Icons.Outlined.ChatBubbleOutline else null,
                        description = stringResource(R.string.bubble_config_entry_summary),
                        onClick = onNavigateToBubble
                    )
                    ClickableSettingItem(
                        title = stringResource(R.string.download_cache_config),
                        imageVector = if (LegadoTheme.isWenku8) Icons.Outlined.CloudDownload else null,
                        onClick = onNavigateToDownloadCache
                    )
                    ClickableSettingItem(
                        title = stringResource(R.string.backup_restore),
                        imageVector = if (LegadoTheme.isWenku8) Icons.Outlined.Backup else null,
                        onClick = onNavigateToBackup
                    )
                    ClickableSettingItem(
                        title = stringResource(R.string.ai_config),
                        imageVector = if (LegadoTheme.isWenku8) Icons.Outlined.AutoAwesome else null,
                        onClick = onNavigateToAi
                    )
                    ClickableSettingItem(
                        title = stringResource(R.string.translation_config),
                        imageVector = if (LegadoTheme.isWenku8) Icons.Outlined.Translate else null,
                        onClick = onNavigateToTranslation
                    )
                    ClickableSettingItem(
                        title = stringResource(R.string.privacy),
                        imageVector = if (LegadoTheme.isWenku8) Icons.Outlined.Lock else null,
                        onClick = onNavigateToPrivate
                    )
                    ClickableSettingItem(
                        title = stringResource(R.string.lab_setting),
                        imageVector = if (LegadoTheme.isWenku8) Icons.Outlined.Science else null,
                        onClick = onNavigateToLab
                    )
                }
            }
        }
    }
}
