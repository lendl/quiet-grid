package com.quietgrid.engine.animaldoku

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private val HINT_PUZZLE = AnimalDokuPuzzleEntry(
    id = "hint-test",
    size = 5,
    difficulty = "easy",
    regions = listOf(
        listOf(4, 4, 4, 2, 2),
        listOf(4, 4, 2, 2, 2),
        listOf(4, 4, 2, 2, 0),
        listOf(1, 1, 3, 3, 0),
        listOf(1, 1, 1, 0, 0),
    ),
    solution = listOf(3, 1, 4, 2, 0),
)

class AnimalDokuHintTest {
    @Test
    fun `following hints from an empty board places every animal without guessing`() {
        val placed = mutableSetOf<Pair<Int, Int>>()
        val excluded = mutableSetOf<Pair<Int, Int>>()
        var reveals = 0
        repeat(100) {
            when (val hint = findAnimalDokuHint(HINT_PUZZLE, placed, excluded)) {
                null -> {
                    assertEquals(5, placed.size)
                    assertEquals(0, reveals)
                    return
                }
                is AnimalDokuHint.WrongMark -> error("unexpected wrong mark $hint")
                is AnimalDokuHint.Reveal -> {
                    reveals++
                    placed.add(hint.row to hint.col)
                }
                is AnimalDokuHint.Deduction -> when (val step = hint.step) {
                    is AnimalDokuStep.Placement -> {
                        assertEquals(HINT_PUZZLE.solution[step.row], step.col)
                        assertEquals(setOf(step.row to step.col), hint.openCells)
                        placed.add(step.row to step.col)
                    }
                    is AnimalDokuStep.Elimination -> {
                        assertTrue(step.cells.isNotEmpty())
                        step.cells.forEach { (row, col) -> assertTrue(HINT_PUZZLE.solution[row] != col) }
                        excluded.addAll(step.cells)
                    }
                }
            }
        }
        error("hints did not finish the puzzle")
    }

    @Test
    fun `an X on an animal's cell is reported before any deduction`() {
        val hint = findAnimalDokuHint(HINT_PUZZLE, placed = emptySet(), excluded = setOf(0 to 0, 2 to 4))
        assertEquals(AnimalDokuHint.WrongMark(2, 4), hint)
    }

    @Test
    fun `a finished board has no hint`() {
        val placed = HINT_PUZZLE.solution.mapIndexed { row, col -> row to col }.toSet()
        assertNull(findAnimalDokuHint(HINT_PUZZLE, placed, emptySet()))
    }

    @Test
    fun `confinement hints name the region and the row it is confined to`() {
        val hint = findAnimalDokuHint(HINT_PUZZLE, placed = emptySet(), excluded = emptySet()) as AnimalDokuHint.Deduction
        val step = hint.step as AnimalDokuStep.Elimination
        assertTrue(step.technique == AnimalDokuTechnique.STRUCTURAL_CONFINEMENT || step.technique == AnimalDokuTechnique.CONFINEMENT)
        assertEquals(1, step.sources.size)
        assertEquals(1, step.targets.size)
        val targetCells = step.targets.single().cells(HINT_PUZZLE.regions).toSet()
        assertTrue(hint.openCells.isNotEmpty())
        assertTrue(targetCells.containsAll(hint.openCells))
        assertTrue(targetCells.containsAll(step.cells))
    }

    @Test
    fun `open cells skip cells next to a placed animal`() {
        val hint = findAnimalDokuHint(HINT_PUZZLE, placed = setOf(4 to 0), excluded = emptySet()) as AnimalDokuHint.Deduction
        assertTrue(hint.openCells.none { (row, col) -> row >= 3 && col <= 1 })
    }

    @Test
    fun `units list their cells`() {
        assertEquals(listOf(2 to 0, 2 to 1, 2 to 2, 2 to 3, 2 to 4), AnimalDokuUnit(AnimalDokuUnitKind.ROW, 2).cells(HINT_PUZZLE.regions))
        assertEquals(listOf(0 to 4, 1 to 4, 2 to 4, 3 to 4, 4 to 4), AnimalDokuUnit(AnimalDokuUnitKind.COLUMN, 4).cells(HINT_PUZZLE.regions))
        assertEquals(listOf(3 to 2, 3 to 3), AnimalDokuUnit(AnimalDokuUnitKind.REGION, 3).cells(HINT_PUZZLE.regions))
    }
}
