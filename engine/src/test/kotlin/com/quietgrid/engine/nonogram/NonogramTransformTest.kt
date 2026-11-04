package com.quietgrid.engine.nonogram

import com.quietgrid.engine.journey.GridTransform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NonogramTransformTest {
    private fun entry(id: String, difficulty: String, rows: List<String>) = NonogramPuzzleEntry(
        id = id,
        difficulty = difficulty,
        rows = rows.size,
        cols = rows.first().length,
        solution = rows.map { row -> row.map { it == '1' } },
    )

    private val narrow = entry("n10x5-64", "hard", listOf("00000", "01010", "01011", "11111", "11111", "01111", "00100", "00111", "00110", "00100"))
    private val square = entry(
        "n10x10-293",
        "expert",
        listOf("0000000110", "1110001110", "0100001110", "1111110110", "0000111110", "0111111010", "0000111000", "0001111000", "0001111000", "0000100000"),
    )

    @Test
    fun `square boards allow every transform and narrow boards keep their shape`() {
        assertEquals(GridTransform.entries - GridTransform.IDENTITY, nonogramReplayCandidates(square))
        assertEquals(listOf(GridTransform.MIRROR_H, GridTransform.ROTATE_180, GridTransform.MIRROR_V), nonogramReplayCandidates(narrow))
    }

    @Test
    fun `every candidate transform keeps the filled count, the shape and a line solve`() {
        listOf(narrow, square).forEach { puzzle ->
            assertNotNull(puzzle.id, analyzeNonogramEntry(puzzle))
            val filled = puzzle.solution.sumOf { row -> row.count { it } }
            nonogramReplayCandidates(puzzle).forEach { t ->
                val moved = puzzle.transformed(t)
                assertEquals("${puzzle.id} $t", moved.rows, moved.solution.size)
                assertTrue("${puzzle.id} $t", moved.solution.all { it.size == moved.cols })
                assertEquals("${puzzle.id} $t", filled, moved.solution.sumOf { row -> row.count { it } })
                assertNotNull("${puzzle.id} $t", analyzeNonogramEntry(moved))
                assertEquals(puzzle.id, moved.id)
            }
        }
        assertEquals(10 to 10, square.transformed(GridTransform.ROTATE_90).let { it.rows to it.cols })
        assertTrue(square.transformed(GridTransform.ROTATE_90).solution != square.solution)
    }
}
