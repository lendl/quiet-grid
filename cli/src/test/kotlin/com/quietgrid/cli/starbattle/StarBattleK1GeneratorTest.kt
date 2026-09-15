package com.quietgrid.cli.starbattle

import com.quietgrid.engine.core.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class StarBattleK1GeneratorTest {
    @Test
    fun `wraps a generated AnimalDoku-medium puzzle as a k=1 star battle easy entry`() {
        val entry = generateStarBattleK1PuzzleForTier(
            size = 5,
            sourceAnimalDokuTier = Difficulty.MEDIUM,
            targetStarBattleTier = "easy",
            idPrefix = "sb5",
        )
        assertNotNull(entry)
        assertEquals(1, entry!!.k)
        assertEquals("easy", entry.difficulty)
        assertEquals(5, entry.solution.size)
        assertEquals(1, entry.solution[0].size)
    }
}
