package io.legado.app.domain.model

/** Directory metadata for the recorded furthest position; unknown values stay null. */
data class ReadingChapterReference(
    val chapterIndex: Int,
    val title: String?,
    val totalChapters: Int?,
    val currentChapterIndex: Long? = null,
    val currentChapterTitle: String? = null,
    val currentTotalChapters: Int? = null,
)
