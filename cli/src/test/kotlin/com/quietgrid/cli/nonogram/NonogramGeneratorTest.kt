// cli/src/test/kotlin/com/quietgrid/cli/nonogram/NonogramGeneratorTest.kt
package com.quietgrid.cli.nonogram

import com.quietgrid.engine.core.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class NonogramGeneratorTest {
    private val seed = listOf(
        listOf(true, true, true, true, true),
        listOf(false, false, false, false, false),
        listOf(true, true, true, true, true),
        listOf(false, false, false, false, false),
        listOf(true, true, true, true, true),
    )

    @Test
    fun `generateNonogramVariant produces a solution of the same dimensions`() {
        val entry = generateNonogramVariant(seed, Difficulty.EASY, variantSeed = 42L, idPrefix = "n5")
        assertNotNull(entry)
        assertEquals(5, entry!!.rows)
        assertEquals(5, entry.cols)
    }
}
