package com.quietgrid.app.games.arrowescape

import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.arrowescape.ArrowEscapePuzzleEntry
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class ArrowEscapePuzzleBankTest {
    @Test
    fun `ArrowEscapePuzzleEntry list decodes from the bundled asset JSON shape`() {
        val json = """[{"id":"ae10-0","difficulty":"easy","rows":10,"cols":10,"pieces":[{"cells":[[0,0]],"headDirection":"up"}]}]"""
        val decoded = Json.decodeFromString<List<ArrowEscapePuzzleEntry>>(json)
        assertEquals(1, decoded.size)
        assertEquals(10, decoded[0].rows)
        assertEquals("easy", decoded[0].difficulty)
        assertEquals("up", decoded[0].pieces[0].headDirection)
    }

    @Test
    fun `pool decoding streams the array and keeps only the requested tier`() {
        val json = """[
            {"id":"a","difficulty":"easy","rows":2,"cols":2,"pieces":[{"cells":[[0,0],[0,1]],"headDirection":"right"}],"shape":"heart","mask":["##",".."]},
            {"id":"b","difficulty":"expert","rows":2,"cols":2,"pieces":[{"cells":[[1,0],[1,1]],"headDirection":"right"}]},
            {"id":"c","difficulty":"easy","rows":2,"cols":2,"pieces":[{"cells":[[1,1],[0,1]],"headDirection":"up"}]}
        ]"""
        val pool = decodeArrowEscapePool(json.byteInputStream(), Difficulty.EASY)
        assertEquals(listOf("a", "c"), pool.map { it.id })
        assertEquals(listOf("##", ".."), pool[0].mask)
    }
}
