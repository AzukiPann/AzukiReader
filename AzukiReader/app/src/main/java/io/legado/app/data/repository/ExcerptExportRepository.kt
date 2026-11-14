package io.legado.app.data.repository

import android.content.Context
import android.net.Uri
import io.legado.app.domain.gateway.BookMarkingGateway
import io.legado.app.domain.gateway.ExcerptExportGateway
import io.legado.app.domain.model.ExcerptEntry
import io.legado.app.domain.model.TextProcessAnchor
import io.legado.app.domain.model.excerptsToMarkdown
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.IOException

class ExcerptExportRepository(
    private val context: Context,
    private val bookmarks: BookmarkRepository,
    private val markings: BookMarkingGateway,
) : ExcerptExportGateway {
    override suspend fun exportAll(destination: String): Int = withContext(Dispatchers.IO) {
        val bookmarkEntries = bookmarks.getAll().map { bookmark ->
            ExcerptEntry(bookmark.bookName, bookmark.bookAuthor, bookmark.chapterName,
                bookmark.chapterIndex, bookmark.chapterPos, bookmark.bookText, bookmark.content)
        }
        val markingEntries = markings.flowAll().first().mapNotNull { marking ->
            val anchor = GSON.fromJsonObject<TextProcessAnchor>(marking.anchorJson).getOrNull()
                ?: return@mapNotNull null
            ExcerptEntry(marking.bookName, marking.bookAuthor, marking.chapterName,
                marking.chapterIndex ?: anchor.chapterIndex, anchor.chapterPosition ?: 0,
                anchor.selectedText.orEmpty(), marking.note)
        }
        val entries = (bookmarkEntries + markingEntries).filter { it.text.isNotBlank() }
        val markdown = excerptsToMarkdown(entries)
        val uri = Uri.parse(destination)
        val stream = context.contentResolver.openOutputStream(uri, "wt")
            ?: throw IOException("Document provider did not open an output stream")
        stream.bufferedWriter(Charsets.UTF_8).use { it.write(markdown) }
        entries.size
    }
}
