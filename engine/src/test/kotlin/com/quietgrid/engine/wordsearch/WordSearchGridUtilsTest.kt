package com.quietgrid.engine.wordsearch

import org.junit.Assert.assertEquals
import org.junit.Test

class WordSearchGridUtilsTest {
    @Test
    fun `directionToDelta has all 8 directions with correct row col deltas`() {
        assertEquals(0 to 1, directionToDelta.getValue(WordSearchDirection.RIGHT))
        assertEquals(-1 to -1, directionToDelta.getValue(WordSearchDirection.UP_LEFT))
        assertEquals(8, directionToDelta.size)
    }

    @Test
    fun `toGridKey encodes row and col uniquely for grids under 1000 cols`() {
        assertEquals(3005, toGridKey(WSCellRef(3, 5)))
    }
}
