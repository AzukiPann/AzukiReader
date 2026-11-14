package io.legado.app.feature.reader.bookmarks

import androidx.compose.runtime.Stable
import io.legado.app.feature.reader.core.navigation.ReaderPageContext
import kotlinx.collections.immutable.ImmutableList

@Stable
data class SavedExcerpt(val id: Long, val text: String, val note: String)

@Stable
data class ReaderExcerptSelection(
    val bookUrl: String,
    val bookName: String,
    val bookAuthor: String,
    val chapterIndex: Int,
    val chapterTitle: String,
    val pageStart: Int,
    val endPosition: Int,
    val currentText: String,
    val nextText: String?,
    val nextPageStart: Int?,
    val saved: ImmutableList<SavedExcerpt>,
) {
    val hasNextPage: Boolean get() = nextText != null
    val text: String get() = currentText + nextText.orEmpty()
}

data class SelectedExcerpt(val position: Int, val text: String)

/** Never include the next chapter or a stale/overlapping window. */
fun excerptNextPage(current: ReaderPageContext, next: ReaderPageContext?): ReaderPageContext? =
    next?.takeIf { it.chapterIndex == current.chapterIndex &&
        (it.startPosition >= current.endPosition || current.text.isBlank() && it.startPosition >= current.startPosition) &&
        it.endPosition > it.startPosition }

/** Selection offsets are UTF-16 offsets, matching Android text selection and chapter positions. */
fun selectedExcerpt(selection: ReaderExcerptSelection, start: Int, end: Int): SelectedExcerpt? {
    val text = selection.text
    val first = minOf(start, end)
    val last = maxOf(start, end)
    if (first < 0 || last > text.length || first == last) return null
    fun splitsSurrogate(offset: Int) = offset in 1 until text.length &&
        text[offset - 1].isHighSurrogate() && text[offset].isLowSurrogate()
    if (splitsSurrogate(first) || splitsSurrogate(last)) return null
    val selected = text.substring(first, last)
    if (selected.isBlank()) return null
    val position = if (first < selection.currentText.length) selection.pageStart + first
        else (selection.nextPageStart ?: selection.pageStart) + first - selection.currentText.length
    return SelectedExcerpt(position, selected)
}
