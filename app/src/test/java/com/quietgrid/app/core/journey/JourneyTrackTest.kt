package com.quietgrid.app.core.journey

import com.quietgrid.app.core.GameId
import org.junit.Assert.assertEquals
import org.junit.Test

class JourneyTrackTest {

    @Test
    fun `english and other games keep their scope`() {
        assertEquals("wordswap", journeyTrack(GameId.WORDSWAP, "en").scope)
        assertEquals("journey_wordswap.json", journeyTrack(GameId.WORDSWAP, "en").assetName)
        assertEquals("sudoku", journeyTrack(GameId.SUDOKU, "nl").scope)
        assertEquals("journey_sudoku.json", journeyTrack(GameId.SUDOKU, "nl").assetName)
    }

    @Test
    fun `wordswap gets a track per language`() {
        assertEquals("wordswap_nl", journeyTrack(GameId.WORDSWAP, "nl").scope)
        assertEquals("journey_wordswap_nl.json", journeyTrack(GameId.WORDSWAP, "nl").assetName)
        assertEquals("wordswap", journeyTrack(GameId.WORDSWAP, "sv").scope)
    }
}
