package com.quietgrid.app.games.flowfree

import com.quietgrid.engine.flowfree.FlowFreePuzzleEntry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FlowFreePuzzleBankTest {
    private val puzzle = FlowFreePuzzleEntry(
        id = "test",
        size = 2,
        difficulty = "easy",
        pairCount = 1,
        endpoints = listOf(listOf(0, -1), listOf(-1, 0)),
        paths = listOf(listOf(0, 1, 3, 2)),
    )

    @Test
    fun `a fresh session has no meaningful progress`() {
        assertFalse(flowFreeHasMeaningfulProgress(createFlowFreeSession(puzzle)))
    }

    @Test
    fun `a session with a path longer than one cell has meaningful progress`() {
        val session = createFlowFreeSession(puzzle).copy(paths = mapOf(0 to listOf(0 to 0, 0 to 1)))
        assertTrue(flowFreeHasMeaningfulProgress(session))
    }
}
