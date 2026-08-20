package com.quietgrid.cli.wordsearch

import com.quietgrid.engine.core.Difficulty
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSearchGeneratorTest {
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
}
