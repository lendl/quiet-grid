// cli/src/test/kotlin/com/quietgrid/cli/animaldoku/AnimalDokuGeneratorTest.kt
package com.quietgrid.cli.animaldoku

import com.quietgrid.engine.animaldoku.isValidAnimalDokuRegionGrid
import com.quietgrid.engine.animaldoku.solveAnimalDoku
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

private val QUADRANT_REGIONS = listOf(
    listOf(0, 0, 1, 1),
    listOf(0, 0, 1, 1),
    listOf(2, 2, 3, 3),
    listOf(2, 2, 3, 3),
)

private val QUADRANT_SOLUTION = intArrayOf(1, 3, 0, 2)

private val LOPSIDED_REGIONS = listOf(
    listOf(0, 0, 2, 1),
    listOf(2, 2, 2, 1),
    listOf(2, 3, 3, 3),
    listOf(3, 3, 3, 3),
)

private fun regionSizesOf(size: Int, regions: List<List<Int>>): IntArray {
    val sizes = IntArray(size)
    for (row in 0 until size) for (col in 0 until size) sizes[regions[row][col]]++
    return sizes
}

class AnimalDokuGeneratorTest {
    @Test
    fun `generateSolutionPermutation produces a permutation with no two consecutive rows within 1 column`() {
        val perm = generateSolutionPermutation(6)
        assertNotNull(perm)
        checkNotNull(perm)
        assertEquals((0..5).toSet(), perm.toSet())
        for (row in 0 until perm.size - 1) {
            assertTrue(abs(perm[row] - perm[row + 1]) >= 2)
        }
    }

    @Test
    fun `growRegions produces a full valid connected region grid seeded by the solution`() {
        val solution = generateSolutionPermutation(6)
        checkNotNull(solution)
        val regions = growRegions(6, solution)
        assertNotNull(regions)
        checkNotNull(regions)
        assertTrue(isValidAnimalDokuRegionGrid(6, regions))
        for (row in 0 until 6) {
            assertEquals(row, regions[row][solution[row]])
        }
    }

    @Test
    fun `generateAnimalDokuPuzzle produces a puzzle classified at the requested difficulty and size`() {
        val puzzle = generateAnimalDokuPuzzle(size = 5, targetDifficulty = com.quietgrid.engine.core.Difficulty.EASY, idPrefix = "ad5")
        assertNotNull(puzzle)
        checkNotNull(puzzle)
        assertEquals(5, puzzle.size)
        assertEquals("easy", puzzle.difficulty)
        assertTrue(isValidAnimalDokuRegionGrid(5, puzzle.regions))
    }

    @Test
    fun `generateAnimalDokuPuzzle at easy difficulty always includes a single-cell freebie region`() {
        val puzzle = generateAnimalDokuPuzzle(size = 5, targetDifficulty = com.quietgrid.engine.core.Difficulty.EASY, idPrefix = "ad5easy")
        assertNotNull(puzzle)
        checkNotNull(puzzle)
        val sizes = regionSizesOf(5, puzzle.regions)
        assertTrue("expected a size-1 region among ${sizes.toList()}", sizes.any { it == 1 })
    }

    @Test
    fun `generateAnimalDokuPuzzle at medium difficulty does not guarantee a single-cell region`() {
        val puzzle = generateAnimalDokuPuzzle(size = 6, targetDifficulty = com.quietgrid.engine.core.Difficulty.MEDIUM, idPrefix = "ad6medium")
        assertNotNull(puzzle)
        checkNotNull(puzzle)
        assertEquals(6, puzzle.size)
        assertEquals("medium", puzzle.difficulty)
        assertTrue(isValidAnimalDokuRegionGrid(6, puzzle.regions))
    }

    @Test
    fun `repairRegionsTowardUniqueSolution repairs an ambiguous stuck layout into a solved one`() {
        assertFalse(solveAnimalDoku(4, QUADRANT_REGIONS).solved)

        val repaired = repairRegionsTowardUniqueSolution(4, QUADRANT_SOLUTION, QUADRANT_REGIONS)
        assertNotNull(repaired)
        checkNotNull(repaired)
        assertTrue(repaired.solveResult.solved)
        assertTrue(isValidAnimalDokuRegionGrid(4, repaired.regions))
        assertEquals(
            (0..3).toSet(),
            (0..3).map { row -> repaired.regions[row][QUADRANT_SOLUTION[row]] }.toSet(),
        )
    }

    @Test
    fun `mutateOneBoundaryCell never reassigns a solution cell and changes exactly one cell`() {
        var regions = QUADRANT_REGIONS
        val originalSolutionRegions = (0..3).map { row -> regions[row][QUADRANT_SOLUTION[row]] }
        repeat(200) {
            val mutated = mutateOneBoundaryCell(4, QUADRANT_SOLUTION, regions) ?: return@repeat
            val changed = (0..3).flatMap { r -> (0..3).map { c -> r to c } }
                .filter { (r, c) -> mutated[r][c] != regions[r][c] }
            assertEquals(1, changed.size)
            val (changedRow, changedCol) = changed.single()
            assertTrue(changedCol != QUADRANT_SOLUTION[changedRow])
            regions = mutated
            for (row in 0..3) {
                assertEquals(originalSolutionRegions[row], regions[row][QUADRANT_SOLUTION[row]])
            }
        }
    }

    @Test
    fun `boundaryMutationCandidates only donates from regions big enough to survive the loss`() {
        val sizes = regionSizesOf(4, LOPSIDED_REGIONS)
        assertEquals(listOf(2, 2, 5, 7), sizes.toList())

        val biased = boundaryMutationCandidates(4, QUADRANT_SOLUTION, LOPSIDED_REGIONS)
        assertTrue(biased.isNotEmpty())
        for ((row, col, _) in biased) {
            assertTrue(
                "candidate ($row,$col) donates from region ${LOPSIDED_REGIONS[row][col]} of size " +
                    "${sizes[LOPSIDED_REGIONS[row][col]]}",
                sizes[LOPSIDED_REGIONS[row][col]] >= MIN_DONOR_REGION_SIZE_TO_PREFER,
            )
        }

        val unrestricted = boundaryMutationCandidates(4, QUADRANT_SOLUTION, LOPSIDED_REGIONS, minDonorRegionSizeToPrefer = 0)
        assertTrue(unrestricted.any { (row, col, _) -> sizes[LOPSIDED_REGIONS[row][col]] < MIN_DONOR_REGION_SIZE_TO_PREFER })
        assertTrue(unrestricted.size > biased.size)
    }

    @Test
    fun `mutateOneBoundaryCell never shrinks a two-cell region to one while a bigger donor exists`() {
        repeat(200) {
            val mutated = mutateOneBoundaryCell(4, QUADRANT_SOLUTION, LOPSIDED_REGIONS)
            assertNotNull(mutated)
            checkNotNull(mutated)
            assertTrue(regionSizesOf(4, mutated).none { it == 1 })
        }
    }

    @Test
    fun `mutateOneBoundaryCell falls back to the unrestricted pool when no donor is big enough`() {
        val mutated = mutateOneBoundaryCell(4, QUADRANT_SOLUTION, LOPSIDED_REGIONS, minDonorRegionSizeToPrefer = 99)
        assertNotNull(mutated)
        checkNotNull(mutated)
        val changed = (0..3).flatMap { r -> (0..3).map { c -> r to c } }
            .filter { (r, c) -> mutated[r][c] != LOPSIDED_REGIONS[r][c] }
        assertEquals(1, changed.size)
    }

    @Test
    fun `forceOneSingleCellRegion shrinks the smallest region down to just its solution cell`() {
        val shrunk = forceOneSingleCellRegion(4, QUADRANT_SOLUTION, QUADRANT_REGIONS)
        assertNotNull(shrunk)
        checkNotNull(shrunk)
        assertTrue(isValidAnimalDokuRegionGrid(4, shrunk))

        val sizes = regionSizesOf(4, shrunk)
        assertEquals(1, sizes[0])
        assertTrue("expected every other region to still hold more than one cell: ${sizes.toList()}", sizes.drop(1).all { it > 1 })

        for (row in 0..3) {
            assertEquals(row, shrunk[row][QUADRANT_SOLUTION[row]])
        }
        assertEquals(0, shrunk[0][QUADRANT_SOLUTION[0]])
    }

    @Test
    fun `forceOneSingleCellRegion never reassigns any regions solution cell`() {
        val shrunk = forceOneSingleCellRegion(4, QUADRANT_SOLUTION, QUADRANT_REGIONS)
        assertNotNull(shrunk)
        checkNotNull(shrunk)
        assertEquals(
            (0..3).toSet(),
            (0..3).map { row -> shrunk[row][QUADRANT_SOLUTION[row]] }.toSet(),
        )
    }

    @Test
    fun `isConnectedWithoutCell rejects a removal that splits its region in two`() {
        val bridgeRegions = listOf(
            listOf(1, 0, 2, 2),
            listOf(1, 0, 0, 0),
            listOf(1, 1, 3, 3),
            listOf(1, 3, 3, 3),
        )
        assertFalse(isConnectedWithoutCell(4, bridgeRegions, region = 0, excluded = 1 to 1))
        assertTrue(isConnectedWithoutCell(4, bridgeRegions, region = 0, excluded = 1 to 3))
    }

    @Test
    fun `defaultMaxAttemptsFor is cut down sharply for hard and expert but unchanged for easy and medium`() {
        assertEquals(50, defaultMaxAttemptsFor(com.quietgrid.engine.core.Difficulty.HARD))
        assertEquals(20, defaultMaxAttemptsFor(com.quietgrid.engine.core.Difficulty.EXPERT))
        assertEquals(300, defaultMaxAttemptsFor(com.quietgrid.engine.core.Difficulty.EASY))
        assertEquals(300, defaultMaxAttemptsFor(com.quietgrid.engine.core.Difficulty.MEDIUM))
    }
}
