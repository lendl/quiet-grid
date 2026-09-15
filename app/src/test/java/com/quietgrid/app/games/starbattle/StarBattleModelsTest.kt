package com.quietgrid.app.games.starbattle

import com.quietgrid.engine.starbattle.StarBattlePuzzleEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private val TEST_PUZZLE = StarBattlePuzzleEntry(
    id = "test",
    size = 4,
    difficulty = "easy",
    k = 1,
    regions = List(4) { row -> List(4) { row } },
    solution = listOf(listOf(1), listOf(3), listOf(0), listOf(2)),
)

class StarBattleModelsTest {
    @Test
    fun `createStarBattleSession starts with an all-empty board, 3 lives, and PLAYING status`() {
        val session = createStarBattleSession(TEST_PUZZLE)
        assertEquals(3, session.lives)
        assertEquals(StarBattleStatus.PLAYING, session.status)
        assertTrue(session.cells.all { row -> row.all { it == StarBattleCellState.EMPTY } })
        assertEquals(4, session.cells.size)
    }
}
