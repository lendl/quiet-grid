package com.quietgrid.app.ui.journey

import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.PlayRecord
import com.quietgrid.app.data.gameKeysByJourneyRecency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActiveJourneyTest {
    private val allGames = GameId.entries.toSet()

    private fun record(gameId: GameId, at: Long, journeyStop: String? = "stop") = PlayRecord(
        gameId = gameId.key,
        difficulty = "easy",
        solved = true,
        score = 0,
        elapsedSeconds = 60,
        timestampMillis = at,
        journeyStop = journeyStop,
    )

    private fun summary(chapter: Int?, stop: Int?) = JourneySummary(chapterIndex = chapter, stopNumber = stop, stars = 0, maxStars = 30)

    @Test
    fun `orders journey games by their latest play`() {
        val records = listOf(
            record(GameId.SUDOKU, 10),
            record(GameId.ARROWESCAPE, 30),
            record(GameId.TAKUZU, 20),
            record(GameId.SUDOKU, 40),
        )
        assertEquals(listOf("sudoku", "arrowescape", "takuzu"), gameKeysByJourneyRecency(records))
    }

    @Test
    fun `ignores plays outside the journey`() {
        val records = listOf(record(GameId.SUDOKU, 10), record(GameId.ARROWESCAPE, 30, journeyStop = null))
        assertEquals(listOf("sudoku"), gameKeysByJourneyRecency(records))
    }

    @Test
    fun `candidates skip games that are not enabled or have no journey`() {
        val keys = listOf("arrowescape", "minesweeper", "sudoku", "unknown")
        assertEquals(listOf(GameId.SUDOKU), journeyCandidates(keys, allGames - GameId.ARROWESCAPE))
    }

    @Test
    fun `falls back to the next game when the latest journey is complete`() {
        val summaries = listOf(
            GameId.ARROWESCAPE to summary(chapter = null, stop = null),
            GameId.SUDOKU to summary(chapter = 3, stop = 5),
        )
        assertEquals(ActiveJourney(GameId.SUDOKU, 3, 5), firstActiveJourney(summaries))
    }

    @Test
    fun `skips games whose journey file is missing`() {
        val summaries = listOf(GameId.WORDSWAP to null, GameId.TAKUZU to summary(chapter = 1, stop = 2))
        assertEquals(ActiveJourney(GameId.TAKUZU, 1, 2), firstActiveJourney(summaries))
    }

    @Test
    fun `nothing to continue when every journey is complete`() {
        assertNull(firstActiveJourney(listOf(GameId.SUDOKU to summary(chapter = null, stop = null))))
        assertNull(firstActiveJourney(emptyList()))
    }
}
