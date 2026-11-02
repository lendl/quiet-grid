package com.quietgrid.engine.animaldoku

import com.quietgrid.engine.journey.GridTransform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class AnimalDokuTransformTest {
    private val regions = listOf(
        listOf(4, 4, 4, 2, 2),
        listOf(4, 4, 2, 2, 2),
        listOf(4, 4, 2, 2, 0),
        listOf(1, 1, 3, 3, 0),
        listOf(1, 1, 1, 0, 0),
    )

    private fun permutations(items: List<Int>): List<List<Int>> =
        if (items.size <= 1) listOf(items) else items.flatMap { head -> permutations(items - head).map { listOf(head) + it } }

    private fun isValidSolution(size: Int, regions: List<List<Int>>, solution: List<Int>): Boolean =
        solution.size == size &&
            solution.toSet().size == size &&
            solution.indices.map { regions[it][solution[it]] }.toSet().size == size &&
            solution.zipWithNext().all { (a, b) -> abs(a - b) > 1 }

    private val entry: AnimalDokuPuzzleEntry = run {
        val solution = permutations((0 until 5).toList()).single { isValidSolution(5, regions, it) }
        AnimalDokuPuzzleEntry("ad-x", 5, "easy", regions, solution)
    }

    @Test
    fun `every transform keeps a valid region grid`() {
        GridTransform.entries.forEach { t ->
            val moved = entry.transformed(t)
            assertTrue(t.name, isValidAnimalDokuRegionGrid(moved.size, moved.regions))
            assertEquals(entry.id, moved.id)
        }
    }

    @Test
    fun `every transform keeps a valid solution`() {
        GridTransform.entries.forEach { t ->
            val moved = entry.transformed(t)
            assertTrue(t.name, isValidSolution(moved.size, moved.regions, moved.solution))
        }
        assertTrue(entry.transformed(GridTransform.ROTATE_90).regions != entry.regions)
    }

    @Test
    fun `every transform still solves`() {
        GridTransform.entries.forEach { t ->
            val moved = entry.transformed(t)
            assertTrue(t.name, solveAnimalDoku(moved.size, moved.regions).solved)
        }
    }

    @Test
    fun `replay candidates are every transform but the original`() {
        assertEquals(GridTransform.entries - GridTransform.IDENTITY, animalDokuReplayCandidates(entry))
    }
}
