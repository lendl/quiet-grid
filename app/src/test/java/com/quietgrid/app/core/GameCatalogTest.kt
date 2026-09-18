package com.quietgrid.app.core

import org.junit.Assert.assertTrue
import org.junit.Test

class GameCatalogTest {

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
}
