package com.quietgrid.engine.minesweeper.endless

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EndlessRampTest {

    @Test
    fun `mine counts match the spec at key levels`() {
        assertEquals(5, endlessTargetsFor(1).mines)
        assertEquals(9, endlessTargetsFor(21).mines)
        assertEquals(13, endlessTargetsFor(41).mines)
        assertEquals(13, endlessTargetsFor(200).mines)
        assertEquals(0.16, endlessTargetsFor(41).density, 1e-9)
        assertEquals(0.16, endlessTargetsFor(200).density, 1e-9)
    }

    @Test
    fun `opening limit starts at level twenty and stops at twenty-five`() {
        assertNull(endlessTargetsFor(19).largestCascadeLimit)
        assertEquals(40, endlessTargetsFor(20).largestCascadeLimit)
        assertEquals(30, endlessTargetsFor(30).largestCascadeLimit)
        assertEquals(25, endlessTargetsFor(35).largestCascadeLimit)
        assertEquals(25, endlessTargetsFor(50).largestCascadeLimit)
        assertEquals(25, endlessTargetsFor(90).largestCascadeLimit)
    }

    @Test
    fun `clue pair and bottleneck limits rise late in small steps with no minimum`() {
        assertEquals(0..2, endlessTargetsFor(29).cluePairSteps)
        assertEquals(0..3, endlessTargetsFor(30).cluePairSteps)
        assertEquals(0..4, endlessTargetsFor(34).cluePairSteps)
        assertEquals(0..3, endlessTargetsFor(39).bottleneckSteps)
        assertEquals(0..4, endlessTargetsFor(40).bottleneckSteps)
        assertEquals(0..5, endlessTargetsFor(45).bottleneckSteps)
    }

    @Test
    fun `no target ever goes down as the level rises`() {
        var previous = endlessTargetsFor(1)
        for (level in 2..300) {
            val current = endlessTargetsFor(level)
            assertTrue(current.mines >= previous.mines)
            assertTrue(current.cluePairSteps.last >= previous.cluePairSteps.last)
            assertTrue(current.bottleneckSteps.last >= previous.bottleneckSteps.last)
            assertEquals(0, current.cluePairSteps.first)
            assertEquals(0, current.bottleneckSteps.first)
            val previousLimit = previous.largestCascadeLimit
            val currentLimit = current.largestCascadeLimit
            if (previousLimit != null) assertTrue(currentLimit != null && currentLimit <= previousLimit)
            previous = current
        }
    }

    @Test
    fun `every lever respects its cap`() {
        for (level in 1..1000) {
            val targets = endlessTargetsFor(level)
            assertTrue(targets.density <= ENDLESS_DENSITY_CAP + 1e-9)
            assertTrue(targets.cluePairSteps.last <= ENDLESS_CLUE_PAIR_CAP + ENDLESS_CLUE_PAIR_MARGIN)
            assertTrue(targets.bottleneckSteps.last <= ENDLESS_BOTTLENECK_CAP + ENDLESS_BOTTLENECK_MARGIN)
            targets.largestCascadeLimit?.let { assertTrue(it >= ENDLESS_CASCADE_MIN_LIMIT) }
            assertEquals(level, targets.level)
        }
    }
}
