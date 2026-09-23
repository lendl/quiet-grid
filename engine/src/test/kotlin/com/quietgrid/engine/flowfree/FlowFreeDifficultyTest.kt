package com.quietgrid.engine.flowfree

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FlowFreeDifficultyTest {
    private fun branchSteps(count: Int, width: Int = 2): List<FlowFreeSolveStep> =
        (1..count).map { FlowFreeSolveStep(FlowFreeTechnique.BRANCH_SEARCH, color = it % 3, branchWidth = width) }

    @Test
    fun `unsolved result classifies as null`() {
        val result = FlowFreeSolveResult(solved = false, steps = emptyList(), solutionPaths = null)
        assertNull(classifyFlowFreeDifficulty(result))
    }

    @Test
    fun `a trace with fewer than 4 branch-search steps classifies as easy`() {
        val result = FlowFreeSolveResult(solved = true, steps = branchSteps(2), solutionPaths = listOf(listOf(0)))
        assertEquals("easy", classifyFlowFreeDifficulty(result))
    }

    @Test
    fun `a trace with 4-5 branch-search steps classifies as medium`() {
        val result = FlowFreeSolveResult(solved = true, steps = branchSteps(5), solutionPaths = listOf(listOf(0)))
        assertEquals("medium", classifyFlowFreeDifficulty(result))
    }

    @Test
    fun `a trace with 6-8 branch-search steps classifies as hard`() {
        val result = FlowFreeSolveResult(solved = true, steps = branchSteps(7), solutionPaths = listOf(listOf(0)))
        assertEquals("hard", classifyFlowFreeDifficulty(result))
    }

    @Test
    fun `a trace with 9 or more branch-search steps classifies as expert`() {
        val result = FlowFreeSolveResult(solved = true, steps = branchSteps(9), solutionPaths = listOf(listOf(0)))
        assertEquals("expert", classifyFlowFreeDifficulty(result))
    }

    @Test
    fun `a single very wide branch-search step also classifies as expert regardless of count`() {
        val result = FlowFreeSolveResult(solved = true, steps = branchSteps(1, width = 4), solutionPaths = listOf(listOf(0)))
        assertEquals("expert", classifyFlowFreeDifficulty(result))
    }
}
