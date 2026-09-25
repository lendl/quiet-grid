package com.quietgrid.app.core.daily

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.ActiveSessionEnvelope
import com.quietgrid.app.data.PlayRecord
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class DailyReminderLogicTest {

    private val zone = ZoneId.of("Europe/Amsterdam")
    private val today = LocalDate.of(2026, 9, 24)
    private val fullPools = Difficulty.entries.associateWith { 200 }

    private fun at(date: LocalDate, hour: Int, minute: Int) = ZonedDateTime.of(LocalDateTime.of(date, LocalTime.of(hour, minute)), zone)

    private fun record(gameId: GameId, difficulty: Difficulty, solved: Boolean) = PlayRecord(
        gameId = gameId.key,
        difficulty = difficulty.key,
        solved = solved,
        score = 0,
        elapsedSeconds = 60,
        timestampMillis = 1L,
        dailyDate = today.toString(),
    )

    @Test
    fun `delay targets later today when time still ahead`() {
        assertEquals(Duration.ofHours(2), nextReminderDelay(at(today, 7, 0), LocalTime.of(9, 0)))
    }

    @Test
    fun `delay targets tomorrow when time already passed`() {
        assertEquals(Duration.ofHours(23), nextReminderDelay(at(today, 10, 0), LocalTime.of(9, 0)))
    }

    @Test
    fun `delay targets tomorrow when exactly at time`() {
        assertEquals(Duration.ofHours(24), nextReminderDelay(at(today, 9, 0), LocalTime.of(9, 0)))
    }

    @Test
    fun `delay across spring forward is one hour shorter`() {
        val beforeDst = LocalDate.of(2026, 3, 28)
        assertEquals(Duration.ofHours(23), nextReminderDelay(at(beforeDst, 9, 0), LocalTime.of(9, 0)))
    }

    @Test
    fun `delay across fall back is one hour longer`() {
        val beforeDst = LocalDate.of(2026, 10, 24)
        assertEquals(Duration.ofHours(25), nextReminderDelay(at(beforeDst, 9, 0), LocalTime.of(9, 0)))
    }

    @Test
    fun `no subscriptions needs no reminder`() {
        val result = gamesNeedingReminder(emptyMap(), listOf(GameId.SUDOKU), mapOf(GameId.SUDOKU to fullPools), emptyList(), null, today)
        assertEquals(emptyList<GameId>(), result)
    }

    @Test
    fun `subscribed but ineligible game is ignored`() {
        val result = gamesNeedingReminder(allTiers(GameId.SUDOKU), emptyList(), mapOf(GameId.SUDOKU to fullPools), emptyList(), null, today)
        assertEquals(emptyList<GameId>(), result)
    }

    @Test
    fun `unplayed subscribed game needs reminder`() {
        val result = gamesNeedingReminder(allTiers(GameId.SUDOKU), listOf(GameId.SUDOKU), mapOf(GameId.SUDOKU to fullPools), emptyList(), null, today)
        assertEquals(listOf(GameId.SUDOKU), result)
    }

    @Test
    fun `all tiers solved or lost needs no reminder`() {
        val records = Difficulty.entries.mapIndexed { i, d -> record(GameId.SUDOKU, d, solved = i % 2 == 0) }
        val result = gamesNeedingReminder(allTiers(GameId.SUDOKU), listOf(GameId.SUDOKU), mapOf(GameId.SUDOKU to fullPools), records, null, today)
        assertEquals(emptyList<GameId>(), result)
    }

    @Test
    fun `in progress tier still needs reminder`() {
        val records = Difficulty.entries.drop(1).map { record(GameId.SUDOKU, it, solved = true) }
        val envelope = ActiveSessionEnvelope(GameId.SUDOKU.key, 10.0, "{}", today.toString(), Difficulty.entries.first().key)
        val result = gamesNeedingReminder(allTiers(GameId.SUDOKU), listOf(GameId.SUDOKU), mapOf(GameId.SUDOKU to fullPools), records, envelope, today)
        assertEquals(listOf(GameId.SUDOKU), result)
    }

    @Test
    fun `tier hidden by small language pool is ignored`() {
        val pools = fullPools + (Difficulty.entries.last() to 5)
        val records = Difficulty.entries.dropLast(1).map { record(GameId.SUDOKU, it, solved = true) }
        val result = gamesNeedingReminder(allTiers(GameId.SUDOKU), listOf(GameId.SUDOKU), mapOf(GameId.SUDOKU to pools), records, null, today)
        assertEquals(emptyList<GameId>(), result)
    }

    private fun allTiers(vararg games: GameId) = games.associateWith { Difficulty.entries.toSet() }
}
