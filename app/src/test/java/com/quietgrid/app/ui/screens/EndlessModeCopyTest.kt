package com.quietgrid.app.ui.screens

import com.quietgrid.app.R
import com.quietgrid.app.core.GameId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EndlessModeCopyTest {

    @Test
    fun `block fill has its endless row`() {
        val copy = endlessModeCopyFor(GameId.BLOCKFILL)!!
        assertEquals(R.string.blockfill_endless_label, copy.label)
        assertEquals(R.string.blockfill_multiplier_value, copy.bestLevelFormat)
    }

    @Test
    fun `minesweeper has its endless row`() {
        val copy = endlessModeCopyFor(GameId.MINESWEEPER)!!
        assertEquals(R.string.minesweeper_endless_label, copy.label)
        assertEquals(R.string.minesweeper_endless_stats_best_ring, copy.statsBestLevel)
        assertNull(copy.bestLevelFormat)
    }

    @Test
    fun `other games have no endless row`() {
        for (gameId in GameId.entries - GameId.BLOCKFILL - GameId.MINESWEEPER) {
            assertNull(endlessModeCopyFor(gameId))
        }
    }
}
