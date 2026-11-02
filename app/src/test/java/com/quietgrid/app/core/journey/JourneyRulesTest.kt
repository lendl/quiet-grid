package com.quietgrid.app.core.journey

import com.quietgrid.engine.journey.JourneyChapter
import com.quietgrid.engine.journey.JourneyStop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JourneyRulesTest {
    private val chapter = JourneyChapter(
        index = 1,
        minRating = 100,
        maxRating = 400,
        stops = (1..10).map { JourneyStop("s$it", "easy", 100 + it * 20, 90) },
    )

    private fun ids(vararg numbers: Int) = numbers.map { "s$it" }.toSet()

    private val clean = JourneySignals(hintsUsed = 0, mistakes = 0)

    @Test
    fun `fresh chapter opens the first four stops`() {
        assertEquals(ids(1, 2, 3, 4), playableStops(chapter, emptyMap()))
    }

    @Test
    fun `cleared and skipped stops stay open and the window moves past them`() {
        val progress = mapOf(
            "s1" to JourneyStopProgress(stars = 1, cleared = true),
            "s2" to JourneyStopProgress(skipped = true),
        )
        assertEquals(ids(1, 2, 3, 4, 5, 6), playableStops(chapter, progress))
    }

    @Test
    fun `current stop skips cleared and skipped stops`() {
        val progress = mapOf(
            "s1" to JourneyStopProgress(stars = 2, cleared = true),
            "s2" to JourneyStopProgress(skipped = true),
            "s3" to JourneyStopProgress(attempts = 2, nextPuzzleId = "x"),
        )
        assertEquals("s3", currentStop(chapter, progress)?.puzzleId)
    }

    @Test
    fun `current stop is null when every stop is cleared`() {
        val progress = chapter.stops.associate { it.puzzleId to JourneyStopProgress(stars = 1, cleared = true) }
        assertNull(currentStop(chapter, progress))
    }

    @Test
    fun `clean fast win earns three stars`() {
        val won = applyJourneyWin(JourneyStopProgress(), clean, elapsedSeconds = 60, targetSeconds = 90)
        assertEquals(3, won.stars)
        assertTrue(won.cleared)
        assertTrue(won.clean)
        assertTrue(won.underTarget)
        assertEquals(60, won.bestSeconds)
        assertEquals(1, won.attempts)
    }

    @Test
    fun `win with a hint earns two at most`() {
        val won = applyJourneyWin(JourneyStopProgress(), JourneySignals(hintsUsed = 1, mistakes = 0), 60, 90)
        assertEquals(2, won.stars)
        assertFalse(won.clean)
        assertTrue(won.underTarget)
    }

    @Test
    fun `slow win with a mistake earns one`() {
        val won = applyJourneyWin(JourneyStopProgress(), JourneySignals(hintsUsed = 0, mistakes = 1), 200, 90)
        assertEquals(1, won.stars)
        assertTrue(won.cleared)
        assertFalse(won.clean)
        assertFalse(won.underTarget)
    }

    @Test
    fun `elapsed equal to target is not under target`() {
        val won = applyJourneyWin(JourneyStopProgress(), clean, elapsedSeconds = 90, targetSeconds = 90)
        assertFalse(won.underTarget)
        assertEquals(2, won.stars)
    }

    @Test
    fun `a worse replay never lowers stars or flags`() {
        val best = applyJourneyWin(JourneyStopProgress(), clean, 50, 90)
        val replay = applyJourneyWin(best, JourneySignals(hintsUsed = 2, mistakes = 3), 300, 90)
        assertEquals(3, replay.stars)
        assertTrue(replay.cleared)
        assertTrue(replay.clean)
        assertTrue(replay.underTarget)
        assertEquals(50, replay.bestSeconds)
        assertEquals(2, replay.attempts)
    }

    @Test
    fun `win clears a pending sibling`() {
        val lost = applyJourneyLoss(JourneyStopProgress(), "sib")
        assertNull(applyJourneyWin(lost, clean, 60, 90).nextPuzzleId)
    }

    @Test
    fun `loss keeps stars and stores the sibling`() {
        val old = JourneyStopProgress(stars = 2, cleared = true, clean = true, bestSeconds = 70, attempts = 3)
        val lost = applyJourneyLoss(old, "sib")
        assertEquals(old.copy(attempts = 4, nextPuzzleId = "sib"), lost)
    }

    @Test
    fun `sibling is nearest rated, skips stops and played puzzles, and is deterministic for a seed`() {
        val stop = chapter.stops[2]
        val pool = listOf(
            "s4" to stop.rating,
            "played" to stop.rating + 1,
            "near-a" to stop.rating + 3,
            "near-b" to stop.rating - 3,
            "far" to stop.rating + 40,
            "out-of-band" to 900,
        ) + chapter.stops.map { it.puzzleId to it.rating }
        val picks = (0 until 20).map { pickJourneySibling(stop, chapter, pool, setOf("played"), seed = it) }.toSet()
        assertEquals(setOf("near-a", "near-b"), picks)
        assertEquals(
            pickJourneySibling(stop, chapter, pool, setOf("played"), seed = 7),
            pickJourneySibling(stop, chapter, pool, setOf("played"), seed = 7),
        )
    }

    @Test
    fun `sibling falls back to played puzzles, then to the stop itself`() {
        val stop = chapter.stops[0]
        val pool = listOf("a" to stop.rating + 5, "b" to stop.rating + 9)
        assertEquals("a", pickJourneySibling(stop, chapter, pool, setOf("a", "b"), seed = 1))
        assertEquals(stop.puzzleId, pickJourneySibling(stop, chapter, chapter.stops.map { it.puzzleId to it.rating }, emptySet(), seed = 1))
    }

    @Test
    fun `replay cycles through the stop's transforms`() {
        assertEquals(listOf(5, 7, 2, 5), (1..4).map { replayTransform(it, listOf(2, 5, 7)) })
    }

    @Test
    fun `no transforms replays the original`() {
        assertEquals(0, replayTransform(3, emptyList()))
    }

    @Test
    fun `sibling seed depends on scope, stop and attempt`() {
        val seed = journeySiblingSeed("animaldoku", "s1", 1)
        assertEquals(seed, journeySiblingSeed("animaldoku", "s1", 1))
        assertTrue(seed != journeySiblingSeed("animaldoku", "s1", 2))
        assertTrue(seed != journeySiblingSeed("arrowescape", "s1", 1))
    }
}
