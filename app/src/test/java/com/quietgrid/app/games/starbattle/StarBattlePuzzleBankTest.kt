package com.quietgrid.app.games.starbattle

import com.quietgrid.engine.starbattle.StarBattlePuzzleEntry
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class StarBattlePuzzleBankTest {
    @Test
    fun `StarBattlePuzzleEntry list decodes from the bundled asset JSON shape`() {
        val json = """[{"id":"sbk14-1302-2001220122312233","size":4,"difficulty":"easy","k":1,"regions":[[2,0,0,1],[2,2,0,1],[2,2,3,1],[2,2,3,3]],"solution":[[1],[3],[0],[2]]}]"""
        val decoded = Json.decodeFromString<List<StarBattlePuzzleEntry>>(json)
        assertEquals(1, decoded.size)
        assertEquals(4, decoded[0].size)
        assertEquals(1, decoded[0].k)
        assertEquals("easy", decoded[0].difficulty)
    }

    @Test
    fun `StarBattlePuzzleEntry list decodes a K2 entry with multi-column solution rows`() {
        val json = """[{"id":"sbk2-test","size":5,"difficulty":"medium","k":2,"regions":[[0,0,1,1,1],[0,0,1,1,1],[0,2,2,1,1],[3,2,2,4,4],[3,3,2,4,4]],"solution":[[0,2],[1,3],[2,4],[0,3],[1,4]]}]"""
        val decoded = Json.decodeFromString<List<StarBattlePuzzleEntry>>(json)
        assertEquals(1, decoded.size)
        assertEquals(2, decoded[0].k)
        assertEquals(2, decoded[0].solution[0].size)
    }
}
