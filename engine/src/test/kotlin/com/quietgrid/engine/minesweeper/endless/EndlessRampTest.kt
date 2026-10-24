package com.quietgrid.engine.minesweeper.endless

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EndlessRampTest {

    @Test
    fun `mine counts match the spec at key levels`() {
        assertEquals(9, endlessTargetsFor(1).mines)
        assertEquals(9, endlessTargetsFor(10).mines)
        assertEquals(10, endlessTargetsFor(11).mines)
        assertEquals(11, endlessTargetsFor(21).mines)
        assertEquals(12, endlessTargetsFor(31).mines)
        assertEquals(13, endlessTargetsFor(41).mines)
        assertEquals(13, endlessTargetsFor(200).mines)
        assertEquals(9.0 / 81, endlessTargetsFor(1).density, 1e-9)
        assertEquals(13.0 / 81, endlessTargetsFor(41).density, 1e-9)
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
            previous = current
        }
    }

    @Test
    fun `every lever respects its cap`() {
        for (level in 1..1000) {
            val targets = endlessTargetsFor(level)
            assertTrue(targets.mines <= ENDLESS_MINES_CAP)
            assertEquals(targets.mines / 81.0, targets.density, 1e-9)
            assertTrue(targets.cluePairSteps.last <= ENDLESS_CLUE_PAIR_CAP + ENDLESS_CLUE_PAIR_MARGIN)
            assertTrue(targets.bottleneckSteps.last <= ENDLESS_BOTTLENECK_CAP + ENDLESS_BOTTLENECK_MARGIN)
            assertEquals(level, targets.level)
        }
    }
}
