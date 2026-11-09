package com.quietgrid.cli.wordswap

import com.quietgrid.engine.wordswap.gradeWordSwapPuzzle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSwapRegradeTest {

    @Test
    fun regradeMovesPuzzlesToTheirGradedTier() {
        val labelled = GRID3.copy(difficulty = "expert")
        val report = regradeWordSwapBank(listOf(labelled))
        val graded = gradeWordSwapPuzzle(GRID3)!!.key
        assertEquals(graded, report.kept.single().difficulty)
        assertEquals(GRID3.id, report.kept.single().id)
        assertEquals(listOf(WordSwapTierMove(GRID3.id, "expert", graded)), report.moved)
    }

    @Test
    fun regradeDropsBoardsWithSeveralAnswers() {
        val report = regradeWordSwapBank(listOf(LATIN3))
        assertTrue(report.kept.isEmpty())
        assertEquals(listOf(WordSwapUntiered(LATIN3.id, broken = true)), report.untiered)
    }

    @Test
    fun regradeDropsUniqueBoardsOutsideEveryBand() {
        val tooManySwaps = GRID3.copy(id = "ws-test-far", minSwaps = 12)
        val report = regradeWordSwapBank(listOf(tooManySwaps))
        assertTrue(report.kept.isEmpty())
        assertEquals(listOf(WordSwapUntiered(tooManySwaps.id, broken = false)), report.untiered)
    }
}
