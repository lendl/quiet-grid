package com.quietgrid.engine.sudoku

import com.quietgrid.engine.core.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SudokuDifficultyTest {
    private val solved: SudokuGrid = listOf(
        listOf(5, 3, 4, 6, 7, 8, 9, 1, 2),
        listOf(6, 7, 2, 1, 9, 5, 3, 4, 8),
        listOf(1, 9, 8, 3, 4, 2, 5, 6, 7),
        listOf(8, 5, 9, 7, 6, 1, 4, 2, 3),
        listOf(4, 2, 6, 8, 5, 3, 7, 9, 1),
        listOf(7, 1, 3, 9, 2, 4, 8, 5, 6),
        listOf(9, 6, 1, 5, 3, 7, 2, 8, 4),
        listOf(2, 8, 7, 4, 1, 9, 6, 3, 5),
        listOf(3, 4, 5, 2, 8, 6, 1, 7, 9),
    )

    @Test
    fun `a single naked-single puzzle classifies as easy`() {
        val puzzle = solved.mapIndexed { r, row -> row.mapIndexed { c, v -> if (r == 0 && c == 0) null else v } }
        val trace = traceHumanSolve(puzzle)
        val metrics = collectDifficultyMetrics(trace.moves)
        val classification = classifyDifficulty(metrics)
        assertNotNull(classification)
        assertEquals(Difficulty.EASY, classification!!.difficulty)
    }

    // --- Synthetic move-list helpers -----------------------------------------------------
    // The remaining tests build SudokuCanonicalMove lists directly rather than deriving them
    // from traceHumanSolve, so each difficulty bucket and safety-rail branch can be exercised
    // precisely without needing to hand-construct real minimal puzzles for every technique.

    private fun placement(technique: SudokuTechnique, complexity: Int) =
        SudokuPlacementMove(technique, complexity, 0, 0, 1, emptyList(), emptyList())

    private fun elimination(technique: SudokuTechnique, complexity: Int, targets: Int) =
        SudokuCandidateEliminationMove(technique, complexity, List(targets) { Triple(0, it, 1) }, emptyList(), emptyList())

    @Test
    fun `collectDifficultyMetrics aggregates counts, branching stats, and per-technique maxima`() {
        val moves = listOf(
            placement(SudokuTechnique.NAKED_SINGLE, complexity = 0),
            elimination(SudokuTechnique.NAKED_PAIR, complexity = 2, targets = 3),
            elimination(SudokuTechnique.X_WING, complexity = 1, targets = 2),
        )

        val metrics = collectDifficultyMetrics(moves)

        assertEquals(3, metrics.stepCount)
        assertEquals(1, metrics.placementCount)
        assertEquals(5, metrics.candidateEliminationCount) // 3 + 2 elimination targets
        assertEquals(1, metrics.advancedStepCount) // only X_WING floors at HARD
        assertEquals(5.0 / 3.0, metrics.branchingFactor, 1e-9)
        assertEquals(5, metrics.branchingScore) // max(1,3) + max(1,2)
        assertEquals(SudokuTechnique.NAKED_SINGLE, metrics.openingTechnique)
        assertEquals(SudokuTechnique.X_WING, metrics.hardestTechnique)
        assertEquals(
            mapOf(SudokuTechnique.NAKED_SINGLE to 1, SudokuTechnique.NAKED_PAIR to 1, SudokuTechnique.X_WING to 1),
            metrics.techniqueCounts,
        )
        assertEquals(
            mapOf(SudokuTechnique.NAKED_SINGLE to 0, SudokuTechnique.NAKED_PAIR to 2, SudokuTechnique.X_WING to 1),
            metrics.maxComplexityPerTechnique,
        )
    }

    @Test
    fun `computeDifficultyScore matches the hand-computed weighted sum`() {
        val moves = listOf(
            placement(SudokuTechnique.NAKED_SINGLE, complexity = 0),
            elimination(SudokuTechnique.NAKED_PAIR, complexity = 2, targets = 3),
            elimination(SudokuTechnique.X_WING, complexity = 1, targets = 2),
        )
        val metrics = collectDifficultyMetrics(moves)

        // techniqueScore = 1*2 (naked-single) + 1*6 (naked-pair) + 1*17 (x-wing) = 25
        // stepCount*6 (18) + placementCount*4 (4) + candidateEliminationCount*3 (15)
        //   + advancedStepCount*5 (5) + round(branchingFactor*10) (round(16.666..) = 17)
        //   + branchingScore (5) + techniqueScore (25) = 89
        assertEquals(89, computeDifficultyScore(metrics))
    }

    @Test
    fun `classifyDifficulty selects medium when the hardest technique floors at medium and rails pass`() {
        val moves = listOf(
            placement(SudokuTechnique.NAKED_SINGLE, complexity = 2),
            placement(SudokuTechnique.HIDDEN_SINGLE, complexity = 2),
            elimination(SudokuTechnique.BOX_LINE_REDUCTION, complexity = 2, targets = 2),
        )
        val metrics = collectDifficultyMetrics(moves)

        val classification = classifyDifficulty(metrics)

        assertNotNull(classification)
        assertEquals(Difficulty.MEDIUM, classification!!.difficulty)
    }

    @Test
    fun `classifyDifficulty selects hard when the hardest technique floors at hard and rails pass`() {
        val moves = listOf(
            placement(SudokuTechnique.NAKED_SINGLE, complexity = 1),
            elimination(SudokuTechnique.XY_WING, complexity = 3, targets = 2),
        )
        val metrics = collectDifficultyMetrics(moves)

        val classification = classifyDifficulty(metrics)

        assertNotNull(classification)
        assertEquals(Difficulty.HARD, classification!!.difficulty)
    }

    @Test
    fun `classifyDifficulty selects expert when the hardest technique floors at expert and rails pass`() {
        val moves = listOf(
            placement(SudokuTechnique.NAKED_SINGLE, complexity = 1),
            elimination(SudokuTechnique.CHAINS, complexity = 5, targets = 1),
        )
        val metrics = collectDifficultyMetrics(moves)

        val classification = classifyDifficulty(metrics)

        assertNotNull(classification)
        assertEquals(Difficulty.EXPERT, classification!!.difficulty)
    }

    @Test
    fun `classifyDifficulty bumps easy up to medium when the easy step-count safety rail is violated`() {
        // Technique bucket alone would call this EASY (hardest technique is naked-single), but
        // 65 steps exceeds the easy profile's maxStepCount of 64, so classification must walk up
        // to the next difficulty and re-check its rails rather than settling for the raw bucket.
        val moves = List(65) { placement(SudokuTechnique.NAKED_SINGLE, complexity = 1) }
        val metrics = collectDifficultyMetrics(moves)

        val classification = classifyDifficulty(metrics)

        assertNotNull(classification)
        assertEquals(Difficulty.MEDIUM, classification!!.difficulty)
    }

    @Test
    fun `classifyDifficulty returns null when even the expert safety rails are violated`() {
        // Hardest technique is already CHAINS (expert's own ceiling technique), so the technique
        // bucket starts at EXPERT with no higher bucket to escalate to. Exceeding expert's
        // maxStepCount of 280 must therefore make classification give up and return null.
        val moves = List(281) { elimination(SudokuTechnique.CHAINS, complexity = 1, targets = 1) }
        val metrics = collectDifficultyMetrics(moves)

        val classification = classifyDifficulty(metrics)

        assertNull(classification)
    }

    @Test
    fun `classifyDifficulty treats an empty move list as easy with a zero score`() {
        val metrics = collectDifficultyMetrics(emptyList())

        val classification = classifyDifficulty(metrics)

        assertNotNull(classification)
        assertEquals(Difficulty.EASY, classification!!.difficulty)
        assertEquals(0, classification.score)
    }
}
