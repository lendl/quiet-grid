package com.quietgrid.engine.wordsearch

import com.quietgrid.engine.core.Difficulty
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSearchQualityTest {
    @Test
    fun `buildQualityMetrics returns non-negative overlap ratio and direction entropy in 0 to 1`() {
        val placements = listOf(
            WordPlacement("1", "CAT", WSCellRef(0, 0), WordSearchDirection.RIGHT, listOf(WSCellRef(0, 0), WSCellRef(0, 1), WSCellRef(0, 2))),
            WordPlacement("2", "DOG", WSCellRef(0, 2), WordSearchDirection.DOWN, listOf(WSCellRef(0, 2), WSCellRef(1, 2), WSCellRef(2, 2))),
        )
        val metrics = buildQualityMetrics(placements)
        assertTrue(metrics.overlapRatio in 0.0..1.0)
        assertTrue(metrics.directionEntropy in 0.0..1.0)
    }

    @Test
    fun `WORD_SEARCH_DIFFICULTY_CONFIG has an entry for all 4 difficulties`() {
        assertTrue(Difficulty.entries.all { WORD_SEARCH_DIFFICULTY_CONFIG.containsKey(it) })
    }
}
