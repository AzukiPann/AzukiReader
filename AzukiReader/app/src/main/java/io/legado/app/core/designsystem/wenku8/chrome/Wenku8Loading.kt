package io.legado.app.core.designsystem.wenku8.chrome

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.legado.app.R

/** Static archive planes: loading does not run a perpetual animation clock. */
@Composable
fun Wenku8Loading(modifier: Modifier = Modifier, progress: Float? = null) {
    val label = stringResource(R.string.loading)
    val ink = MaterialTheme.colorScheme.onSurface
    val value = progress?.coerceIn(0f, 1f)
    Canvas(modifier.size(48.dp, 32.dp).semantics {
        contentDescription = label
        liveRegion = LiveRegionMode.Polite
        progressBarRangeInfo = value?.let { ProgressBarRangeInfo(it, 0f..1f) }
            ?: ProgressBarRangeInfo.Indeterminate
    }) {
        val line = size.height / 7f
        for (i in 0..2) drawRoundRect(ink.copy(alpha = .12f),
            Offset(0f, i * line * 2.3f), Size(size.width * (1f - i * .18f), line), CornerRadius(1.dp.toPx()))
        if (value != null) drawRect(ink, Offset(0f, size.height - line), Size(size.width * value, line))
    }
}
