package io.legado.app.core.designsystem.wenku8.chrome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.legado.app.R
import io.legado.app.core.designsystem.wenku8.effects.archiveReveal

val LocalWenku8NavigationOrdinal = staticCompositionLocalOf<Int?> { null }

internal fun numberedArchiveCode(code: String, ordinal: Int?): String =
    if (ordinal == null) code else "%02d / %s".format(java.util.Locale.ROOT, ordinal, code.replaceFirst(Regex("^\\d+\\s*/\\s*"), ""))

@Composable
fun wenku8ArchiveCode(title: String): String = when (title) {
    stringResource(R.string.home) -> stringResource(R.string.wenku8_home_code)
    stringResource(R.string.bookshelf) -> stringResource(R.string.wenku8_shelf_code)
    stringResource(R.string.discovery) -> stringResource(R.string.wenku8_discovery_code)
    stringResource(R.string.bookmark), stringResource(R.string.all_bookmark) -> stringResource(R.string.wenku8_bookmarks_code)
    stringResource(R.string.rss) -> stringResource(R.string.wenku8_rss_code)
    stringResource(R.string.my) -> stringResource(R.string.wenku8_my_code)
    stringResource(R.string.setting) -> stringResource(R.string.wenku8_settings_code)
    else -> stringResource(R.string.wenku8_archive_label)
}

/** Shared archive masthead; the owning screen retains search, selection and all actions. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Wenku8TopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    bottomContent: @Composable (ColumnScope.() -> Unit)? = null,
    archiveCode: String? = null,
) {
    val code = numberedArchiveCode(archiveCode ?: wenku8ArchiveCode(title), LocalWenku8NavigationOrdinal.current)
    val all = stringResource(R.string.all)
    val contextLabel = subtitle?.takeIf { it.isNotBlank() && it != all && it != title }
    Column(modifier.background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            expandedHeight = 56.dp,
            title = {
                Column {
                    Text(
                        title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 23.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.7).sp,
                        ),
                    )
                    Row {
                    Text(
                        code,
                        modifier = Modifier.weight(1f).clearAndSetSemantics {}.archiveReveal(start = .16f, end = .88f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace, fontSize = 9.sp,
                            lineHeight = 12.sp, letterSpacing = 1.05.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (contextLabel != null) Text(contextLabel,
                        modifier = Modifier.widthIn(max = 96.dp).padding(start = 8.dp),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            navigationIcon = navigationIcon,
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        )
        HorizontalDivider(Modifier.padding(horizontal = 22.dp).archiveReveal(start = 0f, end = .46f), thickness = .5.dp,
            color = MaterialTheme.colorScheme.outlineVariant)
        bottomContent?.invoke(this)
    }
}
