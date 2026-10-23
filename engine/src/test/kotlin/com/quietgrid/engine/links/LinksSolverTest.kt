package com.quietgrid.engine.links

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class LinksSolverTest {
    @Test
    fun `a board with one way to fill it has one solution`() {
        val endpoints = listOf(listOf(0, 2), listOf(3, 4))
        val count = countLinksSolutions(3, endpoints)
        assertTrue(count.exhausted)
        assertEquals(1, count.solutions)
    }

    @Test
    fun `a board that can be filled two ways reports two solutions`() {
        val endpoints = listOf(listOf(0, 8))
        val count = countLinksSolutions(3, endpoints)
        assertEquals(2, count.solutions)
    }

    @Test
    fun `a board that cannot be filled has no solution`() {
        val endpoints = listOf(listOf(0, 8), listOf(2, 6))
        assertEquals(0, countLinksSolutions(3, endpoints).solutions)
    }

    @Test
    fun `generated solutions cover the grid with valid pipes that never touch themselves`() {
        val random = Random(7)
        repeat(20) {
            val solution = generateLinksSolution(7, 7, random) ?: return@repeat
            assertTrue(isValidLinksSolution(7, linksEndpointsOf(solution), solution))
            solution.forEach { path ->
                assertTrue(path.size >= LINKS_MIN_PATH_LENGTH)
                path.forEachIndexed { i, a ->
                    path.forEachIndexed { j, b ->
                        if (kotlin.math.abs(i - j) > 1) assertTrue(!linksAreAdjacent(a, b, 7))
                    }
                }
            }
        }
    }

    @Test
    fun `the solver finds the generated solution`() {
        val random = Random(11)
        var checked = 0
        while (checked < 5) {
            val solution = generateLinksSolution(5, 5, random) ?: continue
            val count = countLinksSolutions(5, linksEndpointsOf(solution), cap = 1)
            assertEquals(1, count.solutions)
            checked++
        }
    }
}
