package com.quietgrid.cli.wordsearch

import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.wordsearch.WSCellRef
import com.quietgrid.engine.wordsearch.WSHiddenWord
import com.quietgrid.engine.wordsearch.WordSearchPuzzleEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSearchBatchTest {
    private fun entry(id: String, locale: String, themeId: String) = WordSearchPuzzleEntry(
        id = id,
        difficulty = "hard",
        rows = 1,
        cols = 3,
        themeId = themeId,
        grid = listOf(listOf("C", "A", "T")),
        words = emptyList(),
        hiddenWord = WSHiddenWord("CAT", themeId, listOf(WSCellRef(0, 0), WSCellRef(0, 1), WSCellRef(0, 2))),
        locale = locale,
    )

    @Test
    fun `wordSearchThemeDeficits counts only the requested locale and skips themes already at target`() {
        val bank = listOf(
            entry("1", "en", "animals"),
            entry("2", "en", "animals"),
            entry("3", "nl", "animals"),
            entry("4", "en", "food"),
        )
        val deficits = wordSearchThemeDeficits(bank, locale = "en", themeIds = listOf("animals", "food", "space"), target = 2)
        assertEquals(mapOf("food" to 1, "space" to 2), deficits)
    }

    @Test
    fun `generateWordSearchForQuotas fills every theme quota exactly`() {
        val entries = generateWordSearchForQuotas(Difficulty.EASY, "en", mapOf("animals" to 2, "food" to 1), threads = 4, maxAttempts = 300)
        assertEquals(mapOf("animals" to 2, "food" to 1), entries.groupingBy { it.themeId }.eachCount())
        assertTrue(entries.all { it.locale == "en" && it.difficulty == Difficulty.EASY.key })
    }

    @Test
    fun `generateWordSearchForQuotas skips entries rejected by the caller`() {
        val rejected = mutableSetOf<String>()
        val entries = generateWordSearchForQuotas(Difficulty.EASY, "en", mapOf("animals" to 2), threads = 2, maxAttempts = 300) { candidate ->
            if (rejected.isEmpty()) {
                rejected += candidate.id
                false
            } else {
                true
            }
        }
        assertEquals(2, entries.size)
        assertTrue(entries.none { it.id in rejected })
    }
}
