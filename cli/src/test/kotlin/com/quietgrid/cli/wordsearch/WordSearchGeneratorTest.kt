package com.quietgrid.cli.wordsearch

import com.quietgrid.engine.core.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSearchGeneratorTest {
    @Test
    fun `generateWordSearchPuzzle builds the requested theme`() {
        val entry = (1..20).firstNotNullOfOrNull {
            generateWordSearchPuzzle(rows = 8, cols = 8, difficulty = Difficulty.EASY, preferredLanguages = listOf("en"), themeId = "space")
        }
        requireNotNull(entry)
        assertEquals("space", entry.themeId)
    }

    @Test
    fun `most hard attempts produce a puzzle`() {
        val produced = (1..20).count {
            generateWordSearchPuzzle(rows = 13, cols = 13, difficulty = Difficulty.HARD, preferredLanguages = listOf("en")) != null
        }
        assertTrue("only $produced of 20 hard attempts produced a puzzle", produced >= 5)
    }

    @Test
    fun `generateWordSearchPuzzle produces a fully-tiled grid with no empty cells`() {
        val maxAttempts = 50
        var succeeded = false
        for (attempt in 1..maxAttempts) {
            val entry = generateWordSearchPuzzle(rows = 8, cols = 8, difficulty = Difficulty.EASY, preferredLanguages = listOf("en"))
            if (entry != null && entry.grid.all { row -> row.all { it.isNotEmpty() && it != "#" } }) {
                succeeded = true
                break
            }
        }
        assertTrue("Expected at least one fully-tiled grid within $maxAttempts attempts", succeeded)
    }

    @Test
    fun `generateWordSearchPuzzle ids are prefixed with the game key`() {
        val maxAttempts = 50
        var succeeded = false
        for (attempt in 1..maxAttempts) {
            val entry = generateWordSearchPuzzle(rows = 8, cols = 8, difficulty = Difficulty.EASY, preferredLanguages = listOf("en"))
            if (entry != null && entry.id.startsWith("ws-")) {
                succeeded = true
                break
            }
        }
        assertTrue("Expected a puzzle with id prefixed 'ws-' within $maxAttempts attempts", succeeded)
    }

    @Test
    fun `generated puzzles use a shared theme id and only that theme's words`() {
        val themes = com.quietgrid.cli.themes.loadSharedThemes().getValue("de")
            .associate { theme -> theme.themeId to theme.words.map { it.uppercase() }.toSet() }
        val entry = (1..50).firstNotNullOfOrNull {
            generateWordSearchPuzzle(rows = 8, cols = 8, difficulty = Difficulty.EASY, preferredLanguages = listOf("de"))
        }
        requireNotNull(entry)
        val words = themes.getValue(entry.themeId)
        assertTrue(entry.hiddenWord.word in words)
        assertTrue(entry.words.all { it.word in words })
        org.junit.Assert.assertEquals(entry.themeId, entry.hiddenWord.clue)
    }
}
