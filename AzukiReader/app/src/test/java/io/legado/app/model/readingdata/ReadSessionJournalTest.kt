package io.legado.app.model.readingdata

import io.legado.app.data.entities.readRecord.ReadRecordSession
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ReadSessionJournalTest {
    private val first = ReadRecordSession(bookName = "书", startTime = 1_000, endTime = 21_000, words = 3)

    @Test fun `pause queued during auto save is written once and preserves the final chapter`() = runTest {
        val ready = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val saved = mutableListOf<ReadRecordSession>()
        val journal = ReadSessionJournal { session ->
            if (saved.isEmpty()) { ready.complete(Unit); release.await() }
            saved += session
        }
        journal.enqueue(first)
        val autoSave = launch { journal.flush() }
        ready.await()
        val pause = first.copy(startTime = first.endTime, endTime = 41_000, words = 0)
        journal.enqueue(pause)
        val pauseSave = launch { journal.flush() }
        runCurrent()
        release.complete(Unit)
        autoSave.join(); pauseSave.join()
        assertEquals(listOf(first, pause), saved)
        assertEquals(40_000L, saved.sumOf { it.endTime - it.startTime })
        assertEquals(0L, saved.last().words)
    }

    @Test fun `failed writes are retained for a later flush without replaying successful checkpoints`() = runTest {
        var fail = true
        val saved = mutableListOf<ReadRecordSession>()
        val second = first.copy(startTime = first.endTime, endTime = 41_000)
        val journal = ReadSessionJournal { if (it == second && fail) error("disk unavailable") else saved += it }
        journal.enqueue(first); journal.enqueue(second)
        assertTrue(runCatching { journal.flush() }.isFailure)
        assertEquals(listOf(first), saved)
        fail = false
        journal.flush(); journal.flush()
        assertEquals(listOf(first, second), saved)
    }

    @Test fun `cancellation leaves the in flight checkpoint available for retry`() = runTest {
        var blocked = true
        val started = CompletableDeferred<Unit>()
        val saved = mutableListOf<ReadRecordSession>()
        val journal = ReadSessionJournal {
            if (blocked) { started.complete(Unit); CompletableDeferred<Unit>().await() }
            saved += it
        }
        journal.enqueue(first)
        val saving = async { journal.flush() }
        started.await(); saving.cancel(); saving.join()
        blocked = false
        journal.flush()
        assertEquals(listOf(first), saved)
    }
}
