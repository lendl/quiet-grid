package com.quietgrid.engine.takuzu

import com.quietgrid.engine.core.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TakuzuDifficultyTest {
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

    @Test
    fun `a puzzle using only the weakest technique cannot jump straight to expert on score alone`() {
        val metrics = TakuzuDifficultyMetrics(givenCount = 20)
        metrics.tipUsageCounts.findPairs = 5
        metrics.highestTipLevel = 1
        metrics.openingHighestTipLevel = 1
        metrics.totalMoves = 5

        val expertOnlyScore = 3000
        assertEquals(Difficulty.MEDIUM, classifyTakuzuDifficulty(10, metrics, expertOnlyScore))
    }
}
