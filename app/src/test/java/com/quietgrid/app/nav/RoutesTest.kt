package com.quietgrid.app.nav

import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.ActiveSessionSummary
import com.quietgrid.app.data.SESSION_MODE_ENDLESS
import com.quietgrid.app.games.blockfill.BlockFillEndlessResult
import com.quietgrid.app.games.minesweeper.MinesweeperEndlessResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoutesTest {
    @Test
    fun `resume route for a plain session`() {
        assertEquals("play/sudoku/easy/true", Routes.resume(ActiveSessionSummary("sudoku", dailyDate = null)))
    }

    @Test
    fun `resume route keeps the daily date`() {
        assertEquals(
            "play/sudoku/easy/true?daily=2026-10-07",
            Routes.resume(ActiveSessionSummary("sudoku", dailyDate = "2026-10-07")),
        )
    }

    @Test
    fun `resume route for an endless session`() {
        assertEquals("endless/blockfill/true", Routes.resume(ActiveSessionSummary("blockfill", null, SESSION_MODE_ENDLESS)))
    }

    @Test
    fun `resume route for an unknown game is null`() {
        assertNull(Routes.resume(ActiveSessionSummary("flowfree", null)))
    }

    @Test
    fun `endless routes round-trip`() {
        assertEquals("endless/blockfill/true", Routes.endless(GameId.BLOCKFILL, true))
        assertEquals(
            "endlessResult/1200/4/30/90/true/900/stuck",
            Routes.endlessResult(BlockFillEndlessResult(1200, 4, 30, 90, true, 900, "stuck")),
        )
    }

    @Test
    fun `minesweeper endless result route`() {
        assertEquals(
            "minesweeperEndlessResult/12/3/754/true/9/out-of-hearts",
            Routes.minesweeperEndlessResult(MinesweeperEndlessResult(12, 3, 754, true, 9, "out-of-hearts")),
        )
    }

    @Test
    fun `resume route for a minesweeper endless session`() {
        assertEquals("endless/minesweeper/true", Routes.resume(ActiveSessionSummary("minesweeper", null, SESSION_MODE_ENDLESS)))
    }
}
