package io.legado.app.ui.book.read.sheet

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.legado.app.R
import io.legado.app.constant.ReadAloudBgMode
import io.legado.app.domain.model.AiReasoningLevel
import io.legado.app.domain.model.readaloud.ReadAloudContentSplitSetting
import io.legado.app.domain.model.readaloud.ReadAloudSplitSymbol
import io.legado.app.domain.model.settings.ReadAloudContentSplitMode
import io.legado.app.feature.readaloud.battery.ReadAloudBatteryOptimizationRoute
import io.legado.app.feature.readaloud.overlay.ReadAloudOverlayPermissionRoute
import io.legado.app.ui.book.read.ReadAloudSubPage
import io.legado.app.ui.book.read.ReadBookIntent
import io.legado.app.ui.book.read.ReadBookSheet
import io.legado.app.ui.book.read.ReadBookUiState
import io.legado.app.ui.book.readaloud.player.ReadAloudPlayerIntent
import io.legado.app.ui.book.readaloud.player.ReadAloudPlayerUiState
import io.legado.app.ui.widget.components.SectionTitle
import io.legado.app.ui.widget.components.modalBottomSheet.AppModalBottomSheet
import io.legado.app.ui.widget.components.settingItem.SliderSettingItem
import io.legado.app.ui.widget.components.settingItem.TinyClickableSettingItem
import io.legado.app.ui.widget.components.settingItem.TinyDropdownSettingItem
import io.legado.app.ui.widget.components.settingItem.TinySettingItem
import io.legado.app.ui.widget.components.settingItem.TinySwitchSettingItem
import io.legado.app.ui.widget.components.tabRow.CardTabRow
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
fun ReadAloudConfigContent(
    state: ReadBookUiState,
    playerState: ReadAloudPlayerUiState,
    onIntent: (ReadBookIntent) -> Unit,
    onPlayerIntent: (ReadAloudPlayerIntent) -> Unit,
    modifier: Modifier = Modifier,
    /**
     * true 表示内容被整页宿主承载，数值项就地铺开成滑块；
     * false（默认）表示宿主是卡片弹层，数值项继续打开选择器弹层。
     */
    asPage: Boolean = false,
    /**
     * 卡片停在哪个 tab（0=常规，1=引擎与音色，2=角色与配乐）。宿主是窗口级浮层时这份状态
     * 必须存在浮层外面：压进整屏页（引擎与音色 / 角色与配乐里那几个跳转项）会把这层
     * composition 拆掉，`rememberPagerState` 的初值回到 0，用户看到的就是「从子项进去、
     * 回来落在常规」。
     */
    selectedTab: Int = 0,
    onTabSelected: (Int) -> Unit = {},
    /**
     * 是否展示「本章背景音乐 / AI 识别背景音乐」这两个正文内浮层入口。
     *
     * 它们发出的意图由阅读器 ViewModel 的分配域处理，听书播放页是 Activity 级浮层、
     * 没有这套宿主，传 false 以免留下点了没反应的死入口。
     */
    bgmSceneActionsEnabled: Boolean = true,
    onDismiss: () -> Unit = {},
) {
    io.legado.app.feature.readaloud.wenku8.Wenku8ListeningRoute(
        reader = bgmSceneActionsEnabled, onDismiss = onDismiss,
        onOfflineSettings = { onIntent(ReadBookIntent.OpenTtsEnginesAndVoices) },
        modifier = modifier.fillMaxWidth().heightIn(max = 560.dp), showHeader = false,
    )
}

/**
 * 整页宿主用的数值项：直接铺开滑块，不再叠一层选择器弹层。
 */
@Composable
private fun ReadAloudNumberSliderItem(
    title: String,
    description: String,
    value: Int,
    defaultValue: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Int) -> Unit,
) {
    SliderSettingItem(
        title = title,
        description = description,
        value = value.toFloat(),
        defaultValue = defaultValue.toFloat(),
        valueRange = valueRange,
        onValueChange = { onValueChange(it.toInt()) },
    )
}

/**
 * 「按符号」划分方式的标点多选。
 *
 * 至少保留一个标点：全部取消会让「按符号」退化成整段，与用户刚选的划分方式矛盾。
 */
@Composable
private fun ContentSplitSymbolSettingItem(
    selectedSymbols: ImmutableSet<String>,
    onToggle: (Char, Boolean) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    TinySettingItem(
        title = stringResource(R.string.read_aloud_content_split_symbols),
        description = stringResource(R.string.read_aloud_content_split_selected_symbols) + ": " +
                selectedSymbols.joinToString(" "),
        expanded = expanded,
        onExpandChange = { expanded = it },
        expandContent = {
            ReadAloudSplitSymbol.entries.forEach { option ->
                TinySwitchSettingItem(
                    title = stringResource(symbolLabelRes(option)),
                    checked = option.symbol.toString() in selectedSymbols,
                    onCheckedChange = { onToggle(option.symbol, it) },
                )
            }
        },
    )
}

private fun symbolLabelRes(option: ReadAloudSplitSymbol): Int = when (option) {
    ReadAloudSplitSymbol.FullStop -> R.string.symbol_period
    ReadAloudSplitSymbol.Exclamation -> R.string.symbol_exclamation
    ReadAloudSplitSymbol.Question -> R.string.symbol_question
    ReadAloudSplitSymbol.Ellipsis -> R.string.symbol_ellipsis
    ReadAloudSplitSymbol.Semicolon -> R.string.symbol_semicolon
    ReadAloudSplitSymbol.Comma -> R.string.symbol_comma
    ReadAloudSplitSymbol.EnumerationComma -> R.string.symbol_enumeration_comma
    ReadAloudSplitSymbol.Colon -> R.string.symbol_colon
    ReadAloudSplitSymbol.Dot -> R.string.symbol_halfwidth_period
    ReadAloudSplitSymbol.Bang -> R.string.symbol_halfwidth_exclamation
    ReadAloudSplitSymbol.QuestionMark -> R.string.symbol_halfwidth_question
    ReadAloudSplitSymbol.HalfSemicolon -> R.string.symbol_halfwidth_semicolon
    ReadAloudSplitSymbol.HalfComma -> R.string.symbol_halfwidth_comma
    ReadAloudSplitSymbol.HalfColon -> R.string.symbol_halfwidth_colon
}

@Composable
fun ReadAloudNumberConfigSheet(
    show: Boolean,
    title: String,
    description: String,
    value: Int,
    defaultValue: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Int) -> Unit,
    onDismissRequest: () -> Unit,
) {
    AppModalBottomSheet(
        show = show,
        onDismissRequest = onDismissRequest,
        title = title,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            SliderSettingItem(
                title = title,
                description = description,
                value = value.toFloat(),
                defaultValue = defaultValue.toFloat(),
                valueRange = valueRange,
                onValueChange = { onValueChange(it.toInt()) },
            )
        }
    }
}
