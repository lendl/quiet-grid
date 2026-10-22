package com.quietgrid.cli.minesweeper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MinesweeperEndlessBenchTest {

    @Test
    fun `a small bench reports consistent numbers`() {
        val report = runMinesweeperEndlessBench(sections = 40, runLength = 20, threads = 1, seed = 1)

        assertEquals(2, report.runs)
        assertTrue(report.sectionsCleared >= 40)
        assertEquals(0, report.stuckRuns)
        assertEquals(0, report.lastPathHits)
        assertEquals(0, report.strandedSections)
        assertTrue(report.settleP50Millis <= report.settleP99Millis)
        assertTrue(report.passRateByLevel.isNotEmpty())
        assertTrue(report.passRateByLevel.values.all { it > 0.0 && it <= 1.0 })
    }

    @Test
    fun `the report prints one line per bucket of ten levels`() {
        val report = EndlessBenchReport(
            runs = 2,
            sectionsCleared = 40,
            stuckRuns = 0,
            mineHitRuns = 0,
            lastPathHits = 1,
            strandedSections = 2,
            passRateByLevel = mapOf(1 to 1.0, 9 to 0.5, 12 to 0.25),
            settleP50Millis = 1.5,
            settleP99Millis = 12.0,
            lastPathByLevel = mapOf(12 to 1),
        )

        val text = formatEndlessBenchReport(report)

        assertTrue(text.contains("last-path hits: 1"))
        assertTrue(text.contains("stranded sections: 2"))
        assertTrue(text.contains("levels 1-10"))
        assertTrue(text.contains("levels 11-20: pass rate 0.250, last path 1"))
        assertTrue(text.contains("p99"))
    }
}
