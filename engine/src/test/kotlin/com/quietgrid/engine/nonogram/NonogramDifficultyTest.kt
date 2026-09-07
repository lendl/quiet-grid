package com.quietgrid.engine.nonogram

import com.quietgrid.engine.core.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
        chainDepth: Int,
        signatures: Int = 1,
        duplicateTrickRatio: Double = 0.0,
        hardestTierRepeats: Int = 1,
        realStepCount: Int = 4,
        multiSegmentLineCount: Int = 3,
    ) = NonogramDifficultyMetrics(
        steps = hardestTierRepeats,
        filledCells = 0,
        clueSegments = 0,
        avgPlacementsAtDeduction = 0.0,
        maxPlacementsAtDeduction = 0,
        singleCellStepCount = 0,
        crossAxisUnlocks = 0,
        hardestLineTier = tier,
        hardestTierRepeats = hardestTierRepeats,
        openingLineTier = tier,
        freebieFillRatio = 0.0,
        freebieLineRatio = 0.0,
        maxChainDepth = chainDepth,
        distinctTechniqueSignatures = signatures,
        duplicateTrickRatio = duplicateTrickRatio,
        realStepCount = realStepCount,
        multiSegmentLineCount = multiSegmentLineCount,
    )

    @Test
    fun `a freebie-only peak is easy regardless of board size (real puzzles like this are rejected as degenerate before reaching classify)`() {
        assertEquals(Difficulty.EASY, classifyNonogramDifficulty(10, 5, metricsWith(NonogramLineTier.FREEBIE, chainDepth = 0, signatures = 0)))
        assertEquals(Difficulty.EASY, classifyNonogramDifficulty(10, 10, metricsWith(NonogramLineTier.FREEBIE, chainDepth = 0, signatures = 0)))
    }

    // Chain depth/signature thresholds now scale with board size (chainDepthUnit/signatureUnit
    // in NonogramDifficulty.kt) instead of being flat constants. 5x5 gives the simplest whole
    // numbers to test boundaries with: depthUnit=max(1,25/10)=2, sigUnit=max(2,10/5)=2.

    @Test
    fun `shallow chain depth (within one board-scaled unit) is easy with few technique signatures, medium with more variety`() {
        assertEquals(Difficulty.EASY, classifyNonogramDifficulty(5, 5, metricsWith(NonogramLineTier.DEPENDENT, chainDepth = 2, signatures = 1)))
        assertEquals(Difficulty.EASY, classifyNonogramDifficulty(5, 5, metricsWith(NonogramLineTier.DEPENDENT, chainDepth = 2, signatures = 2)))
        assertEquals(Difficulty.MEDIUM, classifyNonogramDifficulty(5, 5, metricsWith(NonogramLineTier.DEPENDENT, chainDepth = 2, signatures = 3)))
    }

    @Test
    fun `chain depth within two board-scaled units is medium with modest variety, hard with more`() {
        assertEquals(Difficulty.MEDIUM, classifyNonogramDifficulty(5, 5, metricsWith(NonogramLineTier.DEPENDENT, chainDepth = 3, signatures = 4)))
        assertEquals(Difficulty.HARD, classifyNonogramDifficulty(5, 5, metricsWith(NonogramLineTier.DEPENDENT, chainDepth = 3, signatures = 5)))
    }

    @Test
    fun `very deep chain (beyond two board-scaled units) is hard on a narrow board and expert on a wide enough one`() {
        // 10x5: depthUnit=5, hardDepthCeiling=10, shortSide=5 (<10) so it can never reach expert via depth alone.
        assertEquals(Difficulty.HARD, classifyNonogramDifficulty(10, 5, metricsWith(NonogramLineTier.DEPENDENT, chainDepth = 25, signatures = 10)))
        // 10x10: depthUnit=10, hardDepthCeiling=20, shortSide=10 (>=10) so a sufficiently deep chain reaches expert.
        assertEquals(Difficulty.EXPERT, classifyNonogramDifficulty(10, 10, metricsWith(NonogramLineTier.DEPENDENT, chainDepth = 25, signatures = 10)))
    }

    @Test
    fun `probing always classifies expert regardless of chain depth`() {
        assertEquals(Difficulty.EXPERT, classifyNonogramDifficulty(10, 5, metricsWith(NonogramLineTier.PROBING, chainDepth = 1, signatures = 1)))
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
    fun `isExtremeNonogramPuzzle is false below the probing repeat floor`() {
        assertEquals(false, isExtremeNonogramPuzzle(metricsWith(NonogramLineTier.PROBING, chainDepth = 1, hardestTierRepeats = 3)))
    }

    @Test
    fun `isExtremeNonogramPuzzle is true at or above the probing repeat floor`() {
        assertEquals(true, isExtremeNonogramPuzzle(metricsWith(NonogramLineTier.PROBING, chainDepth = 1, hardestTierRepeats = 4)))
    }

    @Test
    fun `isExtremeNonogramPuzzle is false for a dependent peak no matter how many repeats`() {
        assertEquals(false, isExtremeNonogramPuzzle(metricsWith(NonogramLineTier.DEPENDENT, chainDepth = 1, hardestTierRepeats = 30)))
    }

    @Test
    fun `isDegenerateNonogramPuzzle is true for a freebie-only peak no matter how many steps it took`() {
        assertEquals(true, isDegenerateNonogramPuzzle(5, 5, Difficulty.EASY, metricsWith(NonogramLineTier.FREEBIE, chainDepth = 0, hardestTierRepeats = 1)))
        assertEquals(true, isDegenerateNonogramPuzzle(5, 5, Difficulty.EASY, metricsWith(NonogramLineTier.FREEBIE, chainDepth = 0, hardestTierRepeats = 20)))
    }

    @Test
    fun `isDegenerateNonogramPuzzle is true when the same trick is just repeated across symmetric lines`() {
        val metrics = metricsWith(NonogramLineTier.DEPENDENT, chainDepth = 1, signatures = 4, duplicateTrickRatio = 0.78)
        assertEquals(true, isDegenerateNonogramPuzzle(5, 5, Difficulty.MEDIUM, metrics))
    }

    @Test
    fun `isDegenerateNonogramPuzzle is true when most of the picture is just freebie lines, even with some real steps`() {
        val metrics = metricsWith(NonogramLineTier.DEPENDENT, chainDepth = 1, signatures = 2, duplicateTrickRatio = 0.0)
            .copy(freebieFillRatio = 0.75)
        assertEquals(true, isDegenerateNonogramPuzzle(5, 5, Difficulty.MEDIUM, metrics))
    }

    @Test
    fun `isDegenerateNonogramPuzzle is true for hard or expert with too few distinct real techniques, even at a low duplicate ratio`() {
        val metrics = metricsWith(NonogramLineTier.DEPENDENT, chainDepth = 3, signatures = 5, duplicateTrickRatio = 0.44)
        assertEquals(true, isDegenerateNonogramPuzzle(5, 5, Difficulty.HARD, metrics))
        assertEquals(true, isDegenerateNonogramPuzzle(5, 5, Difficulty.EXPERT, metrics))
    }

    @Test
    fun `isDegenerateNonogramPuzzle is true for a solid-blob shape where every line has only one clue segment`() {
        val metrics = metricsWith(NonogramLineTier.SELF_CONTAINED, chainDepth = 1, signatures = 2, multiSegmentLineCount = 0)
        assertEquals(true, isDegenerateNonogramPuzzle(5, 5, Difficulty.EASY, metrics))
    }

    @Test
    fun `isDegenerateNonogramPuzzle is false once enough lines actually have multiple clue segments`() {
        val metrics = metricsWith(NonogramLineTier.SELF_CONTAINED, chainDepth = 1, signatures = 2, multiSegmentLineCount = 3)
        assertEquals(false, isDegenerateNonogramPuzzle(5, 5, Difficulty.EASY, metrics))
    }

    @Test
    fun `the same low signature count is fine for easy or medium, where a small technique set is expected`() {
        val metrics = metricsWith(NonogramLineTier.DEPENDENT, chainDepth = 1, signatures = 2, duplicateTrickRatio = 0.0)
        assertEquals(false, isDegenerateNonogramPuzzle(5, 5, Difficulty.EASY, metrics))
        assertEquals(false, isDegenerateNonogramPuzzle(5, 5, Difficulty.MEDIUM, metrics))
    }

    @Test
    fun `probing is exempt from the hard-expert distinct-signature floor - the contradiction step is evidence enough`() {
        val metrics = metricsWith(NonogramLineTier.PROBING, chainDepth = 3, signatures = 1, duplicateTrickRatio = 0.0)
        assertEquals(false, isDegenerateNonogramPuzzle(5, 5, Difficulty.EXPERT, metrics))
    }

    @Test
    fun `isDegenerateNonogramPuzzle is false once real technique is varied enough, not just repeated`() {
        assertEquals(false, isDegenerateNonogramPuzzle(5, 5, Difficulty.MEDIUM, metricsWith(NonogramLineTier.SELF_CONTAINED, chainDepth = 1, duplicateTrickRatio = 0.0)))
        assertEquals(false, isDegenerateNonogramPuzzle(5, 5, Difficulty.MEDIUM, metricsWith(NonogramLineTier.DEPENDENT, chainDepth = 2, duplicateTrickRatio = 0.4)))
        assertEquals(false, isDegenerateNonogramPuzzle(5, 5, Difficulty.EXPERT, metricsWith(NonogramLineTier.PROBING, chainDepth = 3, duplicateTrickRatio = 0.0)))
    }

    @Test
    fun `two solid rectangles are junk by duplicate-trick-ratio even though the trace does chain a little`() {
        // The exact 10x10 shape traced by hand during the difficulty redesign investigation:
        // two solid rectangles, each edge solved via the identical "flanking zero-clue lines
        // already eliminated the rest" deduction, repeated 18 times under the old repeat-count
        // metric. The corrected trace does find a real (if narrow) cross-axis chain - one column
        // needs several rows resolved first, and those rows in turn get finished off using that
        // column - reaching depth 4. But distinctTechniqueSignatures stays tiny (a handful of
        // distinct clues) against a much larger real-step count, so duplicateTrickRatio still
        // correctly flags this as junk: whatever the exact chain depth, it's the same few tricks
        // copy-pasted across a symmetric shape, not genuine variety.
        val blockA = listOf(false, true, true, true, false, false, false, false, false, false)
        val blockB = listOf(false, false, false, false, false, true, true, true, true, false)
        val blank = List(10) { false }
        val solution = listOf(blockA, blockA, blockA, blank, blockB, blockB, blockB, blockB, blockB, blank)
        val rowClues = solution.map { buildNonogramClues(it) }
        val colClues = (0 until 10).map { c -> buildNonogramClues(solution.map { it[c] }) }
        val metrics = analyzeNonogramDifficulty(rowClues, colClues, solution)
        assertNotNull(metrics)
        assertTrue("expected a shallow chain, got depth ${metrics!!.maxChainDepth}", metrics.maxChainDepth <= 4)
        assertTrue("expected high duplication, got ${metrics.duplicateTrickRatio}", metrics.duplicateTrickRatio > 0.5)
        val difficulty = classifyNonogramDifficulty(10, 10, metrics)
        assertEquals(true, isDegenerateNonogramPuzzle(10, 10, difficulty, metrics))
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
