package io.legado.app.domain.model

data class ExcerptEntry(
    val bookName: String,
    val author: String,
    val chapterName: String,
    val chapterIndex: Int,
    val position: Int,
    val text: String,
    val note: String = "",
)

/** Pure formatting, shared by the export writer and its tests. No UI/search state is an input. */
fun excerptsToMarkdown(entries: List<ExcerptEntry>): String = buildString {
    val quotations = entries.filter { it.text.isNotBlank() }
        .sortedWith(compareBy({ it.bookName }, { it.author }, { it.chapterIndex }, { it.position }))
    append("# AzukiReader 摘录\n\n")
    append("共 ${quotations.size} 条摘录\n\n")
    if (quotations.isEmpty()) append("暂无摘录。\n")
    quotations.groupBy { it.bookName to it.author }.forEach { (book, items) ->
        append("## ${markdownLiteral(book.first.ifBlank { "未命名书籍" })}\n\n")
        if (book.second.isNotBlank()) append("作者：${markdownLiteral(book.second)}\n\n")
        items.forEach { entry ->
            append("### ${markdownLiteral(entry.chapterName.ifBlank { "未命名章节" })}\n\n")
            normalizeLines(entry.text).lines().forEach { line -> append("> ${markdownLiteral(line)}\n") }
            append('\n')
            if (entry.note.isNotBlank()) {
                append("备注：\n\n")
                normalizeLines(entry.note).lines().forEach { append("${markdownLiteral(it)}\n") }
                append('\n')
            }
            append("---\n\n")
        }
    }
}

private fun normalizeLines(value: String) = value.replace("\r\n", "\n").replace('\r', '\n')
private fun markdownLiteral(value: String): String = buildString {
    // A stored sentence must remain a sentence, even when it contains Markdown or HTML syntax.
    value.forEach { char ->
        when (char) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '\n', '\r' -> append(' ')
            '\\', '`', '*', '_', '[', ']', '#', '!', '|', '~' -> { append('\\'); append(char) }
            else -> append(char)
        }
    }
}
