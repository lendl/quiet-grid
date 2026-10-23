package com.quietgrid.engine.links

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LinksBoardTest {
    private val solution = listOf(
        listOf(0, 1, 2),
        listOf(3, 6, 7, 8, 5, 4),
    )
    private val puzzle = LinksPuzzleEntry(
        id = "test",
        size = 3,
        difficulty = "easy",
        endpoints = linksEndpointsOf(solution),
        solution = solution,
    )

    private fun draw(paths: List<List<Int>>, vararg cells: Int): List<List<Int>> {
        var stroke = startLinksStroke(puzzle, paths, cells.first())!!
        cells.drop(1).forEach { stroke = extendLinksStroke(puzzle, stroke, it) }
        return finishLinksStroke(stroke)
    }

    @Test
    fun `drawing every pair along its solution solves the puzzle`() {
        var paths = emptyLinksPaths(puzzle)
        paths = draw(paths, 0, 1, 2)
        paths = draw(paths, 3, 6, 7, 8, 5, 4)
        assertTrue(isLinksSolved(puzzle, paths))
        assertEquals(2, linksConnectedCount(puzzle, paths))
    }

    @Test
    fun `connected pairs without filling every cell do not solve the puzzle`() {
        val shortcut = listOf(listOf(0, 1, 2), listOf(3, 4))
        assertEquals(2, linksConnectedCount(puzzle, shortcut))
        assertFalse(isLinksSolved(puzzle, shortcut))
    }

    @Test
    fun `a long jump walks through every cell on the way`() {
        val paths = draw(emptyLinksPaths(puzzle), 3, 6, 8)
        assertEquals(listOf(3, 6, 7, 8), paths[1])
    }

    @Test
    fun `a stroke cannot enter another pair's dot`() {
        val paths = draw(emptyLinksPaths(puzzle), 0, 3)
        assertEquals(emptyList<Int>(), paths[0])
    }

    @Test
    fun `drawing back over your own pipe shortens it`() {
        val paths = draw(emptyLinksPaths(puzzle), 3, 6, 7, 8, 7)
        assertEquals(listOf(3, 6, 7), paths[1])
    }

    @Test
    fun `crossing another pipe cuts it at the crossing`() {
        val start = draw(emptyLinksPaths(puzzle), 3, 6, 7, 8, 5)
        val paths = draw(start, 0, 1, 2)
        assertEquals(listOf(3, 6, 7, 8, 5), paths[1])
        val crossed = draw(start, 2, 5)
        assertEquals(listOf(3, 6, 7, 8), crossed[1])
        assertEquals(listOf(2, 5), crossed[0])
    }

    @Test
    fun `starting from the middle of a pipe keeps the part behind it`() {
        val start = draw(emptyLinksPaths(puzzle), 3, 6, 7, 8)
        val stroke = startLinksStroke(puzzle, start, 7)!!
        assertEquals(1, stroke.color)
        assertEquals(listOf(3, 6, 7), stroke.path)
    }

    @Test
    fun `a finished pipe stops at its dot`() {
        val paths = draw(emptyLinksPaths(puzzle), 0, 1, 2, 5)
        assertEquals(listOf(0, 1, 2), paths[0])
    }

    @Test
    fun `starting on an empty cell does nothing`() {
        assertNull(startLinksStroke(puzzle, emptyLinksPaths(puzzle), 7))
    }

    @Test
    fun `accuracy is pairs over moves and a hint costs two moves`() {
        assertEquals(100, linksAccuracyPercent(pairCount = 5, moves = 5, hintsUsed = 0))
        assertEquals(50, linksAccuracyPercent(pairCount = 5, moves = 10, hintsUsed = 0))
        assertEquals(83, linksAccuracyPercent(pairCount = 5, moves = 4, hintsUsed = 1))
    }

    @Test
    fun `the hint picks the shortest pair that is not drawn as its solution`() {
        assertEquals(0, linksHintColor(puzzle, emptyLinksPaths(puzzle)))
        val hinted = applyLinksSolutionPath(puzzle, emptyLinksPaths(puzzle), 0)
        assertEquals(listOf(0, 1, 2), hinted[0])
        assertEquals(1, linksHintColor(puzzle, hinted))
    }
}
