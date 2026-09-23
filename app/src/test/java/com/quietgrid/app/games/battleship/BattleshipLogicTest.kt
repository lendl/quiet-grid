package com.quietgrid.app.games.battleship

import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.battleship.BattleshipCell
import com.quietgrid.engine.battleship.BattleshipPuzzleEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private fun testPuzzle(givens: String = "....") = BattleshipPuzzleEntry(
    id = "t1",
    size = 2,
    difficulty = "easy",
    rowClues = listOf(1, 0),
    colClues = listOf(1, 0),
    fleet = listOf(1),
    solution = "1000",
    givens = givens,
)

class BattleshipLogicTest {
    @Test
    fun `applyBattleshipPressCell cycles unknown to water to ship to unknown`() {
        var session = createBattleshipSession(testPuzzle())
        session = applyBattleshipPressCell(session, 0, 0)
        assertEquals(BattleshipCell.WATER, session.board[0][0])
        session = applyBattleshipPressCell(session, 0, 0)
        assertEquals(BattleshipCell.SHIP, session.board[0][0])
        session = applyBattleshipPressCell(session, 0, 0)
        assertEquals(BattleshipCell.UNKNOWN, session.board[0][0])
    }

    @Test
    fun `applyBattleshipPressCell is a no-op on a given cell`() {
        var session = createBattleshipSession(testPuzzle(givens = "1..."))
        assertEquals(BattleshipCell.SHIP, session.board[0][0])
        session = applyBattleshipPressCell(session, 0, 0)
        assertEquals(BattleshipCell.SHIP, session.board[0][0])
    }

    @Test
    fun `battleshipIsSolved is true only when the board exactly matches the solution`() {
        var session = createBattleshipSession(testPuzzle())
        assertTrue(!battleshipIsSolved(session))
        session = applyBattleshipPressCell(session, 0, 0)
        session = applyBattleshipPressCell(session, 0, 0)
        session = applyBattleshipPressCell(session, 0, 1)
        session = applyBattleshipPressCell(session, 1, 0)
        session = applyBattleshipPressCell(session, 1, 1)
        assertTrue(battleshipIsSolved(session))
    }

    @Test
    fun `board encode-decode round-trips`() {
        var session = createBattleshipSession(testPuzzle())
        session = applyBattleshipPressCell(session, 0, 0)
        val encoded = encodeBattleshipBoard(session.board)
        val decoded = decodeBattleshipBoard(encoded, session.puzzle.size)
        assertEquals(session.board, decoded)
    }

    @Test
    fun `battleshipScore decreases as elapsed time increases`() {
        val fast = battleshipScore(Difficulty.EASY, 0)
        val slow = battleshipScore(Difficulty.EASY, 600)
        assertTrue(fast > slow)
    }
}
