package com.quietgrid.engine.takuzu

import com.quietgrid.engine.core.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TakuzuDifficultyTest {
    // Generated via TakuzuSolver.generateSolvedGrid(6), which only ever returns boards that pass
    // hasUniqueLines (all rows/columns distinct, exactly 3 zeros/3 ones per line, no run of 3) --
    // see task-9-report.md "Fixture fix" section for how this replaced a hand-authored, invalid grid.
    private val solution: TakuzuGrid = listOf(
        listOf(1, 1, 0, 1, 0, 0),
        listOf(1, 0, 1, 1, 0, 0),
        listOf(0, 0, 1, 0, 1, 1),
        listOf(1, 1, 0, 0, 1, 0),
        listOf(0, 0, 1, 1, 0, 1),
        listOf(0, 1, 0, 0, 1, 1),
    )

    @Test
    fun `analyzeTakuzuDifficulty records tip usage and total moves for a solvable puzzle`() {
        // Reveal most of the board so it's a trivial (all find-pairs / complete-lines) solve.
        val givens: TakuzuGrid = solution.mapIndexed { r, row ->
            row.mapIndexed { c, v -> if (r == 5 && c >= 4) null else v }
        }
        val metrics = analyzeTakuzuDifficulty(givens, solution)
        assertEquals(34, metrics.givenCount)
        assertEquals(2, metrics.totalMoves)
    }

    @Test
    fun `computeTakuzuDifficultyScore and classifyTakuzuDifficulty produce a bucket for an easy puzzle`() {
        val givens: TakuzuGrid = solution.mapIndexed { r, row ->
            row.mapIndexed { c, v -> if (r == 5 && c >= 4) null else v }
        }
        val metrics = analyzeTakuzuDifficulty(givens, solution)
        val score = computeTakuzuDifficultyScore(6, metrics)
        val difficulty = classifyTakuzuDifficulty(6, metrics, score)
        assertNotNull(difficulty)
    }
}
