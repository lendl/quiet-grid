package com.quietgrid.engine.links

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class LinksColoringTest {
    private val confusable = setOf(0 to 1, 2 to 3, 4 to 5)

    @Test
    fun `pairs that touch get colors that are not alike`() {
        val random = Random(3)
        repeat(20) {
            val solution = generateLinksSolution(9, 10, random) ?: return@repeat
            val colors = assignLinksPairColors(9, solution, 14, confusable)
            assertEquals(solution.size, colors.toSet().size)
            linksPairNeighbors(9, solution).forEachIndexed { pair, neighbors ->
                neighbors.forEach { other ->
                    assertFalse((colors[pair] to colors[other]) in confusable)
                    assertFalse((colors[other] to colors[pair]) in confusable)
                }
            }
        }
    }

    @Test
    fun `a small palette is reused but never between touching pairs`() {
        val random = Random(9)
        repeat(20) {
            val solution = generateLinksSolution(9, 11, random) ?: return@repeat
            val colors = assignLinksPairColors(9, solution, 8, emptySet())
            assertTrue(colors.all { it in 0 until 8 })
            linksPairNeighbors(9, solution).forEachIndexed { pair, neighbors ->
                neighbors.forEach { other -> assertTrue(colors[pair] != colors[other]) }
            }
        }
    }
}
