package io.legado.app.feature.performance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.legado.app.R
import io.legado.app.domain.model.settings.Wenku8PerformanceEffect
import io.legado.app.platform.display.RefreshRateInfo
import io.legado.app.platform.display.rememberRefreshRateInfo
import io.legado.app.ui.theme.adaptiveContentPadding
import io.legado.app.ui.widget.components.AppScaffold
import io.legado.app.ui.widget.components.SplicedColumnGroup
import io.legado.app.ui.widget.components.settingItem.*
import io.legado.app.ui.widget.components.text.AppText
import io.legado.app.ui.widget.components.topbar.GlassMediumFlexibleTopAppBar
import io.legado.app.ui.widget.components.topbar.TopBarNavigationButton
import org.koin.androidx.compose.koinViewModel

@Composable
fun PerformanceRoute(onBackClick: () -> Unit, viewModel: PerformanceViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val refresh = rememberRefreshRateInfo()
    PerformanceScreen(state, refresh, onBackClick, viewModel::onIntent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceScreen(
    state: PerformanceUiState,
    refresh: RefreshRateInfo?,
    onBackClick: () -> Unit,
    onIntent: (PerformanceIntent) -> Unit,
) {
    val theme = state.theme
    AppScaffold(topBar = {
        GlassMediumFlexibleTopAppBar(
            title = stringResource(R.string.wenku8_performance_title),
            navigationIcon = { TopBarNavigationButton(onClick = onBackClick) },
        )
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().testTag("performance-list").consumeWindowInsets(padding), contentPadding = adaptiveContentPadding(
            top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 32.dp,
        )) {
            if (state.saveFailed) item { AppText(stringResource(R.string.wenku8_save_failed)) }
            item {
                SplicedColumnGroup(title = stringResource(R.string.wenku8_navigation_visibility)) {
                    SwitchSettingItem(title = stringResource(R.string.wenku8_show_discovery), checked = state.shell.showDiscovery,
                        onCheckedChange = { onIntent(PerformanceIntent.Discovery(it)) })
                    SwitchSettingItem(title = stringResource(R.string.wenku8_show_rss), checked = state.shell.showRss,
                        onCheckedChange = { onIntent(PerformanceIntent.Rss(it)) })
                }
            }
            item {
                SplicedColumnGroup(title = stringResource(R.string.wenku8_glass_appearance)) {
                    val previewBackdrop = rememberLayerBackdrop()
                    Box(Modifier.fillMaxWidth().height(94.dp).padding(12.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Box(Modifier.matchParentSize().layerBackdrop(previewBackdrop).background(
                            androidx.compose.ui.graphics.Brush.linearGradient(listOf(
                                androidx.compose.ui.graphics.Color(0xFFB7C7B9), androidx.compose.ui.graphics.Color(0xFFF2DDC4),
                                androidx.compose.ui.graphics.Color(0xFFC1CCD4),
                            )), androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
                        ))
                        io.legado.app.core.designsystem.wenku8.dock.Wenku8GlassDock(
                            items = kotlinx.collections.immutable.persistentListOf(
                                io.legado.app.core.designsystem.wenku8.dock.Wenku8DockItem("preview", stringResource(R.string.wenku8_glass_preview), androidx.compose.material.icons.Icons.Outlined.Tune),
                            ), selectedIndex = 0, onSelected = {},
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            backdrop = previewBackdrop, reduceMotion = true,
                        )
                    }
                    SliderSettingItem(title = stringResource(R.string.wenku8_glass_blur), value = theme.wenku8GlassBlur,
                        defaultValue = 8f, valueRange = 0f..24f,
                        onValueChange = { onIntent(PerformanceIntent.Blur(it)) })
                    SliderSettingItem(title = stringResource(R.string.wenku8_glass_opacity), value = theme.wenku8GlassOpacity * 100f,
                        defaultValue = 87f, valueRange = 0f..100f, valueLabel = { "${it.toInt()}%" },
                        onValueChange = { onIntent(PerformanceIntent.Opacity(it / 100f)) })
                    ClickableSettingItem(title = stringResource(R.string.wenku8_reset_glass), onClick = { onIntent(PerformanceIntent.ResetGlass) })
                    SwitchSettingItem(title = stringResource(R.string.wenku8_reduce_motion), checked = theme.wenku8ReducedEffects,
                        onCheckedChange = { onIntent(PerformanceIntent.ReduceMotion(it)) })
                }
            }
            item {
                SplicedColumnGroup(title = stringResource(R.string.wenku8_refresh_rate)) {
                    AppText(
                        text = refresh?.let { stringResource(R.string.wenku8_refresh_current, it.current) }
                            ?: stringResource(R.string.wenku8_refresh_unavailable),
                        modifier = Modifier.padding(16.dp),
                    )
                    AppText(stringResource(R.string.wenku8_refresh_system_limit), modifier = Modifier.padding(horizontal = 16.dp))
                    val options = listOf(0, -1) + (refresh?.rates.orEmpty()) +
                        listOf(theme.wenku8RefreshRate).filter { it > 0 && it !in refresh?.rates.orEmpty() }
                    options.forEach { hz ->
                        val name = when (hz) {
                            0 -> stringResource(R.string.wenku8_refresh_auto)
                            -1 -> stringResource(R.string.wenku8_refresh_system)
                            else -> if (hz in refresh?.rates.orEmpty()) "$hz Hz"
                                else stringResource(R.string.wenku8_refresh_saved_unavailable, hz)
                        }
                        androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth()
                            .selectable(theme.wenku8RefreshRate == hz, role = androidx.compose.ui.semantics.Role.RadioButton,
                                onClick = { onIntent(PerformanceIntent.RefreshRate(hz)) })
                            .padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            androidx.compose.material3.RadioButton(selected = theme.wenku8RefreshRate == hz, onClick = null)
                            AppText(name, modifier = Modifier.padding(start = 12.dp))
                        }
                    }
                }
            }
            item {
                SplicedColumnGroup(title = stringResource(R.string.reader_refresh_rate)) {
                    AppText(stringResource(R.string.reader_refresh_summary), modifier = Modifier.padding(16.dp))
                    val options = listOf(0, -1) +
                        (listOf(30) + refresh?.rates.orEmpty() + listOf(theme.readerRefreshRate))
                            .filter { it > 0 }.distinct().sorted()
                    options.forEach { hz ->
                        val name = when (hz) {
                            0 -> stringResource(R.string.wenku8_refresh_auto)
                            -1 -> stringResource(R.string.wenku8_refresh_system)
                            else -> "$hz Hz"
                        }
                        Row(Modifier.fillMaxWidth()
                            .selectable(theme.readerRefreshRate == hz, role = androidx.compose.ui.semantics.Role.RadioButton,
                                onClick = { onIntent(PerformanceIntent.ReaderRefreshRate(hz)) })
                            .padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            androidx.compose.material3.RadioButton(selected = theme.readerRefreshRate == hz, onClick = null)
                            AppText(name, modifier = Modifier.padding(start = 12.dp))
                        }
                    }
                }
            }
            item {
                SplicedColumnGroup(title = stringResource(R.string.wenku8_low_performance)) {
                    SwitchSettingItem(title = stringResource(R.string.wenku8_low_performance), checked = theme.wenku8LowPerformance,
                        description = stringResource(R.string.wenku8_low_performance_summary),
                        onCheckedChange = { onIntent(PerformanceIntent.LowPerformance(it)) })
                    AppText(stringResource(R.string.wenku8_retained_effects), modifier = Modifier.padding(16.dp))
                    Wenku8PerformanceEffect.entries.forEach { effect ->
                        val title = when (effect) {
                            Wenku8PerformanceEffect.Glass -> R.string.wenku8_effect_glass
                            Wenku8PerformanceEffect.Blur -> R.string.wenku8_effect_blur
                            Wenku8PerformanceEffect.EdgeBlur -> R.string.wenku8_effect_edge
                            Wenku8PerformanceEffect.Motion -> R.string.wenku8_effect_motion
                            Wenku8PerformanceEffect.Shelf3d -> R.string.wenku8_effect_shelf
                        }
                        SwitchSettingItem(title = stringResource(title), enabled = theme.wenku8LowPerformance,
                            checked = theme.wenku8RetainedEffects and effect.bit != 0,
                            onCheckedChange = { onIntent(PerformanceIntent.Retain(effect, it)) })
                    }
                    ClickableSettingItem(title = stringResource(R.string.wenku8_reset_lightweight), onClick = { onIntent(PerformanceIntent.ResetLightweight) })
                }
            }
        }
    }
}
