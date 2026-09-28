package com.quietgrid.cli.themeclear

import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.themeclear.ThemeClearBankEntry
import com.quietgrid.engine.themeclear.ThemeClearDictionary
import com.quietgrid.engine.themeclear.ThemeClearPuzzleEntry
import com.quietgrid.engine.themeclear.ThemeClearSolver
import com.quietgrid.engine.themeclear.themeClearMeetsTier
import com.quietgrid.engine.themeclear.toPuzzleEntry
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ThemeClearBankVerificationTest {
    private val bankJson = Json { ignoreUnknownKeys = true }
    private val bank: List<ThemeClearPuzzleEntry> = bankJson
        .decodeFromString(ListSerializer(ThemeClearBankEntry.serializer()), File("app/src/main/assets/themeclear_puzzles.json").readText())
        .map { it.toPuzzleEntry() }
    private val themesByLocale: Map<String, Map<String, Set<String>>> =
        com.quietgrid.cli.themes.loadSharedThemes()
            .mapValues { (_, themes) ->
                themes.associate { theme -> theme.themeId to theme.words.map { it.uppercase() }.toSet() }
            }

    @Test
    fun `every tier has at least 50 puzzles per locale`() {
        bank.groupBy { it.locale }.forEach { (_, entries) ->
            Difficulty.entries.forEach { difficulty ->
                assertTrue("${difficulty.key}", entries.count { it.difficulty == difficulty.key } >= 50)
            }
        }
    }

    @Test
    fun `every theme has at least 30 puzzles per difficulty and locale`() {
        val counts = bank.groupingBy { Triple(it.difficulty, it.locale, it.themeId) }.eachCount()
        val buckets = Difficulty.entries.flatMap { difficulty ->
            themesByLocale.flatMap { (locale, themes) -> themes.keys.map { themeId -> Triple(difficulty.key, locale, themeId) } }
        }
        val short = buckets.filter { (counts[it] ?: 0) < 30 }.map { "$it=${counts[it] ?: 0}" }
        assertTrue("${short.size} of ${buckets.size} buckets have fewer than 30 puzzles: ${short.take(20)}", short.isEmpty())
    }

    @Test
    fun `no theme exceeds the puzzle cap per difficulty and locale`() {
        val over = bank.groupingBy { Triple(it.difficulty, it.locale, it.themeId) }.eachCount()
            .filterValues { it > THEMECLEAR_MAX_PUZZLES_PER_THEME }
        assertTrue("${over.size} groups exceed the cap: ${over.entries.take(20)}", over.isEmpty())
    }

    @Test
    fun `ids are unique`() {
        assertEquals(bank.size, bank.map { it.id }.toSet().size)
    }

    @Test
    fun `grid shape and letters match the intended words exactly`() {
        bank.forEach { entry ->
            assertEquals(entry.id, entry.rows, entry.grid.size)
            assertTrue(entry.id, entry.grid.all { it.length == entry.cols })
            assertTrue(entry.id, entry.cols <= 7)
            assertEquals(entry.id, entry.words.joinToString("").toList().sorted(), entry.grid.joinToString("").toList().sorted())
        }
    }

    @Test
    fun `intended words all belong to the puzzle's theme`() {
        bank.forEach { entry ->
            val words = themesByLocale.getValue(entry.locale).getValue(entry.themeId)
            entry.words.forEach { assertTrue("${entry.id}: $it", it in words) }
        }
    }

    @Test
    fun `every puzzle meets its tier on a fresh solver run`() {
        val solvers = HashMap<Pair<String, String>, ThemeClearSolver>()
        val failing = bank.filterNot { entry ->
            val solver = solvers.getOrPut(entry.locale to entry.themeId) {
                ThemeClearSolver(ThemeClearDictionary(themesByLocale.getValue(entry.locale).getValue(entry.themeId)))
            }
            val difficulty = Difficulty.entries.first { it.key == entry.difficulty }
            themeClearMeetsTier(difficulty, entry.grid.joinToString(""), solver)
        }
        assertTrue("${failing.size} puzzles miss their tier: ${failing.take(20).map { it.id }}", failing.isEmpty())
    }
}
