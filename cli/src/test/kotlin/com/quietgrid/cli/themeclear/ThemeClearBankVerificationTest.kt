package com.quietgrid.cli.themeclear

import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.themeclear.ThemeClearDictionary
import com.quietgrid.engine.themeclear.ThemeClearPuzzleEntry
import com.quietgrid.engine.themeclear.ThemeClearSolver
import com.quietgrid.engine.themeclear.themeClearMeetsTier
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ThemeClearBankVerificationTest {
    private val bankJson = Json { ignoreUnknownKeys = true }
    private val bank: List<ThemeClearPuzzleEntry> = bankJson
        .decodeFromString(ListSerializer(ThemeClearPuzzleEntry.serializer()), File("app/src/main/assets/themeclear_puzzles.json").readText())
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
    fun `stored metrics match a fresh solver run and meet the tier`() {
        bank.forEach { entry ->
            val themes = themesByLocale.getValue(entry.locale)
            val solver = ThemeClearSolver(ThemeClearDictionary(themes.getValue(entry.themeId)))
            val letters = entry.grid.joinToString("")
            assertEquals(entry.id, entry.metrics, solver.analyze(letters))
            val difficulty = Difficulty.entries.first { it.key == entry.difficulty }
            assertTrue(entry.id, themeClearMeetsTier(difficulty, letters.length, entry.metrics))
        }
    }
}
