package com.quietgrid.engine.wordsearch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSearchHiddenWordTest {
    @Test
    fun `buildHiddenWordPool dedupes and drops words shorter than 3 letters`() {
        val pool = buildHiddenWordPool(listOf("cat", "CAT", "ox", "dog"))
        assertEquals(listOf("CAT", "DOG"), pool)
    }

    @Test
    fun `reserveHiddenWordCells returns cells sorted into reading order`() {
        val reserved = reserveHiddenWordCells("CAT", rows = 4, cols = 4)
        assertEquals(3, reserved.positions.size)
        for (i in 0 until reserved.positions.size - 1) {
            val a = reserved.positions[i]
            val b = reserved.positions[i + 1]
            assertTrue(a.row < b.row || (a.row == b.row && a.col < b.col))
        }
    }
}
