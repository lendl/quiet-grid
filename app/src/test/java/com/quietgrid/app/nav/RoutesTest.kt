package com.quietgrid.app.nav

import com.quietgrid.app.core.GameId
import com.quietgrid.app.games.blockfill.BlockFillEndlessResult
import org.junit.Assert.assertEquals
import org.junit.Test

class RoutesTest {
    @Test
    fun `endless routes round-trip`() {
        assertEquals("endless/blockfill/true", Routes.endless(GameId.BLOCKFILL, true))
        assertEquals(
            "endlessResult/1200/4/30/90/true/900/stuck",
            Routes.endlessResult(BlockFillEndlessResult(1200, 4, 30, 90, true, 900, "stuck")),
        )
    }
}
