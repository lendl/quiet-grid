package com.quietgrid.engine.links

import com.quietgrid.engine.core.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class LinksDifficultyTest {
    private val endpoints = listOf(listOf(0, 2), listOf(3, 4))

    private fun steps(vararg techniques: LinksTechnique) =
        LinksSolveResult(true, techniques.map { LinksSolveStep(it, if (it == LinksTechnique.FORCED) 0 else 4) })

    @Test
    fun `a head with one free neighbor is forced`() {
        val state = LinksSolverState.of(3, endpoints)
        assertEquals(LinksDeduction(LinksTechnique.FORCED, LinksMove(0, 0, 1)), findLinksForced(state))
    }

    @Test
    fun `an empty cell with two ways in must take the pipe beside it`() {
        val state = LinksSolverState.of(3, endpoints)
        assertEquals(LinksMove(1, 0, 6), findLinksPocket(state)?.move)
    }

    @Test
    fun `a move that strands a cell is ruled out by lookahead`() {
        val state = LinksSolverState.of(3, endpoints)
        assertEquals(LinksDeduction(LinksTechnique.LOOKAHEAD, LinksMove(0, 1, 1)), findLinksLookahead(state))
    }

    @Test
    fun `a stranded cell between two different pipes is infeasible`() {
        val state = LinksSolverState.of(3, endpoints)
        state.apply(LinksMove(0, 1, 5))
        assertTrue(!state.isFeasible())
    }

    @Test
    fun `the small board solves with easy steps only`() {
        val result = solveLinks(3, endpoints)
        assertTrue(result.solved)
        assertTrue(result.steps.all { it.technique == LinksTechnique.FORCED || it.technique == LinksTechnique.POCKET })
        assertEquals(Difficulty.EASY, classifyLinksDifficulty(result))
    }

    @Test
    fun `tiers follow the hardest repeated technique`() {
        assertEquals(Difficulty.EASY, classifyLinksDifficulty(steps(LinksTechnique.FORCED, LinksTechnique.LOOKAHEAD)))
        assertEquals(Difficulty.MEDIUM, classifyLinksDifficulty(steps(LinksTechnique.LOOKAHEAD, LinksTechnique.LOOKAHEAD)))
        assertEquals(Difficulty.HARD, classifyLinksDifficulty(steps(LinksTechnique.CHAIN)))
        assertEquals(Difficulty.HARD, classifyLinksDifficulty(steps(LinksTechnique.CHAIN, LinksTechnique.CHAIN, LinksTechnique.CHAIN)))
        assertEquals(Difficulty.HARD, classifyLinksDifficulty(steps(LinksTechnique.DEEP_CHAIN, LinksTechnique.CHAIN)))
        assertEquals(
            Difficulty.EXPERT,
            classifyLinksDifficulty(steps(LinksTechnique.CHAIN, LinksTechnique.DEEP_CHAIN, LinksTechnique.CHAIN)),
        )
    }

    @Test
    fun `a puzzle the solver cannot finish has no tier`() {
        assertNull(classifyLinksDifficulty(LinksSolveResult(false, emptyList())))
    }

    @Test
    fun `solved generated puzzles end in a valid full board`() {
        val random = Random(5)
        var checked = 0
        while (checked < 10) {
            val solution = generateLinksSolution(6, 5, random) ?: continue
            val ends = linksEndpointsOf(solution)
            if (countLinksSolutions(6, ends).solutions != 1) continue
            val state = LinksSolverState.of(6, ends)
            while (!state.isSolved()) {
                val deduction = nextLinksDeduction(state) ?: break
                state.apply(deduction.move)
            }
            if (state.isSolved()) {
                solution.forEachIndexed { color, path -> path.forEach { assertEquals(color, state.ownerOf(it)) } }
            }
            checked++
        }
    }
}
