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

    @Test
    fun encodeUrlComponentKeepsUnreservedAndPercentEncodesTheRest() {
        assertEquals("a%20b%26c%2F%C3%A9", encodeUrlComponent("a b&c/\u00e9"))
        assertEquals("AZaz09-._~", encodeUrlComponent("AZaz09-._~"))
        assertEquals("%0A", encodeUrlComponent("\n"))
    }

    @Test
    fun issueUrlsMatchTheEarlierEncodingExceptSpacesAsPercent20() {
        val oldPuzzle = "https://github.com/lendl/quiet-grid/issues/new?title=%5BPuzzle%5D+sudoku+hard+a+b%26c%2F%C3%A9&body=%23%23+What+went+wrong%0A%0ADescribe+the+problem+%28for+example%3A+more+than+one+solution%2C+a+correct+move+was+rejected%2C+a+clue+looks+wrong%29.%0A%0A%23%23+Puzzle%0A%0AGame%3A+sudoku%0ADifficulty%3A+hard%0APuzzle%3A+a+b%26c%2F%C3%A9%0ADaily%3A+2026-11-27%0AResult%3A+solved%0A%0A%23%23+App%0A%0AVersion%3A+1.7.0%0ALanguage%3A+nl"
        val oldWords = "https://github.com/lendl/quiet-grid/issues/new?title=%5BTheme+words%5D+caf%C3%A9+noir+%28fr%29&body=%23%23+Theme%0A%0ATheme%3A+caf%C3%A9+noir%0ALanguage%3A+fr%0AGame%3A+wordsearch%0A%0A%23%23+Add%0A%0A-+%C3%A9t%C3%A9%0A-+x+y%0A%0A%23%23+Remove%0A%0A-+a%26b%0A%0A%23%23+Notes%0A%0AAnything+else+about+these+words+%28optional%29.%0A%0A%23%23+App%0A%0AVersion%3A+1.7.0"
        assertEquals(
            oldPuzzle.replace("+", "%20"),
            buildPuzzleReportUrl("sudoku", "hard", "a b&c/\u00e9", "2026-11-27", "solved", "1.7.0", "nl"),
        )
        assertEquals(
            oldWords.replace("+", "%20"),
            buildThemeWordsIssueUrl("wordsearch", "caf\u00e9 noir", "fr", listOf("\u00e9t\u00e9", "x y"), listOf("a&b"), "1.7.0"),
        )
    }
}
