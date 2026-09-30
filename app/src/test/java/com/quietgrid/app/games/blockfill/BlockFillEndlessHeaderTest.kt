package com.quietgrid.app.games.blockfill

import org.junit.Assert.assertEquals
import org.junit.Test

class BlockFillEndlessHeaderTest {
    @Test
    fun `crown only when no best or best strictly beaten`() {
        assertEquals(true, endlessHeaderShowsCrown(score = 0, bestAtStart = 0))
        assertEquals(true, endlessHeaderShowsCrown(score = 500, bestAtStart = 0))
        assertEquals(false, endlessHeaderShowsCrown(score = 499, bestAtStart = 500))
        assertEquals(false, endlessHeaderShowsCrown(score = 500, bestAtStart = 500))
        assertEquals(true, endlessHeaderShowsCrown(score = 501, bestAtStart = 500))
    }
}
