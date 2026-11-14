package io.legado.app.data.repository

import android.app.Application
import androidx.room.Room
import io.legado.app.data.AppDatabase
import io.legado.app.data.entities.readRecord.*
import io.legado.app.help.config.AppConfigStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class DailyReadingRecordsTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: ReadRecordRepository
    private val base = LocalDate.parse("2026-11-14").atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    @Before fun setup() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        AppConfigStore.init(context)
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repository = ReadRecordRepository(db.readRecordDao, db, SettingsRepository())
        repository.setReadRecordEnabled(true)
    }
    @After fun close() { db.close() }
    private fun visit(offset: Long, duration: Long = 60_000, url: String = "book") =
        ReadRecordSession(bookName = "Book", bookAuthor = "Author", bookUrl = url,
            startTime = base + offset, endTime = base + offset + duration)

    @Test fun `many visits use one daily database and export row without counting breaks`() = runBlocking {
        repeat(100) { repository.saveReadSession(visit(it * 300_000L)) }
        val row = db.readRecordDao.allSession.single()
        assertEquals(6_000_000L, row.activeReadTime)
        assertEquals(6_000_000L, repository.getTotalReadTime().first())
        assertEquals(6_000_000L, db.readRecordDao.allDetail.single().readTime)
        val archive = ReadingDataArchive.decode(repository.exportReadingData())
        assertEquals(2, archive.version)
        assertEquals(6_000_000L, archive.sessions.single().readTime)
    }
    @Test fun `checkpoint replay and overlap add no duplicate time`() = runBlocking {
        val first = visit(0)
        repository.saveReadSession(first)
        repository.saveReadSession(first)
        repository.saveReadSession(visit(30_000))
        repository.saveReadSession(first)
        assertEquals(90_000L, db.readRecordDao.allSession.single().activeReadTime)
        assertEquals(90_000L, repository.getTotalReadTime().first())
    }
    @Test fun `midnight splits active time and keeps next date independent`() = runBlocking {
        val midnight = LocalDate.parse("2026-11-15").atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        repository.saveReadSession(ReadRecordSession(bookName = "Book", bookAuthor = "Author", bookUrl = "book",
            startTime = midnight - 30_000, endTime = midnight + 30_000))
        repository.saveReadSession(visit(midnight - base + 3_600_000))
        val rows = db.readRecordDao.allSession.sortedBy { it.recordDate }
        assertEquals(listOf("2026-11-14", "2026-11-15"), rows.map { it.recordDate })
        assertEquals(listOf(30_000L, 90_000L), rows.map { it.activeReadTime })
        assertEquals(120_000L, repository.getTotalReadTime().first())
    }
    @Test fun `copies remain separate and earlier review keeps furthest position`() = runBlocking {
        repository.saveReadSession(visit(0).recordPosition(17, 420, 1000))
        repository.saveReadSession(visit(300_000).recordPosition(2, 900, 1000))
        repository.saveReadSession(visit(600_000, url = "other-copy"))
        val row = db.readRecordDao.allSession.single { it.bookUrl == "book" }
        assertEquals(2L, row.words)
        assertEquals(0.9, row.chapterProgress!!, 0.00001)
        assertEquals(17, row.highestChapterIndex)
        assertEquals(0.42, row.highestChapterProgress!!, 0.00001)
        assertEquals(2, db.readRecordDao.allSession.size)
    }
    @Test fun `daily snapshot roundtrip repeat and older snapshot never double count`() = runBlocking {
        repository.saveReadSession(visit(0).recordPosition(17, 420, 1000))
        val old = repository.exportReadingData()
        repository.saveReadSession(visit(3_600_000).recordPosition(2, 900, 1000))
        val newer = repository.exportReadingData()
        repository.clearReadRecords()
        repository.importReadingData(newer)
        repository.importReadingData(old)
        repository.importReadingData(newer)
        assertEquals(120_000L, repository.getTotalReadTime().first())
        val row = db.readRecordDao.allSession.single()
        assertEquals(2L, row.words)
        assertEquals(17, row.highestChapterIndex)
        repository.saveReadSession(visit(7_200_000))
        assertEquals(180_000L, repository.getTotalReadTime().first())
    }
    @Test fun `deleting daily timeline row removes its whole active total`() = runBlocking {
        repository.saveReadSession(visit(0))
        repository.saveReadSession(visit(3_600_000))
        repository.deleteSession(db.readRecordDao.allSession.single())
        assertTrue(db.readRecordDao.allSession.isEmpty())
        assertEquals(0L, repository.getTotalReadTime().first())
    }
    @Test fun `saved day survives timezone change and deleting it clears totals`() = runBlocking {
        repository.saveReadSession(visit(0))
        val backup = repository.exportReadingData()
        val date = db.readRecordDao.allSession.single().recordDate!!
        val previousZone = java.util.TimeZone.getDefault()
        val changedZone = listOf("Etc/GMT+12", "Pacific/Kiritimati").map(ZoneId::of).first {
            java.time.Instant.ofEpochMilli(base).atZone(it).toLocalDate().toString() != date
        }
        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone(changedZone))
            repository.clearReadRecords()
            repository.importReadingData(backup)
            assertEquals(date, db.readRecordDao.allDetail.single().date)
            assertEquals(1, db.readRecordDao.getSessionsByBookAndDate("", "Book", "Author", date).size)
            assertEquals(60_000L, repository.getBookCopyReadTime("book", "Book", "Author").first())
            repository.deleteDetail(db.readRecordDao.allDetail.single())
            assertTrue(db.readRecordDao.allSession.isEmpty())
            assertEquals(0L, repository.getTotalReadTime().first())
        } finally {
            java.util.TimeZone.setDefault(previousZone)
        }
    }
}
