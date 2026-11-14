package io.legado.app.model.readingdata

import io.legado.app.data.entities.readRecord.*
import org.junit.Assert.*
import org.junit.Test

class ReadRecordProgressTest {
    @Test fun `backtracking keeps furthest chapter and its own fraction`() {
        val session = ReadRecordSession(words = 0)
            .recordPosition(17, 420, 1000)
            .recordPosition(3, 950, 1000)
        assertEquals(3L, session.words)
        assertEquals(0.95, session.chapterProgress!!, 0.00001)
        assertEquals(17, session.highestChapterIndex)
        assertEquals(0.42, session.highestChapterProgress!!, 0.00001)
    }

    @Test fun `same chapter only advances and later chapter resets fraction`() {
        val session = ReadRecordSession().recordPosition(4, 800, 1000)
            .recordPosition(4, 200, 1000)
        assertEquals(0.8, session.highestChapterProgress!!, 0.00001)
        assertEquals(0.2, session.chapterProgress!!, 0.00001)
        val next = session.recordPosition(5, 20, 1000)
        assertEquals(5, next.highestChapterIndex)
        assertEquals(0.02, next.highestChapterProgress!!, 0.00001)
    }

    @Test fun `unknown chapter size stays unknown and never uses earlier chapter percent`() {
        val session = ReadRecordSession().recordPosition(4, 800, 1000)
            .recordPosition(5, 20, null)
        assertEquals(5, session.highestChapterIndex)
        assertNull(session.highestChapterProgress)
        assertEquals(0.5, session.recordPosition(5, 500, 1000).highestChapterProgress!!, 0.00001)
        assertNull(ReadRecordSession().recordPosition(0, 0, 0).highestChapterProgress)
    }

    @Test fun `percent clamps offsets without depending on font or page count`() {
        assertEquals(1.0, ReadRecordSession().recordPosition(0, 1200, 1000).highestChapterProgress!!, 0.0)
        assertEquals(0.0, ReadRecordSession().recordPosition(0, -1, 1000).highestChapterProgress!!, 0.0)
    }

    @Test fun `legacy final chapter is a lower bound and keeps unknown percentage`() {
        val old = ReadRecordSession(words = 17)
        val merged = old.mergeFurthestProgress(ReadRecordSession().recordPosition(3, 950, 1000))
        assertEquals(17, merged.highestChapterIndex)
        assertNull(merged.highestChapterProgress)
    }
}
