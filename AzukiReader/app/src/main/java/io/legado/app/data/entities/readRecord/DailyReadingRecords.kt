package io.legado.app.data.entities.readRecord

import java.time.Instant
import java.time.ZoneId

internal val ReadRecordSession.activeReadTime: Long
    get() = readTime ?: (endTime - startTime).coerceAtLeast(0)

internal fun ReadRecordSession.readingDate(zone: ZoneId = ZoneId.systemDefault()): String =
    recordDate ?: Instant.ofEpochMilli(startTime).atZone(zone).toLocalDate().toString()

/** Split live checkpoints at midnight before accumulating. A saved daily row stays on its recorded date. */
internal fun ReadRecordSession.dailyParts(zone: ZoneId = ZoneId.systemDefault()): List<ReadRecordSession> {
    if (readTime != null && recordDate != null) return listOf(this)
    val result = mutableListOf<ReadRecordSession>()
    var cursor = startTime
    while (cursor < endTime) {
        val date = Instant.ofEpochMilli(cursor).atZone(zone).toLocalDate()
        val end = minOf(endTime, date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli())
        result.add(copy(id = 0, startTime = cursor, endTime = end, readTime = end - cursor, recordDate = date.toString()))
        cursor = end
    }
    return result
}

internal fun ReadRecordSession.dailyKey(): List<String> =
    listOf(deviceId, bookName, bookAuthor, bookUrl, readingDate())

/** One row per day and book copy. Gaps between visits contribute no active reading time. */
internal fun List<ReadRecordSession>.toDailyReadingRecords(): List<ReadRecordSession> =
    flatMap { it.dailyParts() }.groupBy { it.dailyKey() }.values.map { rows ->
        val latest = rows.maxWith(compareBy<ReadRecordSession> { it.endTime }.thenBy { it.startTime }.thenBy { it.id })
        rows.fold(latest) { merged, row -> merged.mergeFurthestProgress(row) }.copy(
            startTime = rows.minOf { it.startTime },
            readTime = rows.sumOf { it.activeReadTime },
        )
    }

/** Same-device daily archives are cumulative snapshots: importing twice must never add time. */
internal fun ReadRecordSession.mergeDailySnapshot(other: ReadRecordSession): ReadRecordSession {
    val latest = if (other.endTime > endTime) other else this
    return latest.mergeFurthestProgress(this).mergeFurthestProgress(other).copy(
        id = id,
        startTime = minOf(startTime, other.startTime),
        readTime = maxOf(activeReadTime, other.activeReadTime),
        recordDate = readingDate(),
    )
}
