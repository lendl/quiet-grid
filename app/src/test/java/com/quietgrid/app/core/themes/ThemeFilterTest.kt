package com.quietgrid.app.core.themes

import com.quietgrid.app.core.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeFilterTest {

    private data class Entry(val id: String, val theme: String, val difficulty: Difficulty)

    private val pool = listOf(
        Entry("a1", "animals", Difficulty.EASY),
        Entry("a2", "animals", Difficulty.HARD),
        Entry("f1", "food", Difficulty.EASY),
        Entry("s1", "space", Difficulty.EXPERT),
    )

    private fun tiers(easy: Int, medium: Int, hard: Int, expert: Int) = mapOf(
        Difficulty.EASY to easy,
        Difficulty.MEDIUM to medium,
        Difficulty.HARD to hard,
        Difficulty.EXPERT to expert,
    )

    @Test
    fun `filter removes entries of excluded themes`() {
        val result = filterPoolByThemes(pool, setOf("animals")) { it.theme }

        assertEquals(listOf("f1", "s1"), result.map { it.id })
    }

    @Test
    fun `filter with no exclusions returns pool unchanged`() {
        assertEquals(pool, filterPoolByThemes(pool, emptySet()) { it.theme })
    }

    @Test
    fun `filter falls back to full pool when every entry is excluded`() {
        val result = filterPoolByThemes(pool, setOf("animals", "food", "space")) { it.theme }

        assertEquals(pool, result)
    }

    private val index = mapOf(
        "easy" to mapOf("nl" to mapOf("dieren" to 3), "en" to mapOf("animals" to 2, "food" to 1)),
        "medium" to mapOf("en" to mapOf("animals" to 4)),
        "hard" to mapOf("de" to mapOf("tiere" to 5)),
    )

    @Test
    fun `index counts use locale then english per tier with zero for missing tiers`() {
        val counts = themeCountsForLocale(index, "nl", fallbackToAllLocales = false).associateBy { it.themeId }

        assertEquals(tiers(3, 0, 0, 0), counts.getValue("dieren").perTier)
        assertEquals(tiers(0, 4, 0, 0), counts.getValue("animals").perTier)
        assertEquals(setOf("dieren", "animals"), counts.keys)
    }

    @Test
    fun `index counts merge all locales for a tier without locale or english entries when allowed`() {
        val counts = themeCountsForLocale(index, "nl", fallbackToAllLocales = true).associateBy { it.themeId }

        assertEquals(tiers(0, 0, 5, 0), counts.getValue("tiere").perTier)
        assertEquals(5, counts.getValue("tiere").total)
    }

    @Test
    fun `no sparse tiers when nothing is excluded even if pools are small`() {
        val counts = listOf(ThemeCount("animals", tiers(1, 1, 1, 1)))

        assertTrue(sparseTiers(counts, emptySet()).isEmpty())
    }

    @Test
    fun `tier at threshold is not sparse and tier below threshold is`() {
        val counts = listOf(
            ThemeCount("animals", tiers(THEME_POOL_WARNING_THRESHOLD, THEME_POOL_WARNING_THRESHOLD - 1, 40, 0)),
            ThemeCount("food", tiers(50, 50, 50, 50)),
        )

        val result = sparseTiers(counts, setOf("food"))

        assertEquals(
            listOf(SparseTier(Difficulty.MEDIUM, THEME_POOL_WARNING_THRESHOLD - 1), SparseTier(Difficulty.EXPERT, 0)),
            result,
        )
    }

    @Test
    fun `stale excluded ids not in counts are ignored`() {
        val counts = listOf(ThemeCount("animals", tiers(5, 5, 5, 5)))

        assertTrue(sparseTiers(counts, setOf("removed-theme")).isEmpty())
    }

    @Test
    fun `toggle excludes a selected theme`() {
        val result = toggleThemeExclusion(emptySet(), "food", setOf("animals", "food", "space"))

        assertEquals(setOf("food"), result)
    }

    @Test
    fun `toggle re-includes an excluded theme`() {
        val result = toggleThemeExclusion(setOf("food", "space"), "food", setOf("animals", "food", "space"))

        assertEquals(setOf("space"), result)
    }

    @Test
    fun `toggle refuses to exclude the last selected theme`() {
        val result = toggleThemeExclusion(setOf("food", "space"), "animals", setOf("animals", "food", "space"))

        assertEquals(setOf("food", "space"), result)
    }

    @Test
    fun `toggle drops stale ids`() {
        val result = toggleThemeExclusion(setOf("removed-theme"), "food", setOf("animals", "food"))

        assertEquals(setOf("food"), result)
    }
}
