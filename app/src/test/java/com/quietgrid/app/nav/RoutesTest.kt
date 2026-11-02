package com.quietgrid.app.nav

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.journey.JourneyRequest
import com.quietgrid.app.core.journey.JourneySignals
import com.quietgrid.app.data.ActiveSessionEnvelope
import com.quietgrid.app.data.ActiveSessionSummary
import com.quietgrid.app.data.toSummary
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

    @Test
    fun `play route carries journey args`() {
        assertEquals(
            "play/arrowescape/hard/false?journey=ae-1&journeyPuzzle=ae-2&transform=0",
            Routes.play(GameId.ARROWESCAPE, Difficulty.HARD, resume = false, journey = JourneyRequest("ae-1", "ae-2", 0)),
        )
        assertEquals("journey/animaldoku", Routes.journey(GameId.ANIMALDOKU))
        assertEquals("journeyMap/arrowescape/3", Routes.journeyMap(GameId.ARROWESCAPE, 3))
    }

    @Test
    fun `resume brings a journey session back to its stop`() {
        val envelope = ActiveSessionEnvelope(
            gameId = "arrowescape",
            elapsedSeconds = 12.0,
            payload = "p",
            difficulty = "hard",
            journeyStopId = "ae-1",
            journeyPuzzleId = "ae-2",
            journeyTransform = 1,
        )
        assertEquals(JourneyRequest("ae-1", "ae-2", 1), envelope.toSummary().journey)
        assertEquals("play/arrowescape/hard/true?journey=ae-1&journeyPuzzle=ae-2&transform=1", Routes.resume(envelope.toSummary()))
    }

    @Test
    fun `envelope without journey fields has no journey`() {
        assertNull(ActiveSessionEnvelope("sudoku", 1.0, "p").toSummary().journey)
    }

    @Test
    fun `journey result route`() {
        assertEquals(
            "journeyResult/arrowescape/ae-1/95/1/2",
            Routes.journeyResult(GameId.ARROWESCAPE, "ae-1", 95, JourneySignals(hintsUsed = 1, mistakes = 2)),
        )
    }

    @Test
    fun `loss route carries the journey stop`() {
        assertEquals(
            "loss/arrowescape/hard/30/out_of_lives/0/0?journey=ae-1",
            Routes.loss(GameId.ARROWESCAPE, Difficulty.HARD, 30, "out_of_lives", 0, 0, journeyStop = "ae-1"),
        )
        assertEquals("loss/sudoku/easy/30/abandoned/0/0?daily=2026-10-07", Routes.loss(GameId.SUDOKU, Difficulty.EASY, 30, "abandoned", 0, 0, daily = "2026-10-07"))
    }
}
