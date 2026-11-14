package io.legado.app.data.repository

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.google.gson.JsonParser
import io.legado.app.data.AppDatabase
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class ReadingProgressMigrationTest {
    @Test fun `134 upgrade preserves existing session and unknown fraction`() {
        val context = RuntimeEnvironment.getApplication()
        val name = "reading-progress-migration.db"
        context.deleteDatabase(name)
        val schema = JsonParser.parseString(File(requireNotNull(System.getProperty("reading.progress.schemaDir")),
            "io.legado.app.data.AppDatabase/134.json").readText()).asJsonObject.getAsJsonObject("database")
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { old ->
            for (entry in schema.getAsJsonArray("entities")) {
                val entity = entry.asJsonObject
                val table = entity.get("tableName").asString
                old.execSQL(entity.get("createSql").asString.replace("\${TABLE_NAME}", table))
                entity.getAsJsonArray("indices")?.forEach { index ->
                    old.execSQL(index.asJsonObject.get("createSql").asString.replace("\${TABLE_NAME}", table))
                }
            }
            schema.getAsJsonArray("views")?.forEach { entry ->
                val view = entry.asJsonObject
                old.execSQL(view.get("createSql").asString.replace("\${VIEW_NAME}", view.get("viewName").asString))
            }
            for (query in schema.getAsJsonArray("setupQueries")) old.execSQL(query.asString)
            old.execSQL("INSERT INTO readRecordSession (id,deviceId,bookName,bookAuthor,bookUrl,startTime,endTime,words) VALUES (1,'','book','author','copy',1000,61000,17)")
            old.version = 134
        }
        try {
            val db = Room.databaseBuilder(context, AppDatabase::class.java, name).allowMainThreadQueries().build()
            try {
                val session = db.readRecordDao.allSession.single()
                assertEquals(17L, session.words)
                assertEquals(60_000L, session.endTime - session.startTime)
                assertNull(session.highestChapterIndex)
                assertNull(session.highestChapterProgress)
                assertNull(session.chapterProgress)
                assertEquals(136, db.openHelper.readableDatabase.version)
            } finally { db.close() }
        } finally { context.deleteDatabase(name) }
    }
}
