package io.legado.app.feature.reader.bookmarks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.legado.app.R
import io.legado.app.ui.widget.components.AppTextFieldSurface
import io.legado.app.ui.widget.components.button.series.MediumTonalButton
import io.legado.app.ui.widget.components.modalBottomSheet.AppModalBottomSheet

@Composable
fun ReaderExcerptSheet(
    show: Boolean,
    selection: ReaderExcerptSelection,
    onDismiss: () -> Unit,
    onSave: (Int, Int, String) -> Unit,
    onEditSaved: (Long) -> Unit,
) {
    var start by rememberSaveable(selection.bookUrl, selection.chapterIndex, selection.pageStart) { mutableIntStateOf(0) }
    var end by rememberSaveable(selection.bookUrl, selection.chapterIndex, selection.pageStart) { mutableIntStateOf(0) }
    var note by rememberSaveable(selection.bookUrl, selection.chapterIndex, selection.pageStart) { mutableStateOf("") }
    val excerpt = remember(selection, start, end) { selectedExcerpt(selection, start, end) }
    AppModalBottomSheet(show = show, onDismissRequest = onDismiss,
        title = stringResource(R.string.bookmark_excerpt_title), animateContentSize = false,
        sheetGesturesEnabled = start == end,
        endAction = {
            MediumTonalButton(text = stringResource(R.string.bookmark_excerpt_save), enabled = excerpt != null,
                onClick = { onSave(start, end, note) })
        },
    ) {
        ReaderExcerptContent(selection, TextRange(start, end), note,
            onSelectionChange = { start = it.start; end = it.end },
            onNoteChange = { note = it }, onEditSaved = onEditSaved)
    }
}

@Composable
internal fun ReaderExcerptContent(
    selection: ReaderExcerptSelection,
    range: TextRange,
    note: String,
    onSelectionChange: (TextRange) -> Unit,
    onNoteChange: (String) -> Unit,
    onEditSaved: (Long) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val selected = remember(selection, range) { selectedExcerpt(selection, range.start, range.end)?.text.orEmpty() }
    val count = selected.codePointCount(0, selected.length)
    var showSaved by rememberSaveable(selection.bookUrl, selection.chapterIndex, selection.pageStart) { mutableStateOf(false) }
    val bodyLabel = stringResource(R.string.bookmark_excerpt_body)
    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 560.dp).testTag("excerpt-list").imePadding(),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item(key = "source") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.FormatQuote, contentDescription = null, tint = colors.primary,
                    modifier = Modifier.padding(end = 12.dp).size(30.dp))
                Column(Modifier.weight(1f)) {
                    Text(selection.bookName, style = MaterialTheme.typography.titleMedium, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(selection.chapterTitle, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        item(key = "paper") {
            Surface(shape = RoundedCornerShape(18.dp), color = colors.surfaceContainerLow,
                border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = .55f))) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(if (selection.hasNextPage) R.string.bookmark_excerpt_two_pages else R.string.bookmark_excerpt_one_page),
                            style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant, modifier = Modifier.weight(1f))
                        Text(stringResource(R.string.bookmark_excerpt_count, count), style = MaterialTheme.typography.labelMedium, color = colors.primary)
                    }
                    HorizontalDivider(color = colors.outlineVariant.copy(alpha = .5f))
                    if (selection.text.isBlank()) Text(stringResource(R.string.bookmark_excerpt_empty), color = colors.onSurface)
                    else BasicTextField(
                        value = TextFieldValue(selection.text, TextRange(range.start.coerceIn(0, selection.text.length), range.end.coerceIn(0, selection.text.length))),
                        onValueChange = { onSelectionChange(it.selection) },
                        readOnly = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Serif,
                            fontSize = 17.sp, lineHeight = 30.sp, color = colors.onSurface),
                        cursorBrush = SolidColor(colors.primary),
                        minLines = 6, maxLines = 12,
                        modifier = Modifier.fillMaxWidth().testTag("excerpt-body").semantics { contentDescription = bodyLabel },
                    )
                }
            }
        }
        item(key = "hint") {
            Text(stringResource(R.string.bookmark_excerpt_hint), style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp))
        }
        item(key = "note") {
            AppTextFieldSurface(value = note, onValueChange = onNoteChange,
                label = stringResource(R.string.bookmark_excerpt_note), modifier = Modifier.fillMaxWidth(), maxLines = 3)
        }
        if (selection.saved.isNotEmpty()) {
            item(key = "saved-title") {
                TextButton(onClick = { showSaved = !showSaved }, modifier = Modifier.testTag("excerpt-show-saved")) {
                    Text(stringResource(R.string.bookmark_excerpt_saved, selection.saved.size))
                }
            }
            if (showSaved) items(selection.saved, key = { "saved-${it.id}" }, contentType = { "saved" }) { saved ->
                Surface(onClick = { onEditSaved(saved.id) }, modifier = Modifier.testTag("saved-excerpt-${saved.id}"),
                    shape = RoundedCornerShape(12.dp), color = colors.surfaceContainerLow) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(saved.text, maxLines = 3, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
                        if (saved.note.isNotBlank()) Text(saved.note, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
