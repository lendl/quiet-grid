package com.quietgrid.app.ui.screens

import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.DifficultyStats
import com.quietgrid.app.data.GameStats
import com.quietgrid.app.data.PlayRecord
import org.junit.Assert.assertEquals
import org.junit.Test

private fun stats(played: Int): GameStats = GameStats(byDifficulty = mapOf("easy" to DifficultyStats(played = played)))

private fun record(gameId: GameId) = PlayRecord(
    gameId = gameId.key,
    difficulty = "easy",
    solved = true,
    score = 10,
    elapsedSeconds = 30,
    timestampMillis = 1L,
)

class StatsFilterTest {

    @Test
    fun `empty inputs give empty list`() {
        assertEquals(emptyList<GameId>(), statsFilterGames(emptyMap(), emptyList()))
    }

    @Test
    fun `games with zero played are excluded`() {
        val result = statsFilterGames(mapOf(GameId.SUDOKU to stats(0)), emptyList())
        assertEquals(emptyList<GameId>(), result)
    }

    @Test
    fun `stats games sorted by played descending`() {
        val result = statsFilterGames(
            mapOf(GameId.SUDOKU to stats(2), GameId.TAKUZU to stats(5)),
            emptyList(),
        )
        assertEquals(listOf(GameId.TAKUZU, GameId.SUDOKU), result)
    }

    @Test
    fun `history-only games appended in catalog order`() {
        val result = statsFilterGames(
            mapOf(GameId.SUDOKU to stats(1)),
            listOf(record(GameId.WORDSEARCH), record(GameId.TAKUZU)),
        )
        assertEquals(listOf(GameId.SUDOKU, GameId.TAKUZU, GameId.WORDSEARCH), result)
    }

    @Test
    fun `game present in both sources appears once`() {
        val result = statsFilterGames(
            mapOf(GameId.SUDOKU to stats(3)),
            listOf(record(GameId.SUDOKU), record(GameId.SUDOKU)),
        )
        assertEquals(listOf(GameId.SUDOKU), result)
    }

    @Test
    fun `unknown game keys in history are ignored`() {
        val unknown = record(GameId.SUDOKU).copy(gameId = "removed-game")
        assertEquals(emptyList<GameId>(), statsFilterGames(emptyMap(), listOf(unknown)))
    }
}
