package io.legado.app.model.readingdata

import io.legado.app.data.entities.readRecord.ReadRecordSession
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Single writer for detached checkpoints. Failed/cancelled writes remain retryable. */
internal class ReadSessionJournal(private val save: suspend (ReadRecordSession) -> Unit) {
    private val pending = ArrayDeque<ReadRecordSession>()
    private val writer = Mutex()

    fun enqueue(session: ReadRecordSession) {
        synchronized(pending) { pending.addLast(session) }
    }

    suspend fun flush() = writer.withLock {
        while (true) {
            val next = synchronized(pending) { pending.firstOrNull() } ?: return@withLock
            save(next)
            synchronized(pending) { pending.removeFirst() }
        }
    }
}
