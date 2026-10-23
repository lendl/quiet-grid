package com.quietgrid.engine.links

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LinksHintTest {
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

    @Test
    fun `a fresh board hints the first logical step`() {
        assertEquals(LinksHint(0, LinksTechnique.FORCED, isMistake = false), findLinksHint(puzzle, emptyLinksPaths(puzzle)))
    }

    @Test
    fun `a pipe off its route is fixed first`() {
        val paths = listOf(emptyList(), listOf(3, 6, 7, 4))
        assertEquals(LinksHint(1, null, isMistake = true), findLinksHint(puzzle, paths))
    }

    @Test
    fun `progress on track is kept and the next pair is hinted`() {
        val paths = listOf(listOf(2, 1, 0), listOf(3, 6))
        assertEquals(1, findLinksHint(puzzle, paths)?.color)
    }

    @Test
    fun `a solved board has no hint`() {
        assertNull(findLinksHint(puzzle, solution))
    }
}
