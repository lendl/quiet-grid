package com.quietgrid.app.core.daily

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameCatalog
import com.quietgrid.app.core.GameId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyEligibilityTest {

    @Test
    fun `eligible games are allowlisted and not beta, in catalog order`() {
        val expected = GameCatalog.games.filter { it.id in DAILY_GAMES && !it.beta }.map { it.id }
        assertEquals(expected, dailyEligibleGames())
        assertTrue(dailyEligibleGames().none { GameCatalog.get(it).beta })
    }

    @Test
    fun `allowlist is the five launch games`() {
        assertEquals(
            setOf(GameId.ANIMALDOKU, GameId.SUDOKU, GameId.TAKUZU, GameId.WORDSEARCH, GameId.WORDGUESS),
            DAILY_GAMES,
        )
    }

    @Test
    fun `tiers below the bar are hidden`() {
        val sizes = mapOf(
            Difficulty.EASY to 380,
            Difficulty.MEDIUM to 90,
            Difficulty.HARD to 89,
            Difficulty.EXPERT to 3,
        )
        assertEquals(listOf(Difficulty.EASY, Difficulty.MEDIUM), availableDailyTiers(sizes))
    }

    @Test
    fun `missing tiers count as empty`() {
        assertEquals(emptyList<Difficulty>(), availableDailyTiers(emptyMap()))
    }
}
