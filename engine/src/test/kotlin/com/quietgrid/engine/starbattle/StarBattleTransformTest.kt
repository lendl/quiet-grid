package com.quietgrid.engine.starbattle

import com.quietgrid.engine.journey.GridTransform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class StarBattleTransformTest {
    private val entry = StarBattlePuzzleEntry(
        id = "sb-x",
        size = 8,
        difficulty = "medium",
        k = 2,
        regions = "1111000013100024333322243733444477355444755556667555566677776666".chunked(8).map { row -> row.map { it.digitToInt() } },
        solution = listOf(listOf(4, 6), listOf(0, 2), listOf(4, 6), listOf(0, 2), listOf(5, 7), listOf(1, 3), listOf(5, 7), listOf(1, 3)),
    )

    private fun isValidSolution(puzzle: StarBattlePuzzleEntry): Boolean {
        val stars = puzzle.solution.flatMapIndexed { row, cols -> cols.map { row to it } }
        val k = puzzle.k
        val rowsOk = puzzle.solution.all { it.size == k }
        val colsOk = (0 until puzzle.size).all { col -> stars.count { it.second == col } == k }
        val regionsOk = (0 until puzzle.size).all { region -> stars.count { (r, c) -> puzzle.regions[r][c] == region } == k }
        val apart = stars.all { a -> stars.none { b -> a != b && abs(a.first - b.first) <= 1 && abs(a.second - b.second) <= 1 } }
        return rowsOk && colsOk && regionsOk && apart
    }

    @Test
    fun `the fixture solution is valid`() {
        assertTrue(isValidSolution(entry))
    }

    @Test
    fun `every transform keeps a valid region grid and solution`() {
        GridTransform.entries.forEach { t ->
            val moved = entry.transformed(t)
            assertTrue(t.name, isValidStarBattleRegionGrid(moved.size, moved.k, moved.regions))
            assertTrue(t.name, isValidSolution(moved))
            assertTrue(t.name, moved.solution.all { it == it.sorted() })
            assertEquals(entry.id, moved.id)
        }
        assertTrue(entry.transformed(GridTransform.ROTATE_90).regions != entry.regions)
    }

    @Test
    fun `every transform keeps exactly one solution`() {
        GridTransform.entries.forEach { t ->
            val moved = entry.transformed(t)
            assertEquals(t.name, 1, countStarBattleSolutions(moved.size, moved.k, moved.regions))
        }
    }

    @Test
    fun `replay candidates are every transform but the original`() {
        assertEquals(GridTransform.entries - GridTransform.IDENTITY, starBattleReplayCandidates(entry))
    }
}
