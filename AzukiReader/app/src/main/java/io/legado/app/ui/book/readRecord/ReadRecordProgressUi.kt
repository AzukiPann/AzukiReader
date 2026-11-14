package io.legado.app.ui.book.readRecord

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.res.stringResource
import io.legado.app.R
import io.legado.app.data.entities.readRecord.ReadRecordSession
import io.legado.app.data.entities.readRecord.furthestChapter
import io.legado.app.data.entities.readRecord.mergeFurthestProgress
import io.legado.app.domain.model.ReadingChapterReference
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.widget.components.text.AppText
import java.text.NumberFormat

@Stable
data class ReadRecordProgressUi(
    val currentChapterIndex: Long,
    val currentChapterProgress: Double?,
    val highestChapterIndex: Int,
    val highestChapterProgress: Double?,
    val highestChapterTitle: String? = null,
    val totalChapters: Int? = null,
    val currentChapterTitle: String? = null,
    val currentTotalChapters: Int? = null,
)

internal fun ReadRecordProgressUi.withChapterReference(reference: ReadingChapterReference?): ReadRecordProgressUi =
    if (reference?.chapterIndex == highestChapterIndex) copy(
        highestChapterTitle = reference.title,
        totalChapters = reference.totalChapters,
        currentChapterTitle = reference.currentChapterTitle.takeIf { reference.currentChapterIndex == currentChapterIndex },
        currentTotalChapters = reference.currentTotalChapters.takeIf { reference.currentChapterIndex == currentChapterIndex },
    ) else this

internal fun summarizeReadingProgress(sessions: List<ReadRecordSession>): ReadRecordProgressUi? {
    val latest = sessions.maxWithOrNull(compareBy<ReadRecordSession> { it.endTime }.thenBy { it.startTime }.thenBy { it.id })
        ?: return null
    val furthest = sessions.reduce { left, right -> left.mergeFurthestProgress(right) }
    return ReadRecordProgressUi(latest.words, latest.chapterProgress,
        furthest.furthestChapter, furthest.highestChapterProgress)
}

/** Used only within reading records. Callers omit it for private locked books. */
@Composable
internal fun ReadRecordProgressLabels(progress: ReadRecordProgressUi?) {
    if (progress == null) return
    val unknown = stringResource(R.string.reading_progress_unknown)
    val percent = NumberFormat.getPercentInstance().apply { maximumFractionDigits = 1 }
    val highestTitle = progress.highestChapterTitle?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.chapter_index_format, progress.highestChapterIndex.toLong() + 1)
    val currentTitle = progress.currentChapterTitle?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.chapter_index_format, progress.currentChapterIndex + 1)
    val currentMarker = "${progress.currentChapterIndex + 1}/${progress.currentTotalChapters?.toString() ?: "?"}"
    val highestMarker = "${progress.highestChapterIndex.toLong() + 1}/${progress.totalChapters?.toString() ?: "?"}"
    Column {
        ProgressRow(stringResource(R.string.reading_current_label),
            stringResource(R.string.reading_position_value, currentTitle, currentMarker,
                progress.currentChapterProgress?.let(percent::format) ?: unknown))
        ProgressRow(stringResource(R.string.reading_highest_label),
            stringResource(R.string.reading_position_value, highestTitle, highestMarker,
                progress.highestChapterProgress?.let(percent::format) ?: unknown))
    }
}

/** Separate text nodes provide a hanging indent, including with large fonts and long titles. */
@Composable
private fun ProgressRow(label: String, value: String) {
    Row {
        AppText(text = label, modifier = Modifier.widthIn(min = 36.dp),
            style = LegadoTheme.typography.labelSmall, color = LegadoTheme.colorScheme.onSurfaceVariant)
        AppText(text = value, modifier = Modifier.weight(1f),
            style = LegadoTheme.typography.labelSmall, color = LegadoTheme.colorScheme.onSurfaceVariant)
    }
}
