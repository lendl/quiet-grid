package com.quietgrid.cli.journey

import com.quietgrid.engine.journey.GridTransform
import com.quietgrid.engine.journey.JourneyChapter
import com.quietgrid.engine.journey.JourneyFile
import com.quietgrid.engine.journey.JourneyStop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.serialization.json.Json

class JourneyBuilderTest {
    private val target = JourneyTargetSeconds(100, 1000)

    private fun uniform(count: Int, prefix: String = "p"): List<RatedPuzzle> =
        (0 until count).map { RatedPuzzle("$prefix%04d".format(it), "easy", 100 + it * 900 / (count - 1)) }

    private fun allIds(file: JourneyFile) = file.chapters.flatMap { chapter -> chapter.stops.map { it.puzzleId } }

    @Test
    fun `bands are contiguous and cover the scale`() {
        val bands = journeyBands(500)
        assertEquals(10, bands.size)
        assertEquals(100, bands.first().first)
        assertEquals(1000, bands.last().last)
        bands.zipWithNext().forEach { (a, b) -> assertEquals(a.last + 1, b.first) }
        assertEquals(listOf(100..1000), journeyBands(10))
    }

    @Test
    fun `first build spreads 25 stops sorted by rating then id`() {
        val puzzles = uniform(500)
        val file = buildJourney("g", puzzles, target)
        assertEquals(10, file.chapters.size)
        assertEquals((1..10).toList(), file.chapters.map { it.index })
        file.chapters.forEach { chapter ->
            assertEquals(25, chapter.stops.size)
            chapter.stops.zipWithNext().forEach { (a, b) -> assertTrue(a.rating <= b.rating) }
            chapter.stops.forEach { assertTrue(it.rating in chapter.minRating..chapter.maxRating) }
        }
        assertEquals("p0000", file.chapters.first().stops.first().puzzleId)
    }

    @Test
    fun `small band takes every puzzle and empty bands are dropped`() {
        val low = (0 until 90).map { RatedPuzzle("l%02d".format(it), "easy", 100 + it * 200 / 89) }
        val high = (0 until 10).map { RatedPuzzle("h%02d".format(it), "easy", 900 + it * 10) }
        val file = buildJourney("g", low + high, target)
        assertEquals(listOf(100..549, 550..1000), file.chapters.map { it.minRating..it.maxRating })
        assertEquals(listOf(25, 10), file.chapters.map { it.stops.size })

        val lumped = buildJourney("g", (0 until 100).map { RatedPuzzle("x%03d".format(it), "easy", 100 + it) }, target)
        assertEquals(1, lumped.chapters.size)
        assertEquals(1, lumped.chapters.single().index)
    }

    @Test
    fun `merge with an unchanged bank is a no-op`() {
        val puzzles = uniform(500)
        val built = buildJourney("g", puzzles, target)
        assertEquals(built, mergeJourney(built, puzzles, emptySet(), target))
    }

    @Test
    fun `missing stop is replaced by the nearest unused puzzle in its band`() {
        val puzzles = uniform(500)
        val built = buildJourney("g", puzzles, target)
        val removed = built.chapters.first().stops[2].puzzleId
        val merged = mergeJourney(built, puzzles.filterNot { it.id == removed }, emptySet(), target)
        val before = built.chapters.first().stops
        val after = merged.chapters.first().stops
        assertEquals(before.size, after.size)
        assertNotEquals(removed, after[2].puzzleId)
        assertEquals(removed, after[2].replaces)
        assertTrue(after[2].rating in merged.chapters.first().minRating..merged.chapters.first().maxRating)
        before.indices.filter { it != 2 }.forEach { assertEquals(before[it], after[it]) }
    }

    @Test
    fun `two missing stops in one band get distinct replacements`() {
        val puzzles = uniform(500)
        val built = buildJourney("g", puzzles, target)
        val removed = built.chapters.first().stops.slice(2..3).map { it.puzzleId }.toSet()
        val merged = mergeJourney(built, puzzles.filterNot { it.id in removed }, emptySet(), target)
        val ids = allIds(merged)
        assertEquals(ids.size, ids.toSet().size)
        assertEquals(removed, merged.chapters.first().stops.slice(2..3).map { it.replaces }.toSet())
    }

    @Test
    fun `missing stop with no unused band puzzle is dropped`() {
        val puzzles = uniform(25)
        val built = buildJourney("g", puzzles, target)
        assertEquals(25, built.chapters.single().stops.size)
        val merged = mergeJourney(built, puzzles.drop(1), emptySet(), target)
        assertEquals(24, merged.chapters.single().stops.size)
    }

    @Test
    fun `new puzzles fill the widest gaps and append up to 30`() {
        val existingStops = (0 until 25).map { JourneyStop("s%02d".format(it), "easy", 100 + it * 10, 100) }
        val existing = JourneyFile("g", listOf(JourneyChapter(1, 100, 1000, existingStops)))
        val fresh = listOf(345, 350, 360, 500, 700, 900, 1000, 990).mapIndexed { i, rating -> RatedPuzzle("n$i", "easy", rating) }
        val puzzles = existingStops.map { RatedPuzzle(it.puzzleId, it.difficulty, it.rating) } + fresh
        val merged = mergeJourney(existing, puzzles, fresh.map { it.id }.toSet(), target)
        val stops = merged.chapters.single().stops
        assertEquals(30, stops.size)
        assertEquals(existingStops.map { it.puzzleId }, stops.take(25).map { it.puzzleId })
        assertEquals("n6", stops[25].puzzleId)
    }

    @Test
    fun `puzzles above the top band open a new chapter`() {
        val puzzles = uniform(500)
        val built = buildJourney("g", puzzles, target)
        val higher = (1001..1100).map { RatedPuzzle("hi$it", "easy", it) }
        val merged = mergeJourney(built, puzzles + higher, higher.map { it.id }.toSet(), target)
        val added = merged.chapters.drop(built.chapters.size)
        assertTrue(added.isNotEmpty())
        assertEquals(11, added.first().index)
        assertEquals(1001, added.first().minRating)
        assertEquals(built.chapters, merged.chapters.take(built.chapters.size))
    }

    @Test
    fun `validate rejects a missing id`() {
        val puzzles = uniform(100)
        val file = buildJourney("g", puzzles, target)
        val missing = file.chapters.first().stops.first().puzzleId
        val error = assertThrows(IllegalStateException::class.java) {
            validateJourney(file, puzzles.map { it.id }.toSet() - missing)
        }
        assertTrue(error.message!!.contains(missing))
    }

    @Test
    fun `allowed transforms keep only ratings within five percent`() {
        val ratings = mapOf(GridTransform.MIRROR_H to 400, GridTransform.ROTATE_90 to 421, GridTransform.ROTATE_180 to 379, GridTransform.MIRROR_V to 420)
        assertEquals(listOf(1, 5), allowedReplayTransforms(ratings.keys.toList(), 400) { ratings[it] })
    }

    @Test
    fun `unsolvable transforms are dropped`() {
        assertEquals(listOf(3), allowedReplayTransforms(listOf(GridTransform.MIRROR_H, GridTransform.ROTATE_180), 500) { if (it == GridTransform.MIRROR_H) null else 500 })
    }

    @Test
    fun `replay transforms are recomputed for every stop`() {
        val file = buildJourney("g", uniform(100), target)
        val stale = file.copy(chapters = file.chapters.map { chapter -> chapter.copy(stops = chapter.stops.map { it.copy(replayTransforms = listOf(7)) }) })
        val refreshed = withReplayTransforms(stale) { stop -> if (stop.rating % 2 == 0) listOf(1) else emptyList() }
        refreshed.chapters.flatMap { it.stops }.forEach { stop ->
            assertEquals(stop.puzzleId, if (stop.rating % 2 == 0) listOf(1) else emptyList<Int>(), stop.replayTransforms)
        }
        assertEquals(allIds(stale), allIds(refreshed))
    }

    @Test
    fun `a stop without replay transforms decodes with an empty list`() {
        val stop = Json.decodeFromString(JourneyStop.serializer(), """{"puzzleId":"a","difficulty":"easy","rating":100,"targetSeconds":60}""")
        assertEquals(emptyList<Int>(), stop.replayTransforms)
        assertTrue("replayTransforms" !in Json.encodeToString(JourneyStop.serializer(), stop))
    }
}
