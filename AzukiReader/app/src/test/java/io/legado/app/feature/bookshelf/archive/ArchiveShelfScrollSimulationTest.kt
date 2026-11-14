package io.legado.app.feature.bookshelf.archive

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class ArchiveShelfScrollSimulationTest {
    @Test fun `fast flicks retain momentum beyond three books`() {
        val slow = ArchiveShelfScrollSimulation(0f, 300f, 27f)
        val fast = ArchiveShelfScrollSimulation(0f, 1200f, 27f)
        assertTrue(fast.target > slow.target * 3)
        assertTrue(fast.target / 27 > 3)
        assertTrue(fast.position(.15f) > fast.position(.05f))
        assertTrue(fast.velocity(.15f) > 100)
    }

    @Test fun `signed flicks land monotonically at exact cyclic detents`() {
        for (pitch in listOf(25f, 27f, 29f)) for (lane in listOf(-100.99f, -.5f, 0f, .49f, 10.75f, 100f)) {
            for (speed in listOf(50f, 80f, 300f, 1800f, 8000f)) for (sign in listOf(-1f, 1f)) {
                val sim = ArchiveShelfScrollSimulation(lane * pitch, speed * sign, pitch)
                assertEquals(lane * pitch, sim.position(0f), .001f)
                assertEquals(speed * sign, sim.velocity(0f), .001f)
                var previous = sim.position(0f)
                var previousSpeed = speed
                repeat(720) { tick ->
                    val time = (tick + 1) / 240f
                    val current = sim.position(time)
                    assertTrue((current - previous) * sign >= -.002f)
                    assertTrue(abs(sim.velocity(time)) <= previousSpeed + .01f)
                    previous = current
                    previousSpeed = abs(sim.velocity(time))
                }
                assertEquals(sim.target, sim.position(3f), .001f)
                assertEquals(0f, sim.velocity(3f), .001f)
                assertEquals(archiveLaneRound(sim.target / pitch), sim.target / pitch, .001f)
            }
        }
    }

    @Test fun `resting negative and positive half lanes match Dart rounding`() {
        for (lane in listOf(-10.4f, -.6f, -.5f, -.4f, 0f, .4f, .5f, .6f, 10.4f)) {
            val sim = ArchiveShelfScrollSimulation(lane * 27f, 0f, 27f)
            assertEquals(archiveLaneRound(lane), sim.target / 27f, .001f)
            assertEquals(sim.target, sim.position(sim.duration), .001f)
        }
        assertEquals(-1f, archiveLaneRound(-.5f), 0f)
        assertEquals(1f, archiveLaneRound(.5f), 0f)
    }

    @Test fun `reference dimensions and crest extraction stay unchanged`() {
        assertEquals(14f, ArchiveShelfGeometry.spine(200f), 0f)
        assertEquals(18f, ArchiveShelfGeometry.spine(800f), 0f)
        assertEquals(100f, ArchiveShelfGeometry.cover(200f), 0f)
        assertEquals(128f, ArchiveShelfGeometry.cover(800f), 0f)
        assertEquals(0f, ArchiveShelfGeometry.weight(1.48f), .0001f)
        assertEquals(1f, ArchiveShelfGeometry.weight(0f), .0001f)
        assertEquals(.5f, ArchiveShelfGeometry.weight(.74f), .0001f)
        assertEquals(5.753125f, ArchiveShelfGeometry.rail(1.75f), .0001f)
        val h = ArchiveShelfGeometry.height(128f)
        val closed = ArchiveShelfGeometry.origin(128f, h, 0f)
        val open = ArchiveShelfGeometry.origin(128f, h, 1f)
        assertEquals(-70.9632f, open.x, .001f)
        assertEquals(82.656f, open.y, .001f)
        assertTrue(open.y < closed.y)
    }

    @Test fun `transparent projected corners are excluded from hit testing`() {
        assertFalse(ArchiveShelfGeometry.contains(Offset(1f, 1f), 16f, 120f, 195f))
        assertTrue(ArchiveShelfGeometry.contains(Offset(8f, 80f), 16f, 120f, 195f))
        assertTrue(ArchiveShelfGeometry.contains(Offset(70f, 80f), 16f, 120f, 195f))
        assertFalse(ArchiveShelfGeometry.contains(Offset(90f, 230f), 16f, 120f, 195f))
        assertEquals(9, ArchiveShelfGeometry.bookIndex(-1, 10))
        assertEquals(0, ArchiveShelfGeometry.bookIndex(10, 10))
    }
}
