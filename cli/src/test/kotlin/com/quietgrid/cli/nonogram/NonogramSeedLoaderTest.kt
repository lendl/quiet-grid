// cli/src/test/kotlin/com/quietgrid/cli/nonogram/NonogramSeedLoaderTest.kt
package com.quietgrid.cli.nonogram

import org.junit.Assert.assertTrue
import org.junit.Test

class NonogramSeedLoaderTest {
    @Test
    fun `loadNonogramSeeds reads entries from the real committed asset file`() {
        val seeds = loadNonogramSeeds("app/src/main/assets")
        assertTrue(seeds.isNotEmpty())
    }
}
