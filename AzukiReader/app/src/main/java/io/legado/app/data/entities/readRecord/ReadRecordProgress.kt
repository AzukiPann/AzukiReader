package io.legado.app.data.entities.readRecord

/** Old sessions only prove that their last chapter was reached; the fraction remains unknown. */
internal val ReadRecordSession.furthestChapter: Int
    get() = maxOf(highestChapterIndex ?: 0, words.coerceIn(0, Int.MAX_VALUE.toLong()).toInt())

/** Merge metadata without changing session identity, timing, or last position. */
internal fun ReadRecordSession.mergeFurthestProgress(other: ReadRecordSession): ReadRecordSession {
    val chapter = maxOf(furthestChapter, other.furthestChapter)
    val progress = listOf(this, other).mapNotNull {
        it.highestChapterProgress?.takeIf { value ->
            it.highestChapterIndex == chapter && value.isFinite() && value in 0.0..1.0
        }
    }.maxOrNull()
    return copy(highestChapterIndex = chapter, highestChapterProgress = progress)
}

/** Semantic text offset / chapter size, independent of font, margins and pagination. */
internal fun ReadRecordSession.recordPosition(chapter: Int, position: Int, length: Int?): ReadRecordSession {
    if (chapter < 0) return this
    val progress = length?.takeIf { it > 0 }?.let { position.coerceIn(0, it).toDouble() / it }
    val current = copy(words = chapter.toLong(), chapterProgress = progress,
        highestChapterIndex = chapter, highestChapterProgress = progress)
    return current.mergeFurthestProgress(this)
}

/** Enrich a duplicate checkpoint without adding time or losing a previously known current fraction. */
internal fun ReadRecordSession.mergeDuplicateProgress(other: ReadRecordSession): ReadRecordSession =
    mergeFurthestProgress(other).copy(
        chapterProgress = listOfNotNull(chapterProgress, other.chapterProgress).maxOrNull(),
        readTime = listOfNotNull(readTime, other.readTime).maxOrNull(),
        recordDate = recordDate ?: other.recordDate,
    )
