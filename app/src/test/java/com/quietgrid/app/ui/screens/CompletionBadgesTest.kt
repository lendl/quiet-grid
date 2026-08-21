package com.quietgrid.app.ui.screens

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.PlayRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

private fun record(
    gameId: GameId = GameId.SUDOKU,
    difficulty: Difficulty = Difficulty.EASY,
    solved: Boolean = true,
    timestampMillis: Long = 0L,
) = PlayRecord(
    gameId = gameId.key,
    difficulty = difficulty.key,
    solved = solved,
    score = 0,
    elapsedSeconds = 0,
    timestampMillis = timestampMillis,
)

class CompletionBadgesTest {

    @Test
    fun `milestoneReached matches every listed threshold`() {
        MILESTONE_THRESHOLDS.forEach { threshold ->
            assertEquals(threshold, milestoneReached(threshold))
        }
    }

    @Test
    fun `milestoneReached returns null for an off-threshold count`() {
        assertNull(milestoneReached(11))
        assertNull(milestoneReached(49))
        assertNull(milestoneReached(0))
    }

    @Test
    fun `bouncedBackFromSkid fires when the two most recent prior records are both losses`() {
        val records = listOf(
            record(solved = false, timestampMillis = 1L),
            record(solved = false, timestampMillis = 2L),
            record(solved = true, timestampMillis = 3L),
        )

        assertTrue(bouncedBackFromSkid(records, Difficulty.EASY, excludingTimestampMillis = 3L))
    }

    @Test
    fun `bouncedBackFromSkid does not fire with only one prior loss`() {
        val records = listOf(
            record(solved = true, timestampMillis = 1L),
            record(solved = false, timestampMillis = 2L),
            record(solved = true, timestampMillis = 3L),
        )

        assertFalse(bouncedBackFromSkid(records, Difficulty.EASY, excludingTimestampMillis = 3L))
    }

    @Test
    fun `bouncedBackFromSkid does not fire when a win breaks up two losses`() {
        val records = listOf(
            record(solved = false, timestampMillis = 1L),
            record(solved = true, timestampMillis = 2L),
            record(solved = false, timestampMillis = 3L),
            record(solved = true, timestampMillis = 4L),
        )

        assertFalse(bouncedBackFromSkid(records, Difficulty.EASY, excludingTimestampMillis = 4L))
    }

    @Test
    fun `bouncedBackFromSkid ignores records from a different difficulty`() {
        val records = listOf(
            record(difficulty = Difficulty.HARD, solved = false, timestampMillis = 1L),
            record(difficulty = Difficulty.HARD, solved = false, timestampMillis = 2L),
            record(difficulty = Difficulty.EASY, solved = true, timestampMillis = 3L),
        )

        assertFalse(bouncedBackFromSkid(records, Difficulty.EASY, excludingTimestampMillis = 3L))
    }

    @Test
    fun `bouncedBackFromSkid returns false on an empty history`() {
        assertFalse(bouncedBackFromSkid(emptyList(), Difficulty.EASY, excludingTimestampMillis = 1L))
    }

    @Test
    fun `distinctGamesToday counts only today's records and ignores duplicates`() {
        val today = LocalDate.of(2026, 8, 21)
        val todayMillis = today.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() + 1000L
        val yesterdayMillis = today.minusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() + 1000L
        val records = listOf(
            record(gameId = GameId.SUDOKU, timestampMillis = todayMillis),
            record(gameId = GameId.SUDOKU, timestampMillis = todayMillis),
            record(gameId = GameId.TAKUZU, timestampMillis = todayMillis),
            record(gameId = GameId.WORDGUESS, timestampMillis = yesterdayMillis),
        )

        assertEquals(2, distinctGamesToday(records, today))
    }

    @Test
    fun `recordsToday counts all of today's records regardless of outcome`() {
        val today = LocalDate.of(2026, 8, 21)
        val todayMillis = today.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() + 1000L
        val yesterdayMillis = today.minusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() + 1000L
        val records = listOf(
            record(solved = true, timestampMillis = todayMillis),
            record(solved = false, timestampMillis = todayMillis),
            record(solved = true, timestampMillis = yesterdayMillis),
        )

        assertEquals(2, recordsToday(records, today))
    }
}
