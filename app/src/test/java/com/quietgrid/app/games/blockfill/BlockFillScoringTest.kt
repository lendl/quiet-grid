package com.quietgrid.app.games.blockfill

import org.junit.Assert.assertEquals
import org.junit.Test

class BlockFillScoringTest {
    @Test
    fun `no lines cleared scores nothing`() {
        assertEquals(0, scorePlacement(linesCleared = 0, comboStreakBeforeThisMove = 5, boardEmptiedAfter = false))
    }

    @Test
    fun `single line clear scores base points`() {
        assertEquals(80, scorePlacement(linesCleared = 1, comboStreakBeforeThisMove = 0, boardEmptiedAfter = false))
    }

    @Test
    fun `multi line clear adds bonus per extra line`() {
        assertEquals(80 * 2 + 40, scorePlacement(linesCleared = 2, comboStreakBeforeThisMove = 0, boardEmptiedAfter = false))
    }

    @Test
    fun `combo streak adds bonus per streak point`() {
        assertEquals(80 + 20 * 3, scorePlacement(linesCleared = 1, comboStreakBeforeThisMove = 3, boardEmptiedAfter = false))
    }

    @Test
    fun `full board clear adds the full-clear bonus`() {
        assertEquals(80 + 300, scorePlacement(linesCleared = 1, comboStreakBeforeThisMove = 0, boardEmptiedAfter = true))
    }

    @Test
    fun `multiplier scales every point of the placement`() {
        val base = 80 * 2 + 40 + 20 + 300
        assertEquals(base * 3, scorePlacement(linesCleared = 2, comboStreakBeforeThisMove = 1, boardEmptiedAfter = true, multiplier = 3))
    }

    @Test
    fun `puzzle score drops with every move needed`() {
        assertEquals(50_000 - 30 * 500, blockFillPuzzleScore(movesUsed = 30, scoreTarget = 1000))
        assertEquals(true, blockFillPuzzleScore(movesUsed = 25, scoreTarget = 1000) > blockFillPuzzleScore(movesUsed = 26, scoreTarget = 1000))
    }

    @Test
    fun `puzzle score starts higher for a higher target`() {
        assertEquals(125_000 - 75 * 500, blockFillPuzzleScore(movesUsed = 75, scoreTarget = 2500))
        assertEquals(75_000, blockFillPuzzleScore(movesUsed = 0, scoreTarget = 1500))
    }

    @Test
    fun `puzzle score never drops below the floor`() {
        assertEquals(100, blockFillPuzzleScore(movesUsed = 10_000, scoreTarget = 2500))
    }

    @Test
    fun `multiplier does not turn a bare placement into points`() {
        assertEquals(0, scorePlacement(linesCleared = 0, comboStreakBeforeThisMove = 0, boardEmptiedAfter = false, multiplier = 4))
    }
}
