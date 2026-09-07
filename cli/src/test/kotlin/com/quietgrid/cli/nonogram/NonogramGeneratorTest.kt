// cli/src/test/kotlin/com/quietgrid/cli/nonogram/NonogramGeneratorTest.kt
package com.quietgrid.cli.nonogram

import com.quietgrid.cli.GenerationState
import com.quietgrid.engine.core.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class NonogramGeneratorTest {
    @Test
    fun `generateRandomNonogramPuzzle produces a solution of the requested dimensions`() {
        val stateFile = Files.createTempDirectory("nonogram-test").resolve("state.json").toString()
        val state = GenerationState(stateFile)
        // Medium at 10x5 (5x5 is no longer used by any difficulty - too small to require real
        // logic, confirmed by direct inspection), with a generous attempt budget: the
        // post-redesign gates (shallow chain depth, few distinct techniques, low freebie-fill,
        // minimum multi-segment-line count) make every target region fairly narrow for pure
        // random generation, so the production default of attempts is too tight for a single
        // deterministic call. The real generator's multi-attempt outer retry loop absorbs this
        // fine in production; this smoke test just needs enough budget in one call to be reliable.
        val entry = generateRandomNonogramPuzzle(10, 5, Difficulty.MEDIUM, idPrefix = "n10x5", state = state, maxAttempts = 20000)
        assertNotNull(entry)
        assertEquals(10, entry!!.rows)
        assertEquals(5, entry.cols)
        assertEquals("medium", entry.difficulty)
    }

    @Test
    fun `nonogramSizesForDifficulty returns non-empty sizes for every difficulty`() {
        Difficulty.entries.forEach { difficulty ->
            assertTrue(nonogramSizesForDifficulty(difficulty).isNotEmpty())
        }
    }
}
