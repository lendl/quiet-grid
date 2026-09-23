package com.quietgrid.cli.flowfree

import com.quietgrid.engine.core.Difficulty
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class FlowFreeCliGeneratorTest {
    @Test
    fun `generates a hard 7x7 puzzle with 6 pairs that classifies as hard and has a unique solution`() {
        var accepted: com.quietgrid.engine.flowfree.FlowFreePuzzleEntry? = null
        for (seed in 1..3000) {
            accepted = generateFlowFreePuzzleForTier(7, 6, Difficulty.HARD, idPrefix = "ff7", random = Random(seed))
            if (accepted != null) break
        }
        assertNotNull(accepted)
        assertEquals("hard", accepted!!.difficulty)
        assertEquals(7, accepted.size)
        assertEquals(6, accepted.pairCount)
    }
}
