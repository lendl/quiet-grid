package com.quietgrid.engine.nonogram

import com.quietgrid.engine.core.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NonogramDifficultyTest {
    @Test
    fun `buildNonogramClues finds run-lengths, defaulting to zero for a blank line`() {
        assertEquals(listOf(2, 1), buildNonogramClues(listOf(true, true, false, true)))
        assertEquals(listOf(0), buildNonogramClues(listOf(false, false, false)))
    }

    @Test
    fun `analyzeNonogramDifficulty solves a trivial fully-overlap-fillable 5x5 puzzle`() {
        val solution = listOf(
            listOf(true, true, true, true, true),
            listOf(false, false, false, false, false),
            listOf(true, true, true, true, true),
            listOf(false, false, false, false, false),
            listOf(true, true, true, true, true),
        )
        val rowClues = solution.map { buildNonogramClues(it) }
        val colClues = (0 until 5).map { c -> buildNonogramClues(solution.map { it[c] }) }
        val metrics = analyzeNonogramDifficulty(rowClues, colClues, solution)
        assertNotNull(metrics)
        assertEquals(15, metrics!!.filledCells)
    }

    @Test
    fun `classifyNonogramDifficulty returns easy for the trivial 5x5 puzzle above`() {
        val solution = listOf(
            listOf(true, true, true, true, true),
            listOf(false, false, false, false, false),
            listOf(true, true, true, true, true),
            listOf(false, false, false, false, false),
            listOf(true, true, true, true, true),
        )
        val rowClues = solution.map { buildNonogramClues(it) }
        val colClues = (0 until 5).map { c -> buildNonogramClues(solution.map { it[c] }) }
        val metrics = analyzeNonogramDifficulty(rowClues, colClues, solution)!!
        assertEquals(Difficulty.EASY, classifyNonogramDifficulty(5, 5, metrics))
    }

    @Test
    fun `classifyNonogramLineTier is freebie for a zero-slack clue, full or not`() {
        assertEquals(NonogramLineTier.FREEBIE, classifyNonogramLineTier(5, listOf(5)))
        assertEquals(NonogramLineTier.FREEBIE, classifyNonogramLineTier(5, listOf(3, 1)))
    }

    @Test
    fun `classifyNonogramLineTier is self-contained when a blank line still yields an overlap cell`() {
        assertEquals(NonogramLineTier.SELF_CONTAINED, classifyNonogramLineTier(5, listOf(4)))
    }

    @Test
    fun `classifyNonogramLineTier is dependent when a blank line yields no overlap cell at all`() {
        assertEquals(NonogramLineTier.DEPENDENT, classifyNonogramLineTier(5, listOf(2)))
    }

    private fun metricsWith(
        tier: NonogramLineTier,
        repeats: Int,
        opening: NonogramLineTier = tier,
        crossAxisUnlocks: Int = 10,
        freebieLineRatio: Double = 0.0,
    ) = NonogramDifficultyMetrics(
        steps = repeats,
        filledCells = 0,
        clueSegments = 0,
        avgPlacementsAtDeduction = 0.0,
        maxPlacementsAtDeduction = 0,
        singleCellStepCount = 0,
        crossAxisUnlocks = crossAxisUnlocks,
        hardestLineTier = tier,
        hardestTierRepeats = repeats,
        openingLineTier = opening,
        freebieFillRatio = 0.0,
        freebieLineRatio = freebieLineRatio,
    )

    @Test
    fun `a freebie peak is easy on a small board but demotes to medium once the board gets too big to stay quick`() {
        assertEquals(Difficulty.EASY, classifyNonogramDifficulty(10, 5, metricsWith(NonogramLineTier.FREEBIE, 20)))
        assertEquals(Difficulty.MEDIUM, classifyNonogramDifficulty(10, 10, metricsWith(NonogramLineTier.FREEBIE, 20)))
    }

    @Test
    fun `self-contained peak is medium below the repeat floor and hard at or above it`() {
        assertEquals(Difficulty.MEDIUM, classifyNonogramDifficulty(10, 5, metricsWith(NonogramLineTier.SELF_CONTAINED, 7)))
        assertEquals(Difficulty.HARD, classifyNonogramDifficulty(10, 5, metricsWith(NonogramLineTier.SELF_CONTAINED, 8)))
    }

    @Test
    fun `dependent peak escalates medium then hard then expert as repeats climb, on a large enough board`() {
        val opening = NonogramLineTier.SELF_CONTAINED
        assertEquals(Difficulty.MEDIUM, classifyNonogramDifficulty(10, 10, metricsWith(NonogramLineTier.DEPENDENT, 2, opening)))
        assertEquals(Difficulty.HARD, classifyNonogramDifficulty(10, 10, metricsWith(NonogramLineTier.DEPENDENT, 3, opening)))
        assertEquals(Difficulty.EXPERT, classifyNonogramDifficulty(10, 10, metricsWith(NonogramLineTier.DEPENDENT, 17, opening)))
    }

    @Test
    fun `dependent peak at expert-grade repeats stays capped at hard on a narrow board`() {
        assertEquals(Difficulty.HARD, classifyNonogramDifficulty(10, 5, metricsWith(NonogramLineTier.DEPENDENT, 30)))
    }

    @Test
    fun `dependent peak with enough repeats still stays medium if the lines never actually cross-reference`() {
        val metrics = metricsWith(NonogramLineTier.DEPENDENT, repeats = 4, opening = NonogramLineTier.SELF_CONTAINED, crossAxisUnlocks = 2)
        assertEquals(Difficulty.MEDIUM, classifyNonogramDifficulty(10, 10, metrics))
    }

    @Test
    fun `dependent peak promotes to hard once both the repeat and cross-axis floors are met`() {
        val metrics = metricsWith(NonogramLineTier.DEPENDENT, repeats = 4, opening = NonogramLineTier.SELF_CONTAINED, crossAxisUnlocks = 4)
        assertEquals(Difficulty.HARD, classifyNonogramDifficulty(10, 10, metrics))
    }

    @Test
    fun `a puzzle unsolvable by line logic alone resolves via probing and classifies as expert`() {
        val solution = listOf(
            listOf(false, false, true, true, false),
            listOf(false, false, true, true, false),
            listOf(true, true, false, false, false),
            listOf(true, true, false, false, true),
            listOf(true, false, true, false, false),
            listOf(true, true, false, false, false),
            listOf(false, true, false, true, true),
            listOf(false, true, false, false, true),
            listOf(false, false, false, true, true),
            listOf(true, false, true, false, false),
        )
        val rowClues = solution.map { buildNonogramClues(it) }
        val colClues = (0 until 5).map { c -> buildNonogramClues(solution.map { it[c] }) }
        val metrics = analyzeNonogramDifficulty(rowClues, colClues, solution)
        assertNotNull(metrics)
        assertEquals(NonogramLineTier.PROBING, metrics!!.hardestLineTier)
        assertEquals(1, metrics.hardestTierRepeats)
        assertEquals(Difficulty.EXPERT, classifyNonogramDifficulty(10, 5, metrics))
    }

    @Test
    fun `a puzzle that opens harder than mediums ceiling escalates to hard`() {
        val metrics = metricsWith(NonogramLineTier.SELF_CONTAINED, repeats = 2, opening = NonogramLineTier.DEPENDENT)
        assertEquals(Difficulty.HARD, classifyNonogramDifficulty(10, 10, metrics))
    }

    @Test
    fun `a puzzle that opens harder than both mediums and hards ceiling escalates all the way to expert`() {
        val metrics = metricsWith(NonogramLineTier.SELF_CONTAINED, repeats = 2, opening = NonogramLineTier.PROBING)
        assertEquals(Difficulty.EXPERT, classifyNonogramDifficulty(10, 10, metrics))
    }

    @Test
    fun `a hard-bound puzzle opening within its ceiling does not escalate`() {
        val metrics = metricsWith(NonogramLineTier.SELF_CONTAINED, repeats = 8, opening = NonogramLineTier.DEPENDENT)
        assertEquals(Difficulty.HARD, classifyNonogramDifficulty(10, 10, metrics))
    }

    @Test
    fun `isExtremeNonogramPuzzle is false below the probing repeat floor`() {
        assertEquals(false, isExtremeNonogramPuzzle(metricsWith(NonogramLineTier.PROBING, 3)))
    }

    @Test
    fun `isExtremeNonogramPuzzle is true at or above the probing repeat floor`() {
        assertEquals(true, isExtremeNonogramPuzzle(metricsWith(NonogramLineTier.PROBING, 4)))
    }

    @Test
    fun `isExtremeNonogramPuzzle is false for a dependent peak no matter how many repeats`() {
        assertEquals(false, isExtremeNonogramPuzzle(metricsWith(NonogramLineTier.DEPENDENT, 30)))
    }

    @Test
    fun `a genuinely ambiguous puzzle still fails to resolve, even with probing available`() {
        val solution = listOf(
            listOf(true, false),
            listOf(false, true),
        )
        val rowClues = solution.map { buildNonogramClues(it) }
        val colClues = (0 until 2).map { c -> buildNonogramClues(solution.map { it[c] }) }
        assertNull(analyzeNonogramDifficulty(rowClues, colClues, solution))
    }
}
