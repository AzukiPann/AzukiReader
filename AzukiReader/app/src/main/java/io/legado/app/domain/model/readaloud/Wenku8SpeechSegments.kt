package io.legado.app.domain.model.readaloud

/** Wenku8 speechSegments: bounded clauses with original UTF-16 chapter offsets. */
fun wenku8SpeechSegments(content: String, maximum: Int = 240): List<ReadAloudSplitUnit> {
    require(maximum in 20..1000)
    return buildList {
        var base = 0
        content.split('\n').forEach { paragraph ->
            val trimmed = paragraph.trim()
            if (trimmed.isNotEmpty() && trimmed != "[插图]" && !trimmed.contains("<!--image-->") && !trimmed.contains("<img", ignoreCase = true) && trimmed != "\uFFFC") {
                var offset = 0
                while (offset < paragraph.length) {
                    while (offset < paragraph.length && paragraph[offset].isWhitespace()) offset++
                    if (offset == paragraph.length) break
                    var end = minOf(offset + maximum, paragraph.length)
                    if (end < paragraph.length) {
                        if (paragraph[end-1].isHighSurrogate()) end--
                        val window = paragraph.substring(offset,end)
                        val sentence = Regex("[。！？!?；;]").findAll(window).lastOrNull()?.range?.last?.plus(1)
                        val clause = Regex("[，,：:、\\s]").findAll(window).filter { match ->
                            val at = offset + match.range.first
                            match.range.last+1 >= maximum/2 && !(paragraph[at] in "，," && paragraph.getOrNull(at-1)?.isDigit() == true && paragraph.getOrNull(at+1)?.isDigit() == true)
                        }.lastOrNull()?.range?.last?.plus(1)
                        end = offset + (sentence?.takeIf { it >= maximum/3 } ?: clause ?: (end-offset))
                        while (end < paragraph.length && end < offset+maximum+4 && paragraph[end] in "」』”’") end++
                    }
                    val text = paragraph.substring(offset,end)
                    if (text.codePoints().anyMatch { Character.isLetterOrDigit(it) }) add(ReadAloudSplitUnit(text, base+offset))
                    offset=end
                }
            }
            base += paragraph.length + 1
        }
    }
}
