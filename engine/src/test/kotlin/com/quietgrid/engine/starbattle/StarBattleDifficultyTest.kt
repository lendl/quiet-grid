package com.quietgrid.engine.starbattle

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue

private fun chainResult(chains: Int, depth: Int, stepsBeforeFirstPlacement: Int): StarBattleSolveResult {
    val steps = mutableListOf<StarBattleSolveStep>()
    repeat(stepsBeforeFirstPlacement) { steps.add(StarBattleSolveStep(StarBattleTechnique.CHAIN, depth)) }
    steps.add(StarBattleSolveStep(StarBattleTechnique.FORCED_PLACEMENT, 0))
    repeat(chains - stepsBeforeFirstPlacement) { steps.add(StarBattleSolveStep(StarBattleTechnique.CHAIN, depth)) }
    return StarBattleSolveResult(solved = true, steps = steps)
}

class StarBattleDifficultyTest {
    @Test
    fun `unsolved result classifies as null`() {
        val result = StarBattleSolveResult(solved = false, steps = emptyList())
        assertNull(classifyStarBattleK2Grade(result))
    }

    @Test
    fun `a solve with no chain steps at all classifies as null (not medium-eligible by forced placement alone)`() {
        val result = StarBattleSolveResult(
            solved = true,
            steps = listOf(StarBattleSolveStep(StarBattleTechnique.FORCED_PLACEMENT, 0)),
        )
        assertNull(classifyStarBattleK2Grade(result))
    }

    @Test
    fun `a single shallow chain step clears medium grade`() {
        val result = StarBattleSolveResult(
            solved = true,
            steps = listOf(StarBattleSolveStep(StarBattleTechnique.CHAIN, 1)),
        )
        assertEquals("medium", classifyStarBattleK2Grade(result))
    }

    @Test
    fun `a single deep chain is no longer enough for expert`() {
        val result = StarBattleSolveResult(
            solved = true,
            steps = listOf(StarBattleSolveStep(StarBattleTechnique.FORCED_PLACEMENT, 0), StarBattleSolveStep(StarBattleTechnique.CHAIN, 9)),
        )
        assertEquals("medium", classifyStarBattleK2Grade(result))
    }

    @Test
    fun `enough chains and a delayed first placement clear hard`() {
        assertEquals("hard", classifyStarBattleK2Grade(chainResult(chains = 6, depth = 3, stepsBeforeFirstPlacement = 6)))
        assertEquals("medium", classifyStarBattleK2Grade(chainResult(chains = 6, depth = 3, stepsBeforeFirstPlacement = 5)))
        assertEquals("medium", classifyStarBattleK2Grade(chainResult(chains = 5, depth = 3, stepsBeforeFirstPlacement = 5)))
    }

    @Test
    fun `expert needs many deep chains and a long opening`() {
        assertEquals("expert", classifyStarBattleK2Grade(chainResult(chains = 12, depth = 8, stepsBeforeFirstPlacement = 12)))
        assertEquals("hard", classifyStarBattleK2Grade(chainResult(chains = 12, depth = 7, stepsBeforeFirstPlacement = 12)))
        assertEquals("hard", classifyStarBattleK2Grade(chainResult(chains = 12, depth = 8, stepsBeforeFirstPlacement = 11)))
    }

    @Test
    fun `analyzeStarBattleSolveResult counts the steps before the first placement`() {
        assertEquals(7, analyzeStarBattleSolveResult(chainResult(chains = 9, depth = 2, stepsBeforeFirstPlacement = 7)).stepsBeforeFirstPlacement)
    }

    @Test
    fun `classifyStarBattleK2Puzzle enforces each grades sizes and minimum region size`() {
        val expert = chainResult(chains = 12, depth = 8, stepsBeforeFirstPlacement = 12)
        val stripes9 = List(9) { row -> List(9) { row } }
        val stripes8 = List(8) { row -> List(8) { row } }
        assertEquals("expert", classifyStarBattleK2Puzzle(9, stripes9, expert))
        assertNull(classifyStarBattleK2Puzzle(8, stripes8, expert))
        val tinyRegion = stripes9.mapIndexed { r, line -> if (r == 0) List(9) { c -> if (c < 3) 0 else 1 } else line }
        assertFalse(meetsStarBattleK2RegionShape("expert", 9, tinyRegion))
        assertTrue(meetsStarBattleK2RegionShape("medium", 9, tinyRegion))
    }
}
