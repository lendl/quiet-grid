package com.quietgrid.app.nav

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.journey.JourneyRequest
import com.quietgrid.app.data.ActiveSessionEnvelope
import com.quietgrid.app.data.ActiveSessionSummary
import com.quietgrid.app.data.toSummary
import com.quietgrid.app.data.SESSION_MODE_ENDLESS
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoutesTest {
    @Test
    fun `resume route for a plain session`() {
        assertEquals(PlayRoute(GameId.SUDOKU, Difficulty.EASY, resume = true), resumeRoute(ActiveSessionSummary("sudoku", dailyDate = null)))
    }

    @Test
    fun `resume route keeps the daily date`() {
        assertEquals(
            PlayRoute(GameId.SUDOKU, Difficulty.EASY, resume = true, daily = "2026-10-07"),
            resumeRoute(ActiveSessionSummary("sudoku", dailyDate = "2026-10-07")),
        )
    }

    @Test
    fun `resume route for an endless session`() {
        assertEquals(EndlessRoute(GameId.BLOCKFILL, resume = true), resumeRoute(ActiveSessionSummary("blockfill", null, SESSION_MODE_ENDLESS)))
    }

    @Test
    fun `resume route for an unknown game is null`() {
        assertNull(resumeRoute(ActiveSessionSummary("flowfree", null)))
    }



    @Test
    fun `resume route for a minesweeper endless session`() {
        assertEquals(EndlessRoute(GameId.MINESWEEPER, resume = true), resumeRoute(ActiveSessionSummary("minesweeper", null, SESSION_MODE_ENDLESS)))
    }

    @Test
    fun `play route carries journey args`() {
        val route = PlayRoute.of(GameId.ARROWESCAPE, Difficulty.HARD, resume = false, journey = JourneyRequest("ae-1", "ae-2", 2))
        assertEquals(PlayRoute(GameId.ARROWESCAPE, Difficulty.HARD, resume = false, journeyStop = "ae-1", journeyPuzzle = "ae-2", transform = 2), route)
        assertEquals(JourneyRequest("ae-1", "ae-2", 2), route.journey)
    }

    @Test
    fun `play route parses its daily date`() {
        assertEquals(LocalDate(2026, 10, 7), PlayRoute(GameId.SUDOKU, Difficulty.EASY, resume = false, daily = "2026-10-07").dailyDate)
        assertNull(PlayRoute(GameId.SUDOKU, Difficulty.EASY, resume = false).journey)
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
        assertEquals(
            PlayRoute(GameId.ARROWESCAPE, Difficulty.HARD, resume = true, journeyStop = "ae-1", journeyPuzzle = "ae-2", transform = 1),
            resumeRoute(envelope.toSummary()),
        )
    }

    @Test
    fun `envelope without journey fields has no journey`() {
        assertNull(ActiveSessionEnvelope("sudoku", 1.0, "p").toSummary().journey)
    }


}
