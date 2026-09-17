package com.quietgrid.app.ui.screens

import com.quietgrid.app.R
import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.PlayRecord
import org.junit.Assert.assertEquals
import org.junit.Test

private fun record(gameId: GameId, isChallenger: Boolean, timestampMillis: Long) = PlayRecord(
    gameId = gameId.key,
    difficulty = "easy",
    puzzleId = null,
    solved = true,
    score = 10,
    elapsedSeconds = 30,
    timestampMillis = timestampMillis,
    isChallenger = isChallenger,
    puzzlesSolved = if (isChallenger) 3 else null,
)

class LogsFilterTest {

    @Test
    fun `ALL mode with no game filter returns everything newest first`() {
        val oldest = record(GameId.SUDOKU, isChallenger = false, timestampMillis = 1L)
        val newest = record(GameId.TAKUZU, isChallenger = true, timestampMillis = 2L)

        val result = filterLogRecords(listOf(oldest, newest), LogsMode.ALL, gameId = null)

        assertEquals(listOf(newest, oldest), result)
    }

    @Test
    fun `SOLO mode excludes Challenger records`() {
        val solo = record(GameId.SUDOKU, isChallenger = false, timestampMillis = 1L)
        val challenger = record(GameId.SUDOKU, isChallenger = true, timestampMillis = 2L)

        val result = filterLogRecords(listOf(solo, challenger), LogsMode.SOLO, gameId = null)

        assertEquals(listOf(solo), result)
    }

    @Test
    fun `CHALLENGER mode excludes solo records`() {
        val solo = record(GameId.SUDOKU, isChallenger = false, timestampMillis = 1L)
        val challenger = record(GameId.SUDOKU, isChallenger = true, timestampMillis = 2L)

        val result = filterLogRecords(listOf(solo, challenger), LogsMode.CHALLENGER, gameId = null)

        assertEquals(listOf(challenger), result)
    }

    @Test
    fun `game filter narrows to one game regardless of mode`() {
        val sudoku = record(GameId.SUDOKU, isChallenger = false, timestampMillis = 1L)
        val takuzu = record(GameId.TAKUZU, isChallenger = false, timestampMillis = 2L)

        val result = filterLogRecords(listOf(sudoku, takuzu), LogsMode.ALL, gameId = GameId.SUDOKU)

        assertEquals(listOf(sudoku), result)
    }

    @Test
    fun `logsReasonLabelRes maps known reasons and falls back for unknown ones`() {
        assertEquals(R.string.logs_reason_time_up, logsReasonLabelRes("time_up"))
        assertEquals(R.string.logs_reason_lives_exhausted, logsReasonLabelRes("lives_exhausted"))
        assertEquals(R.string.logs_reason_lives_exhausted, logsReasonLabelRes("out_of_lives"))
        assertEquals(R.string.logs_reason_lives_exhausted, logsReasonLabelRes("hearts_exhausted"))
        assertEquals(R.string.logs_reason_bank_exhausted, logsReasonLabelRes("bank_exhausted"))
        assertEquals(R.string.logs_reason_abandoned, logsReasonLabelRes("abandoned"))
        assertEquals(R.string.logs_reason_rule_failure, logsReasonLabelRes("rule-failure"))
        assertEquals(R.string.logs_reason_rule_failure, logsReasonLabelRes("no-moves-left"))
        assertEquals(R.string.logs_reason_generic, logsReasonLabelRes("some-unmapped-reason"))
    }
}
