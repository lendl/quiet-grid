package com.quietgrid.app.core.daily

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.ActiveSessionEnvelope
import com.quietgrid.app.data.PlayRecord
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

private val today = LocalDate.of(2026, 9, 24)

private fun daily(gameId: GameId, date: LocalDate, solved: Boolean, difficulty: Difficulty = Difficulty.EASY, elapsed: Int = 60) = PlayRecord(
    gameId = gameId.key,
    difficulty = difficulty.key,
    solved = solved,
    score = 0,
    elapsedSeconds = elapsed,
    timestampMillis = date.toEpochDay(),
    dailyDate = date.toString(),
)

private val fullPools = Difficulty.entries.associateWith { 200 }

class DailyStatusTest {

    @Test
    fun `no record and no envelope is unplayed`() {
        assertEquals(DailyTierStatus.Unplayed, dailyTierStatus(emptyList(), null, GameId.SUDOKU, Difficulty.EASY, today))
    }

    @Test
    fun `solved record gives solved with time`() {
        val records = listOf(daily(GameId.SUDOKU, today, solved = true, elapsed = 402))
        assertEquals(DailyTierStatus.Solved(402), dailyTierStatus(records, null, GameId.SUDOKU, Difficulty.EASY, today))
    }

    @Test
    fun `lost record gives lost even if envelope matches`() {
        val records = listOf(daily(GameId.SUDOKU, today, solved = false))
        val envelope = ActiveSessionEnvelope("sudoku", 5.0, "p", today.toString(), "easy")
        assertEquals(DailyTierStatus.Lost, dailyTierStatus(records, envelope, GameId.SUDOKU, Difficulty.EASY, today))
    }

    @Test
    fun `matching envelope without record is in progress`() {
        val envelope = ActiveSessionEnvelope("sudoku", 5.0, "p", today.toString(), "easy")
        assertEquals(DailyTierStatus.InProgress, dailyTierStatus(emptyList(), envelope, GameId.SUDOKU, Difficulty.EASY, today))
    }

    @Test
    fun `non-daily record for same game does not lock the tier`() {
        val records = listOf(daily(GameId.SUDOKU, today, solved = true).copy(dailyDate = null))
        assertEquals(DailyTierStatus.Unplayed, dailyTierStatus(records, null, GameId.SUDOKU, Difficulty.EASY, today))
    }

    @Test
    fun `streak counts consecutive solved days ending today`() {
        val records = listOf(
            daily(GameId.SUDOKU, today, solved = true),
            daily(GameId.SUDOKU, today.minusDays(1), solved = true),
            daily(GameId.SUDOKU, today.minusDays(3), solved = true),
        )
        assertEquals(2, dailyStreak(records, setOf(GameId.SUDOKU), today))
    }

    @Test
    fun `streak stays alive through yesterday when today unplayed`() {
        val records = listOf(
            daily(GameId.SUDOKU, today.minusDays(1), solved = true),
            daily(GameId.SUDOKU, today.minusDays(2), solved = true),
        )
        assertEquals(2, dailyStreak(records, setOf(GameId.SUDOKU), today))
    }

    @Test
    fun `loss-only day breaks the streak`() {
        val records = listOf(
            daily(GameId.SUDOKU, today, solved = true),
            daily(GameId.SUDOKU, today.minusDays(1), solved = false),
            daily(GameId.SUDOKU, today.minusDays(2), solved = true),
        )
        assertEquals(1, dailyStreak(records, setOf(GameId.SUDOKU), today))
    }

    @Test
    fun `any tier solve keeps a day and losses on other tiers do not matter`() {
        val records = listOf(
            daily(GameId.SUDOKU, today, solved = false, difficulty = Difficulty.EXPERT),
            daily(GameId.SUDOKU, today, solved = true, difficulty = Difficulty.EASY),
        )
        assertEquals(1, dailyStreak(records, setOf(GameId.SUDOKU), today))
    }

    @Test
    fun `overall streak spans games, per-game streak filters`() {
        val records = listOf(
            daily(GameId.SUDOKU, today, solved = true),
            daily(GameId.TAKUZU, today.minusDays(1), solved = true),
        )
        assertEquals(2, dailyStreak(records, setOf(GameId.SUDOKU, GameId.TAKUZU), today))
        assertEquals(1, dailyStreak(records, setOf(GameId.SUDOKU), today))
    }

    @Test
    fun `buildDailyGames keeps catalog order, drops unsubscribed and games without tiers`() {
        val games = buildDailyGames(
            subscribed = setOf(GameId.WORDSEARCH, GameId.SUDOKU, GameId.TAKUZU),
            eligible = listOf(GameId.TAKUZU, GameId.SUDOKU, GameId.WORDSEARCH, GameId.WORDGUESS),
            poolSizes = mapOf(
                GameId.TAKUZU to fullPools,
                GameId.SUDOKU to fullPools,
                GameId.WORDSEARCH to mapOf(Difficulty.EASY to 40),
            ),
            records = emptyList(),
            envelope = null,
            today = today,
        )
        assertEquals(listOf(GameId.TAKUZU, GameId.SUDOKU), games.map { it.gameId })
        assertEquals(Difficulty.entries, games.first().tiers.map { it.difficulty })
    }
}
