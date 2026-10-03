package com.quietgrid.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLDecoder

class SupportLinksTest {

    private fun decodedQuery(url: String): Map<String, String> =
        url.substringAfter("?").split("&").associate { part ->
            val (key, value) = part.split("=", limit = 2)
            key to URLDecoder.decode(value, "UTF-8")
        }

    @Test
    fun puzzleReportIncludesPuzzleDetails() {
        val url = buildPuzzleReportUrl(
            gameKey = "sudoku",
            difficultyKey = "hard",
            puzzleId = "sdk-8f3a",
            dailyDate = null,
            result = "solved",
            appVersion = "1.7.0",
            appLanguage = "nl",
        )

        assertTrue(url.startsWith("$ISSUES_URL/new?"))
        val query = decodedQuery(url)
        assertEquals("[Puzzle] sudoku hard sdk-8f3a", query["title"])
        val body = query.getValue("body")
        assertTrue(body.contains("Game: sudoku"))
        assertTrue(body.contains("Difficulty: hard"))
        assertTrue(body.contains("Puzzle: sdk-8f3a"))
        assertTrue(body.contains("Result: solved"))
        assertTrue(body.contains("Version: 1.7.0"))
        assertTrue(body.contains("Language: nl"))
        assertFalse(body.contains("Daily:"))
    }

    @Test
    fun puzzleReportForGeneratedPuzzleSaysGenerated() {
        val url = buildPuzzleReportUrl(
            gameKey = "game2048",
            difficultyKey = "easy",
            puzzleId = null,
            dailyDate = null,
            result = "lost (abandoned)",
            appVersion = "1.7.0",
            appLanguage = "en",
        )

        val query = decodedQuery(url)
        assertEquals("[Puzzle] game2048 easy", query["title"])
        val body = query.getValue("body")
        assertTrue(body.contains("Puzzle: generated"))
        assertTrue(body.contains("Result: lost (abandoned)"))
    }

    @Test
    fun puzzleReportForDailyIncludesDate() {
        val url = buildPuzzleReportUrl(
            gameKey = "takuzu",
            difficultyKey = "medium",
            puzzleId = "tkz-12",
            dailyDate = "2026-10-03",
            result = "solved",
            appVersion = "1.7.0",
            appLanguage = "fr",
        )

        assertTrue(decodedQuery(url).getValue("body").contains("Daily: 2026-10-03"))
    }
}
