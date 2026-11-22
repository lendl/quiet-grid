package com.quietgrid.engine.takuzu

import com.quietgrid.engine.journey.GridTransform
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Test

class TakuzuTransformTest {
    private val entries = listOf(
        TakuzuPuzzleEntry("p741", 6, "medium", "68dd2a4e5", "140c29020"),
        TakuzuPuzzleEntry("p3", 10, "expert", "65a4baa5b4d2ac94d4da99536", "41800200138008183a60182a8"),
    )

    private fun isValidSolution(grid: List<List<Int>>): Boolean {
        val lines = grid + grid.indices.map { c -> grid.map { it[c] } }
        val balanced = lines.all { line -> line.count { it == 1 } * 2 == line.size }
        val noTrios = lines.all { line -> line.windowed(3).none { it.toSet().size == 1 } }
        val unique = grid.toSet().size == grid.size && grid.indices.map { c -> grid.map { it[c] } }.toSet().size == grid.size
        return balanced && noTrios && unique
    }

    @Test
    fun `every transform keeps a valid solution the given count and exactly one solution`() {
        entries.forEach { entry ->
            assertTrue(takuzuHasOneSolution(entry), entry.id)
            val givenCount = decodeMask(entry.mask, entry.size).sumOf { row -> row.count { it } }
            GridTransform.entries.forEach { t ->
                val moved = entry.transformed(t)
                assertTrue(isValidSolution(decodeSolution(moved.solution, moved.size).map { row -> row.map { it!! } }), "${entry.id} $t")
                assertEquals(givenCount, decodeMask(moved.mask, moved.size).sumOf { row -> row.count { it } }, "${entry.id} $t")
                assertTrue(takuzuHasOneSolution(moved), "${entry.id} $t")
                assertEquals(entry.id, moved.id)
            }
            assertTrue(entry.transformed(GridTransform.ROTATE_90).mask != entry.mask)
        }
    }

    @Test
    fun `replay candidates are every transform but the original`() {
        assertEquals(GridTransform.entries - GridTransform.IDENTITY, takuzuReplayCandidates(entries.first()))
    }
}
