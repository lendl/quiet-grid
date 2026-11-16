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

    private fun win(old: JourneyStopProgress, signals: JourneySignals, elapsedSeconds: Int, targetSeconds: Int, score: Int = 0) =
        applyJourneyWin(old, journeyRun(signals, elapsedSeconds, targetSeconds), elapsedSeconds, score)

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
        val won = win(JourneyStopProgress(), clean, elapsedSeconds = 60, targetSeconds = 90)
        assertEquals(3, won.stars)
        assertTrue(won.cleared)
        assertTrue(won.clean)
        assertTrue(won.underTarget)
        assertEquals(60, won.bestSeconds)
        assertEquals(1, won.attempts)
    }

    @Test
    fun `win with a hint earns two at most`() {
        val won = win(JourneyStopProgress(), JourneySignals(hintsUsed = 1, mistakes = 0), 60, 90)
        assertEquals(2, won.stars)
        assertFalse(won.clean)
        assertTrue(won.underTarget)
    }

    @Test
    fun `slow win with a mistake earns one`() {
        val won = win(JourneyStopProgress(), JourneySignals(hintsUsed = 0, mistakes = 1), 200, 90)
        assertEquals(1, won.stars)
        assertTrue(won.cleared)
        assertFalse(won.clean)
        assertFalse(won.underTarget)
    }

    @Test
    fun `elapsed equal to target is not under target`() {
        val won = win(JourneyStopProgress(), clean, elapsedSeconds = 90, targetSeconds = 90)
        assertFalse(won.underTarget)
        assertEquals(2, won.stars)
    }

    @Test
    fun `a worse replay never lowers stars or flags`() {
        val best = win(JourneyStopProgress(), clean, 50, 90, score = 800)
        val replay = win(best, JourneySignals(hintsUsed = 2, mistakes = 3), 300, 90, score = 200)
        assertEquals(3, replay.stars)
        assertTrue(replay.cleared)
        assertTrue(replay.clean)
        assertTrue(replay.underTarget)
        assertEquals(50, replay.bestSeconds)
        assertEquals(800, replay.bestScore)
        assertEquals(2, replay.attempts)
    }

    @Test
    fun `a higher scoring replay raises the best score`() {
        val first = win(JourneyStopProgress(), clean, 50, 90, score = 400)
        assertEquals(400, first.bestScore)
        assertEquals(900, win(first, clean, 40, 90, score = 900).bestScore)
    }

    @Test
    fun `win clears a pending sibling`() {
        val lost = applyJourneyLoss(JourneyStopProgress(), "sib")
        assertNull(win(lost, clean, 60, 90).nextPuzzleId)
    }

    @Test
    fun `loss keeps stars and stores the sibling`() {
        val old = JourneyStopProgress(stars = 2, cleared = true, clean = true, bestSeconds = 70, attempts = 3)
        val lost = applyJourneyLoss(old, "sib")
        assertEquals(old.copy(attempts = 4, nextPuzzleId = "sib"), lost)
    }

    private val levels = (0 until 60).map { JourneyStop("L$it", "easy", it * 10 + 5, 90) }
    private val ratedBank = (0 until 600).map { "p$it" to it }

    @Test
    fun `levels split the bank into back-to-back score ranges that never overlap`() {
        val ranges = journeyLevelRanges(levels, ratedBank)

        assertEquals(60, ranges.size)
        assertEquals(listOf(0 to 9, 10 to 19, 20 to 29), ranges.take(3).map { it.minRating to it.maxRating })
        ranges.zipWithNext().forEach { (a, b) -> assertTrue(a.maxRating < b.minRating) }
        assertEquals(ratedBank.size + levels.size, ranges.sumOf { it.puzzles.size })
        ranges.forEach { range -> assertTrue(range.puzzles.any { it.first == range.stopId }) }
    }

    private fun spareIds(range: JourneyLevelRange) = range.puzzles.map { it.first }.filter { it != range.stopId }

    @Test
    fun `levels bunched below the bank still get four other puzzles each, nearest first`() {
        val bunched = listOf(10, 11, 12).map { JourneyStop("L$it", "easy", it, 90) }
        val bank = (20 until 40).map { "p$it" to it }

        val ranges = journeyLevelRanges(bunched, bank)

        assertEquals((20 until 24).map { "p$it" }, spareIds(ranges[0]))
        assertEquals((24 until 28).map { "p$it" }, spareIds(ranges[1]))
        assertEquals((28 until 40).map { "p$it" }, spareIds(ranges[2]))
    }

    @Test
    fun `levels bunched above the bank still get four other puzzles each, nearest first`() {
        val bunched = listOf(30, 31, 32).map { JourneyStop("L$it", "easy", it, 90) }
        val bank = (0 until 20).map { "p$it" to it }

        val ranges = journeyLevelRanges(bunched, bank)

        assertEquals(listOf(12, 4, 4), ranges.map { spareIds(it).size })
        assertEquals((16 until 20).map { "p$it" }, spareIds(ranges[2]))
    }

    @Test
    fun `a tier with too few puzzles shares them out as evenly as it can`() {
        val bunched = listOf(10, 11, 12).map { JourneyStop("L$it", "easy", it, 90) }
        val bank = (20 until 27).map { "p$it" to it }

        assertEquals(listOf(2, 2, 3), journeyLevelRanges(bunched, bank).map { spareIds(it).size })
    }

    @Test
    fun `replay comes from the level's own range and skips retired and reserved puzzles`() {
        val ranges = journeyLevelRanges(levels, ratedBank)
        val own = ranges[1].puzzles.map { it.first }.filter { it != "L1" }

        val first = pickJourneyReplay("L1", ranges, mapOf("L1" to listOf("L1")), emptySet())
        assertTrue(first in own)
        val second = pickJourneyReplay("L1", ranges, mapOf("L1" to listOf("L1", first!!)), emptySet())
        assertTrue(second in own && second != first)
        val reserved = pickJourneyReplay("L1", ranges, mapOf("L1" to listOf("L1")), setOf(first))
        assertTrue(reserved in own && reserved != first)
        val retiredElsewhere = pickJourneyReplay("L1", ranges, mapOf("L1" to listOf("L1"), "L2" to listOf(first)), emptySet())
        assertTrue(retiredElsewhere in own && retiredElsewhere != first)
    }

    @Test
    fun `an emptied range opens up again, least recently played first and never the previous one`() {
        val ranges = journeyLevelRanges(levels, ratedBank)
        val own = ranges[1].puzzles.map { it.first }
        val played = listOf("L1") + own.filter { it != "L1" }

        val pick = pickJourneyReplay("L1", ranges, mapOf("L1" to played), emptySet())

        assertEquals("L1", pick)
        assertEquals(own.first { it != "L1" }, pickJourneyReplay("L1", ranges, mapOf("L1" to played + "L1"), emptySet()))
    }

    @Test
    fun `a short range borrows every unused neighbour puzzle before repeating its own`() {
        val stops = listOf(10, 20, 30).map { JourneyStop("L$it", "easy", it, 90) }
        val ranges = journeyLevelRanges(stops, listOf("x" to 11, "y" to 21, "z" to 31))
        val played = (1..4).fold(listOf("L10")) { acc, _ -> acc + pickJourneyReplay("L10", ranges, mapOf("L10" to acc), emptySet())!! }

        assertEquals(listOf("L10", "x", "y", "z", "L10"), played)
    }

    @Test
    fun `a range with only the level's own puzzle borrows the nearest unused one from a neighbour`() {
        val ranges = journeyLevelRanges(listOf(JourneyStop("a", "easy", 10, 90), JourneyStop("b", "easy", 20, 90)), listOf("x" to 30))

        assertEquals("x", pickJourneyReplay("a", ranges, mapOf("a" to listOf("a")), emptySet()))
    }

    @Test
    fun `once nothing unused is left the least recently played comes back, never the previous one`() {
        val ranges = journeyLevelRanges(listOf(JourneyStop("a", "easy", 10, 90)), listOf("b" to 11, "c" to 12))

        assertEquals("a", pickJourneyReplay("a", ranges, mapOf("a" to listOf("a", "b", "c")), emptySet()))
        assertEquals("b", pickJourneyReplay("a", ranges, mapOf("a" to listOf("a", "b", "c", "a")), emptySet()))
        assertEquals("c", pickJourneyReplay("a", ranges, mapOf("a" to listOf("a", "b", "c", "a")), setOf("b")))
    }

    @Test
    fun `replay never repeats the previous puzzle over many plays`() {
        val ranges = journeyLevelRanges(listOf(JourneyStop("a", "easy", 10, 90)), listOf("b" to 11))
        val played = (1..6).fold(listOf("a")) { acc, _ -> acc + pickJourneyReplay("a", ranges, mapOf("a" to acc), emptySet())!! }

        assertEquals(listOf("a", "b", "a", "b", "a", "b", "a"), played)
    }

    @Test
    fun `a level with no other puzzle anywhere has no replay`() {
        val ranges = journeyLevelRanges(listOf(JourneyStop("a", "easy", 10, 90)), emptyList())

        assertNull(pickJourneyReplay("a", ranges, mapOf("a" to listOf("a")), emptySet()))
    }

    @Test
    fun `when the whole tier is used up it reuses another level's puzzle rather than the previous one`() {
        val stops = listOf(JourneyStop("a", "easy", 10, 90), JourneyStop("b", "easy", 20, 90))

        val pick = pickJourneyReplay("a", journeyLevelRanges(stops, emptyList()), mapOf("a" to listOf("a"), "b" to listOf("b")), emptySet())

        assertEquals("b", pick)
    }

    @Test
    fun `replay cycles through the stop's transforms`() {
        assertEquals(listOf(5, 7, 2, 5), (1..4).map { replayTransform(it, listOf(2, 5, 7)) })
    }

    @Test
    fun `no transforms replays the original`() {
        assertEquals(0, replayTransform(3, emptyList()))
    }
}
