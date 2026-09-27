package com.quietgrid.cli.wordsearch

import com.quietgrid.cli.themes.loadSharedThemes
import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.wordsearch.WordPlacement
import com.quietgrid.engine.wordsearch.WordSearchDirection
import com.quietgrid.engine.wordsearch.WordSearchPuzzleEntry
import com.quietgrid.engine.wordsearch.hasCoverageViolation
import com.quietgrid.engine.wordsearch.hasDuplicateOccurrence
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class WordSearchBankVerificationTest {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        private val bank: List<WordSearchPuzzleEntry> by lazy {
            Difficulty.entries.flatMap { difficulty ->
                json.decodeFromString(
                    ListSerializer(WordSearchPuzzleEntry.serializer()),
                    File("app/src/main/assets/wordsearch_puzzles_${difficulty.key}.json").readText(),
                )
            }
        }
    }

    private val wordsByTheme: Map<String, Map<String, Set<String>>> = loadSharedThemes().mapValues { (_, themes) ->
        themes.associate { theme -> theme.themeId to theme.words.map { it.uppercase() }.toSet() }
    }

    @Test
    fun `ids are unique`() {
        assertEquals(bank.size, bank.map { it.id }.toSet().size)
    }

    @Test
    fun `every theme has at least 30 puzzles per difficulty and locale`() {
        val counts = bank.groupingBy { Triple(it.difficulty, it.locale, it.themeId) }.eachCount()
        val buckets = Difficulty.entries.flatMap { difficulty ->
            wordsByTheme.flatMap { (locale, themes) -> themes.keys.map { themeId -> Triple(difficulty.key, locale, themeId) } }
        }
        val short = buckets.filter { (counts[it] ?: 0) < 30 }.map { "$it=${counts[it] ?: 0}" }
        assertTrue("${short.size} of ${buckets.size} buckets have fewer than 30 puzzles: ${short.take(20)}", short.isEmpty())
    }

    @Test
    fun `every puzzle uses a shared theme id for its locale`() {
        bank.forEach { entry ->
            assertTrue("${entry.id}: ${entry.locale}/${entry.themeId}", wordsByTheme[entry.locale]?.containsKey(entry.themeId) == true)
            assertEquals(entry.id, entry.themeId, entry.hiddenWord.clue)
        }
    }

    @Test
    fun `every puzzle follows the placement rules`() {
        val broken = bank.filter { entry ->
            val placements = entry.words.map { WordPlacement(it.id, it.word, it.positions.first(), WordSearchDirection.RIGHT, it.positions) }
            val allWords = entry.words.map { it.word to it.positions } + listOf(entry.hiddenWord.word to entry.hiddenWord.positions)
            val hasGap = entry.grid.any { row -> row.any { it.length != 1 } }
            hasGap || hasCoverageViolation(placements) || hasDuplicateOccurrence(entry.grid, allWords)
        }
        assertTrue("${broken.size} puzzles break placement rules: ${broken.take(20).map { it.id }}", broken.isEmpty())
    }

    @Test
    fun `every word and hidden word belongs to the puzzle's theme`() {
        bank.forEach { entry ->
            val words = wordsByTheme.getValue(entry.locale).getValue(entry.themeId)
            assertTrue("${entry.id}: hidden ${entry.hiddenWord.word}", entry.hiddenWord.word in words)
            entry.words.forEach { assertTrue("${entry.id}: ${it.word}", it.word in words) }
        }
    }
}
