package com.quietgrid.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameCatalogTest {

    @Test
    fun `enabledGameIds hides beta games unless enabled`() {
        val beta = GameCatalog.games.filter { it.beta }.map { it.id }.toSet()
        assertTrue(GameCatalog.enabledGameIds(false).none { it in beta })
        assertEquals(GameCatalog.games.map { it.id }.toSet(), GameCatalog.enabledGameIds(true))
    }

    @Test
    fun everyGameHasAtLeastOneCategory() {
        GameCatalog.games.forEach { meta ->
            assertTrue(
                "${meta.id} has no categories assigned",
                meta.categories.isNotEmpty(),
            )
        }
    }

    @Test
    fun placementPuzzlesAreLogicAndSpatial() {
        val animaldoku = GameCatalog.get(GameId.ANIMALDOKU)
        val starbattle = GameCatalog.get(GameId.STARBATTLE)
        assertTrue(animaldoku.categories.containsAll(setOf(GameCategory.LOGIC, GameCategory.SPATIAL)))
        assertTrue(starbattle.categories.containsAll(setOf(GameCategory.LOGIC, GameCategory.SPATIAL)))
    }

    @Test
    fun whoDidItIsBetaLogicGame() {
        val meta = GameCatalog.get(GameId.WHODIDIT)
        assertTrue(meta.beta)
        assertEquals(setOf(GameCategory.LOGIC), meta.categories)
    }

    @Test
    fun betaWhoDidItIsNotDailyEligible() {
        assertFalse(com.quietgrid.app.core.daily.dailyEligibleGames().contains(GameId.WHODIDIT))
    }
}
