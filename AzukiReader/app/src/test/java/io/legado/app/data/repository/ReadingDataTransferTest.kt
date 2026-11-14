package io.legado.app.data.repository

import android.app.Application
import android.net.Uri
import androidx.room.Room
import io.legado.app.data.AppDatabase
import io.legado.app.data.entities.readRecord.*
import io.legado.app.help.config.AppConfigStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class ReadingDataTransferTest {
    @get:Rule val temporary = TemporaryFolder()
    private lateinit var db: AppDatabase
    private lateinit var records: ReadRecordRepository
    private lateinit var files: ReadingDataRepository
    private val day = "2026-11-12"
    private val base = LocalDate.parse(day).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Before fun setup() {
        val context = RuntimeEnvironment.getApplication()
        AppConfigStore.init(context)
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        records = ReadRecordRepository(db.readRecordDao, db, SettingsRepository())
        files = ReadingDataRepository(context, records)
    }
    @After fun close() = db.close()
    private fun session(start: Long = base, chapter: Long = 0, url: String = "copy-a") =
        ReadRecordSession(bookName = "书", bookAuthor = "作者", bookUrl = url,
            startTime = start, endTime = start + 60_000, words = chapter)

    private fun debugFixture() = requireNotNull(javaClass.getResource("/reading-data/debug-v1.json")).readText()

    @Test fun `debug v1 backup imports without resolving debug app content uris`() = runBlocking {
        repeat(2) { records.importReadingData(debugFixture()) }
        assertEquals(60_000L, records.getTotalReadTime().first())
        val session=db.readRecordDao.allSession.single()
        assertEquals(17L, session.words)
        assertEquals("debug-device",session.deviceId)
        assertEquals("content://io.legato.kazusa.debug/books/copy-a",session.bookUrl)
        val exported=com.google.gson.JsonParser.parseString(records.exportReadingData()).asJsonObject
        assertEquals(setOf("format","version","timeZone","records","details","sessions"),exported.keySet())
        assertEquals("Azuk1P4N.reading-data",exported.get("format").asString)
        assertEquals(2,exported.get("version").asInt)
        assertEquals(17L,exported.getAsJsonArray("sessions")[0].asJsonObject.get("words").asLong)
    }

    @Test fun `known 1_00_02 release field names recover without accepting unrelated json`() = runBlocking {
        val original=com.google.gson.JsonParser.parseString(debugFixture()).asJsonObject
        val legacy=com.google.gson.JsonObject().apply {
            add("a",original.get("format"));add("b",original.get("timeZone"))
            add("c",original.get("records"));add("d",original.get("details"));add("e",original.get("sessions"))
        }
        repeat(2) { records.importReadingData(legacy.toString()) }
        assertEquals(60_000L, records.getTotalReadTime().first())
        assertEquals(17L,db.readRecordDao.allSession.single().words)
        legacy.addProperty("a","unrelated")
        assertTrue(runCatching { records.importReadingData(legacy.toString()) }.isFailure)
        assertEquals(1,db.readRecordDao.allSession.size)
    }

    @Test fun `file roundtrip restores summary days and final chapter without importing row ids`() = runBlocking {
        records.setReadRecordEnabled(true)
        records.saveReadSession(session(chapter = 17))
        val file = temporary.newFile("reading.json")
        files.exportData(Uri.fromFile(file).toString())
        val archive = ReadingDataArchive.decode(file.readText())
        assertEquals(0L, archive.sessions.single().id)
        assertEquals(17L, archive.sessions.single().words)
        records.clearReadRecords()
        files.importData(Uri.fromFile(file).toString())
        files.importData(Uri.fromFile(file).toString())
        assertEquals(60_000L, records.getTotalReadTime().first())
        assertEquals(60_000L, db.readRecordDao.allDetail.sumOf { it.readTime })
        assertEquals(17L, db.readRecordDao.allSession.single().words)
    }

    @Test fun `import unions divergent sessions and preserves legacy time idempotently`() = runBlocking {
        val local = session()
        val remote = session(base + 120_000, chapter = 5)
        db.readRecordDao.insertSession(local)
        db.readRecordDao.insert(ReadRecord("", "书", "作者", 90_000, local.endTime))
        db.readRecordDao.insertDetail(ReadRecordDetail("", "书", "作者", day, 90_000))
        val json = ReadingDataArchive(records = listOf(ReadRecord("", "书", "作者", 100_000, remote.endTime)),
            details = listOf(ReadRecordDetail("", "书", "作者", day, 100_000)), sessions = listOf(remote.copy(id = 1))).encode()
        repeat(3) { records.importReadingData(json) }
        assertEquals(2, db.readRecordDao.allSession.size)
        assertEquals(160_000L, records.getTotalReadTime().first())
        assertEquals(160_000L, db.readRecordDao.allDetail.sumOf { it.readTime })
    }

    @Test fun `legacy summary only archives remain repeatable and do not clear sessions`() = runBlocking {
        db.readRecordDao.insertSession(session())
        db.readRecordDao.insert(ReadRecord("", "书", "作者", 60_000, base))
        val json = ReadingDataArchive(records = listOf(ReadRecord("", "旧书", "作者", 123_456, base))).encode()
        repeat(2) { records.importReadingData(json) }
        assertEquals(183_456L, records.getTotalReadTime().first())
        assertEquals(1, db.readRecordDao.allSession.size)
    }

    @Test fun `copies and devices stay separate even with colliding database ids`() = runBlocking {
        val first = session().copy(id = 1)
        val other = session(url = "copy-b").copy(id = 1, deviceId = "remote", words = 3)
        db.readRecordDao.insertSession(first)
        records.importReadingData(ReadingDataArchive(sessions = listOf(other)).encode())
        assertEquals(2, db.readRecordDao.allSession.size)
        assertEquals(setOf("copy-a", "copy-b"), db.readRecordDao.allSession.map { it.bookUrl }.toSet())
    }

    @Test fun `midnight session rebuilds both dates and repeated import keeps totals`() = runBlocking {
        val midnight = LocalDate.parse(day).plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val session = session(midnight - 30_000)
        val json = ReadingDataArchive(sessions = listOf(session)).encode()
        repeat(2) { records.importReadingData(json) }
        assertEquals(listOf(30_000L, 30_000L), db.readRecordDao.allDetail.sortedBy { it.date }.map { it.readTime })
        assertEquals(60_000L, records.getTotalReadTime().first())
    }

    @Test fun `invalid file is fully rejected before any import writes`() = runBlocking {
        db.readRecordDao.insertSession(session())
        val json = ReadingDataArchive(sessions = listOf(session(base + 120_000))).encode()
        fun invalid(change: (com.google.gson.JsonObject) -> Unit): String =
            com.google.gson.JsonParser.parseString(json).asJsonObject.also(change).toString()
        val corruptions = listOf(invalid { it.addProperty("version", 2) },
            invalid { it.getAsJsonArray("sessions")[0].asJsonObject.add("bookName", com.google.gson.JsonNull.INSTANCE) },
            invalid { it.getAsJsonArray("sessions")[0].asJsonObject.addProperty("words", -1) },
            "{}", "not json")
        corruptions.forEach { invalid ->
            assertTrue(runCatching { records.importReadingData(invalid) }.isFailure)
            assertEquals(1, db.readRecordDao.allSession.size)
            assertTrue(db.readRecordDao.all.isEmpty())
        }
    }

    @Test fun `home recovers final chapter from imported data without a shelf book`() = runBlocking {
        records.importReadingData(ReadingDataArchive(sessions = listOf(session(chapter = 17))).encode())
        val row = db.readRecordDao.observeRecentHomeBooks(7).first().single()
        assertEquals(17, row.chapterIndex)
        assertNull(row.bookUrl)
        assertNull(row.chapterTitle)
    }

    @Test fun `home uses actual final chapter from the recorded copy without changing shelf position`() = runBlocking {
        val book = io.legado.app.data.entities.Book(bookUrl = "copy-a", name = "书", author = "作者",
            durChapterIndex = 50, durChapterTime = base - 1, totalChapterNum = 100)
        db.bookDao.insert(book)
        db.bookChapterDao.insert(io.legado.app.data.entities.BookChapter(url = "chapter-0", bookUrl = "copy-a", index = 0, title = "序章"))
        records.importReadingData(ReadingDataArchive(sessions = listOf(session(chapter = 0))).encode())
        val row = db.readRecordDao.observeRecentHomeBooks(7).first().single()
        assertEquals(0, row.chapterIndex)
        assertEquals("序章", row.chapterTitle)
        assertEquals(50, db.bookDao.getBook("copy-a")!!.durChapterIndex)
    }

    @Test fun `archive timezone is respected when rebuilding days without doubling remote dates`() = runBlocking {
        val remoteZone = ZoneId.of("Pacific/Honolulu")
        val start = LocalDate.parse(day).atTime(23, 59, 30).atZone(remoteZone).toInstant().toEpochMilli()
        val remote = session(start)
        val remoteDetails = listOf(ReadRecordDetail("", "书", "作者", day, 30_000),
            ReadRecordDetail("", "书", "作者", LocalDate.parse(day).plusDays(1).toString(), 30_000))
        val json = ReadingDataArchive(timeZone = remoteZone.id, sessions = listOf(remote), details = remoteDetails).encode()
        repeat(2) { records.importReadingData(json) }
        assertEquals(60_000L, records.getTotalReadTime().first())
        assertEquals(60_000L, db.readRecordDao.allDetail.sumOf { it.readTime })
    }

    @Test fun `timeline uses last visited chapter even after jumping backwards`() {
        val first = session(chapter = 20)
        val last = session(base + 90_000, chapter = 0)
        val merged = mergeReadSessionsForTimeline(listOf(last, first))
        assertEquals(1, merged.size)
        assertEquals(0L, merged.single().words)
        assertEquals(last.endTime, merged.single().endTime)
    }

    @Test fun `furthest progress roundtrips without changing final chapter`() = runBlocking {
        val first = session(chapter = 17).recordPosition(17, 420, 1000)
        val back = session(base + 120_000, chapter = 2).recordPosition(2, 990, 1000)
        records.saveReadSession(first)
        records.saveReadSession(back)
        val row = io.legado.app.ui.book.readRecord.summarizeReadingProgress(db.readRecordDao.allSession)!!
        assertEquals(2L, row.currentChapterIndex)
        assertEquals(0.99, row.currentChapterProgress!!, 0.00001)
        assertEquals(17, row.highestChapterIndex)
        assertEquals(0.42, row.highestChapterProgress!!, 0.00001)
        val exported = records.exportReadingData()
        records.clearReadRecords()
        repeat(2) { records.importReadingData(exported) }
        assertEquals(120_000L, records.getTotalReadTime().first())
        val restored = io.legado.app.ui.book.readRecord.summarizeReadingProgress(db.readRecordDao.allSession)!!
        assertEquals(row.highestChapterIndex, restored.highestChapterIndex)
        assertEquals(row.highestChapterProgress, restored.highestChapterProgress)
        assertEquals(2L, restored.currentChapterIndex)
        assertEquals(0.99, restored.currentChapterProgress!!, 0.00001)
    }

    @Test fun `metadata enrichment does not double count time or downgrade progress`() = runBlocking {
        val legacy = session(chapter = 17)
        records.importReadingData(ReadingDataArchive(sessions = listOf(legacy)).encode())
        val enriched = legacy.recordPosition(17, 650, 1000)
        repeat(2) { records.importReadingData(ReadingDataArchive(sessions = listOf(enriched)).encode()) }
        records.importReadingData(ReadingDataArchive(sessions = listOf(legacy)).encode())
        val stored = db.readRecordDao.allSession.single()
        assertEquals(0.65, stored.highestChapterProgress!!, 0.00001)
        assertEquals(0.65, stored.chapterProgress!!, 0.00001)
        assertEquals(60_000L, records.getTotalReadTime().first())
        assertEquals(0.65, io.legado.app.ui.book.readRecord.summarizeReadingProgress(db.readRecordDao.allSession)!!.highestChapterProgress!!, 0.00001)
    }

    @Test fun `later chapter wins over earlier percent and equal chapter uses higher percent`() = runBlocking {
        val sessions = listOf(
            session(chapter = 17).recordPosition(17, 990, 1000),
            session(base + 120_000, chapter = 18).recordPosition(18, 200, 1000),
            session(base + 240_000, chapter = 18).recordPosition(18, 50, 1000),
        )
        records.importReadingData(ReadingDataArchive(sessions = sessions).encode())
        val row = io.legado.app.ui.book.readRecord.summarizeReadingProgress(db.readRecordDao.allSession)!!
        assertEquals(18, row.highestChapterIndex)
        assertEquals(0.2, row.highestChapterProgress!!, 0.00001)
    }

    @Test fun `legacy backup infers chapter only and invalid percent import is atomic`() = runBlocking {
        records.importReadingData(debugFixture())
        val row = io.legado.app.ui.book.readRecord.summarizeReadingProgress(db.readRecordDao.allSession)!!
        assertEquals(17, row.highestChapterIndex)
        assertNull(row.highestChapterProgress)
        for (bad in listOf("-0.1", "1.1", "null-index", "lower-index", "string")) {
            val json = com.google.gson.JsonParser.parseString(debugFixture()).asJsonObject
            json.getAsJsonArray("sessions")[0].asJsonObject.apply {
                addProperty("highestChapterIndex", 17)
                addProperty("highestChapterProgress", 0.5)
                when (bad) {
                    "null-index" -> remove("highestChapterIndex")
                    "lower-index" -> addProperty("highestChapterIndex", 2)
                    "string" -> addProperty("highestChapterProgress", "0.5")
                    else -> addProperty("highestChapterProgress", bad.toDouble())
                }
            }
            assertTrue(bad, runCatching { records.importReadingData(json.toString()) }.isFailure)
            assertEquals(1, db.readRecordDao.allSession.size)
            assertEquals(60_000L, records.getTotalReadTime().first())
        }
    }

    @Test fun `timeline never combines different book copies`() {
        assertEquals(2, mergeReadSessionsForTimeline(listOf(session(), session(base + 90_000, url = "copy-b"))).size)
    }
}
