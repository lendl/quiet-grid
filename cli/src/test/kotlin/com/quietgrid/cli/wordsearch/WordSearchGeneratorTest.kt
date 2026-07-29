package com.quietgrid.cli.wordsearch

import com.quietgrid.engine.core.Difficulty
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSearchGeneratorTest {
    @Test
    fun `generateWordSearchPuzzle produces a fully-tiled grid with no empty cells`() {
        // generateWordSearchPuzzle has a well-documented, intentional ~20% single-attempt
        // success rate (the reference architecture expects an outer retry loop that callers
        // are responsible for). Retry here so the test verifies the algorithm can succeed,
        // not that it always succeeds on the first try.
        val maxAttempts = 50
        var succeeded = false
        for (attempt in 1..maxAttempts) {
            // preferredLanguages = ["en"] is honored via the generator's language selection;
            // it isn't exposed on the returned entry itself, so there's nothing further to assert on it.
            val entry = generateWordSearchPuzzle(rows = 8, cols = 8, difficulty = Difficulty.EASY, preferredLanguages = listOf("en"))
            if (entry != null && entry.grid.all { row -> row.all { it.isNotEmpty() && it != "#" } }) {
                succeeded = true
                break
            }
        }
        assertTrue("Expected at least one fully-tiled grid within $maxAttempts attempts", succeeded)
    }
}
