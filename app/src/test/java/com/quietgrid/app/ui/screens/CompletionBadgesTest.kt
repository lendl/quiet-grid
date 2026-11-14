package com.quietgrid.app.ui.screens

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.PlayRecord
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
}
