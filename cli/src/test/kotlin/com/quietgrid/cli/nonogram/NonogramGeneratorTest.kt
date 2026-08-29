// cli/src/test/kotlin/com/quietgrid/cli/nonogram/NonogramGeneratorTest.kt
package com.quietgrid.cli.nonogram

import com.quietgrid.engine.core.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NonogramGeneratorTest {
    @Test
    fun `generateRandomNonogramPuzzle produces a solution of the requested dimensions`() {
        val entry = generateRandomNonogramPuzzle(5, 5, Difficulty.EASY, idPrefix = "n5x5")
        assertNotNull(entry)
        assertEquals(5, entry!!.rows)
        assertEquals(5, entry.cols)
        assertEquals("easy", entry.difficulty)
    }

    @Test
    fun `nonogramSizesForDifficulty returns non-empty sizes for every difficulty`() {
        Difficulty.entries.forEach { difficulty ->
            assertTrue(nonogramSizesForDifficulty(difficulty).isNotEmpty())
        }
    }
}
