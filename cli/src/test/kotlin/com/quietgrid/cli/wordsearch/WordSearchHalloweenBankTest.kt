package com.quietgrid.cli.wordsearch

import com.quietgrid.cli.themes.loadSeasonalThemes
import com.quietgrid.cli.themes.loadSharedThemes
import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.wordsearch.WordPlacement
import com.quietgrid.engine.wordsearch.WordSearchBankEntry
import com.quietgrid.engine.wordsearch.WordSearchDirection
import com.quietgrid.engine.wordsearch.hasCoverageViolation
import com.quietgrid.engine.wordsearch.hasDuplicateOccurrence
import com.quietgrid.engine.wordsearch.toPuzzleEntry
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class WordSearchHalloweenBankTest {
    private val themes = loadSeasonalThemes("halloween")
    private val bank = Difficulty.entries.flatMap { difficulty ->
        Json { ignoreUnknownKeys = true }.decodeFromString(
            ListSerializer(WordSearchBankEntry.serializer()),
            File("app/src/main/assets/wordsearch_halloween_puzzles_${difficulty.key}.json").readText(),
        ).map { it.toPuzzleEntry(difficulty.key) }
    }

    @Test
    fun `the halloween theme covers every shared locale with title case words`() {
        assertEquals(loadSharedThemes().keys, themes.keys)
        val pattern = Regex("[A-Z][a-z]{2,13}")
        themes.forEach { (locale, list) ->
            val theme = list.single()
            assertEquals("halloween", theme.themeId)
            assertTrue("$locale has only ${theme.words.size}", theme.words.size >= 60)
            assertEquals("$locale has duplicates", theme.words.size, theme.words.map { it.uppercase() }.toSet().size)
            theme.words.forEach { assertTrue("$locale: '$it'", pattern.matches(it)) }
        }
    }

    @Test
    fun `every locale has a halloween puzzle for every tier`() {
        Difficulty.entries.forEach { difficulty ->
            themes.keys.forEach { locale ->
                assertTrue("$locale/${difficulty.key}", bank.any { it.difficulty == difficulty.key && it.locale == locale })
            }
        }
        assertEquals(bank.size, bank.map { it.id }.toSet().size)
    }

    @Test
    fun `every halloween puzzle uses only halloween words and follows the placement rules`() {
        bank.forEach { entry ->
            assertEquals(entry.id, "halloween", entry.themeId)
            val words = themes.getValue(entry.locale).single().words.map { it.uppercase() }.toSet()
            assertTrue("${entry.id}: hidden ${entry.hiddenWord.word}", entry.hiddenWord.word in words)
            entry.words.forEach { assertTrue("${entry.id}: ${it.word}", it.word in words) }
            val placements = entry.words.map { WordPlacement(it.id, it.word, it.positions.first(), WordSearchDirection.RIGHT, it.positions) }
            val allWords = entry.words.map { it.word to it.positions } + listOf(entry.hiddenWord.word to entry.hiddenWord.positions)
            assertTrue(entry.id, entry.grid.all { row -> row.all { it.length == 1 } })
            assertTrue(entry.id, !hasCoverageViolation(placements) && !hasDuplicateOccurrence(entry.grid, allWords))
        }
    }
}
