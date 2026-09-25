package com.quietgrid.app.ui.screens

import com.quietgrid.app.R
import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.PlayRecord
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

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
    fun `groupLogsByDay splits by local day newest first`() {
        val utc = ZoneId.of("UTC")
        fun at(day: Int, hour: Int) = LocalDateTime.of(2026, 9, day, hour, 0).atZone(utc).toInstant().toEpochMilli()
        val morning24 = record(GameId.SUDOKU, isChallenger = false, timestampMillis = at(24, 8))
        val evening24 = record(GameId.TAKUZU, isChallenger = false, timestampMillis = at(24, 22))
        val noon25 = record(GameId.SUDOKU, isChallenger = false, timestampMillis = at(25, 12))

        val days = groupLogsByDay(listOf(morning24, noon25, evening24), utc)

        assertEquals(listOf(LocalDate.of(2026, 9, 25), LocalDate.of(2026, 9, 24)), days.map { it.date })
        assertEquals(listOf(evening24, morning24), days[1].records)
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
