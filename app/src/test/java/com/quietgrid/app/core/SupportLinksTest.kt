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

    @Test
    fun themeWordsIssueListsAdditionsAndRemovals() {
        val url = buildThemeWordsIssueUrl(
            gameKey = "themeclear",
            themeId = "animals",
            locale = "nl",
            added = listOf("Otter", "Das"),
            removed = listOf("Mol"),
            appVersion = "1.7.0",
        )

        assertTrue(url.startsWith("$ISSUES_URL/new?"))
        val query = decodedQuery(url)
        assertEquals("[Theme words] animals (nl)", query["title"])
        val body = query.getValue("body")
        assertTrue(body.contains("Theme: animals"))
        assertTrue(body.contains("Language: nl"))
        assertTrue(body.contains("Game: themeclear"))
        assertTrue(body.contains("## Add\n\n- Otter\n- Das"))
        assertTrue(body.contains("## Remove\n\n- Mol"))
        assertTrue(body.contains("## Notes"))
        assertTrue(body.contains("Version: 1.7.0"))
    }

    @Test
    fun themeWordsIssueOmitsEmptySections() {
        val url = buildThemeWordsIssueUrl(
            gameKey = "wordsearch",
            themeId = "food",
            locale = "en",
            added = emptyList(),
            removed = listOf("Gin"),
            appVersion = "1.7.0",
        )

        val body = decodedQuery(url).getValue("body")
        assertFalse(body.contains("## Add"))
        assertTrue(body.contains("## Remove\n\n- Gin"))
    }
}
