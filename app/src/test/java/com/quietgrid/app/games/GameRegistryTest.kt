package com.quietgrid.app.games

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameCatalog
import com.quietgrid.app.core.GameId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class GameRegistryTest {
    @Test
    fun `every game id has exactly one definition`() {
        assertEquals(GameId.entries.toList(), GameRegistry.all.map { it.id }.sortedBy { it.ordinal })
    }

    @Test
    fun `every catalog game is registered`() {
        assertEquals(GameCatalog.games.map { it.id }.toSet(), GameRegistry.all.map { it.id }.toSet())
    }

    @Test
    fun `every definition is complete`() {
        GameRegistry.all.forEach { definition ->
            assert(definition.iconGlyph.isNotEmpty()) { "${definition.id} glyph" }
            Difficulty.entries.forEach { difficulty ->
                assertNotEquals("${definition.id} $difficulty label", 0, definition.difficultyLabelRes(difficulty))
            }
        }
    }
}
