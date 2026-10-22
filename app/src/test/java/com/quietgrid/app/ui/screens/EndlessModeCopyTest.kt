package com.quietgrid.app.ui.screens

import com.quietgrid.app.R
import com.quietgrid.app.core.GameId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EndlessModeCopyTest {

    @Test
    fun `block fill always has its endless row`() {
        for (beta in listOf(false, true)) {
            val copy = endlessModeCopyFor(GameId.BLOCKFILL, beta)!!
            assertEquals(R.string.blockfill_endless_label, copy.label)
            assertEquals(R.string.blockfill_multiplier_value, copy.bestLevelFormat)
        }
    }

    @Test
    fun `minesweeper endless shows only with beta games on`() {
        assertNull(endlessModeCopyFor(GameId.MINESWEEPER, betaGamesEnabled = false))
        val copy = endlessModeCopyFor(GameId.MINESWEEPER, betaGamesEnabled = true)!!
        assertEquals(R.string.minesweeper_endless_label, copy.label)
        assertEquals(R.string.minesweeper_endless_stats_best_ring, copy.statsBestLevel)
        assertNull(copy.bestLevelFormat)
    }

    @Test
    fun `other games have no endless row`() {
        for (gameId in GameId.entries - GameId.BLOCKFILL - GameId.MINESWEEPER) {
            assertNull(endlessModeCopyFor(gameId, betaGamesEnabled = false))
            assertNull(endlessModeCopyFor(gameId, betaGamesEnabled = true))
        }
    }
}
