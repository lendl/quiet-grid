package com.quietgrid.app.core.daily

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.ActiveSessionEnvelope
import com.quietgrid.app.data.PlayRecord
import org.junit.Assert.assertEquals
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Test
import kotlin.time.Duration.Companion.hours

class DailyReminderLogicTest {

    private val zone = TimeZone.of("Europe/Amsterdam")
    private val today = LocalDate(2026, 9, 24)
    private val fullPools = Difficulty.entries.associateWith { 200 }

    private fun at(date: LocalDate, hour: Int, minute: Int) = LocalDateTime(date, LocalTime(hour, minute)).toInstant(zone)

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
        assertEquals(2.hours, nextReminderDelay(at(today, 7, 0), zone, LocalTime(9, 0)))
    }

    @Test
    fun `delay targets tomorrow when time already passed`() {
        assertEquals(23.hours, nextReminderDelay(at(today, 10, 0), zone, LocalTime(9, 0)))
    }

    @Test
    fun `delay targets tomorrow when exactly at time`() {
        assertEquals(24.hours, nextReminderDelay(at(today, 9, 0), zone, LocalTime(9, 0)))
    }

    @Test
    fun `delay across spring forward is one hour shorter`() {
        val beforeDst = LocalDate(2026, 3, 28)
        assertEquals(23.hours, nextReminderDelay(at(beforeDst, 9, 0), zone, LocalTime(9, 0)))
    }

    @Test
    fun `delay across fall back is one hour longer`() {
        val beforeDst = LocalDate(2026, 10, 24)
        assertEquals(25.hours, nextReminderDelay(at(beforeDst, 9, 0), zone, LocalTime(9, 0)))
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
