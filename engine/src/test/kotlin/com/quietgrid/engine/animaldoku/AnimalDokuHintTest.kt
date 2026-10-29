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
    fun `chain hints use the shortest chain and list the open cells of every unit in it`() {
        val puzzle = AnimalDokuPuzzleEntry(
            id = "chain-test",
            size = 8,
            difficulty = "hard",
            regions = listOf(
                listOf(0, 0, 2, 3, 3, 3, 3, 3),
                listOf(0, 0, 2, 3, 3, 3, 1, 1),
                listOf(2, 2, 2, 3, 3, 3, 3, 1),
                listOf(2, 2, 3, 3, 2, 3, 3, 1),
                listOf(5, 2, 2, 2, 2, 5, 3, 4),
                listOf(5, 5, 5, 5, 5, 5, 4, 4),
                listOf(6, 5, 5, 5, 7, 5, 5, 4),
                listOf(6, 6, 5, 5, 7, 7, 7, 4),
            ),
            solution = listOf(1, 6, 2, 5, 7, 3, 0, 4),
        )
        val excluded = setOf(
            0 to 2, 0 to 3, 0 to 4, 0 to 5, 0 to 6, 0 to 7, 1 to 0, 1 to 1, 1 to 7, 2 to 0, 2 to 1, 2 to 6, 2 to 7,
            3 to 0, 3 to 1, 3 to 6, 3 to 7, 4 to 0, 4 to 1, 4 to 2, 4 to 3, 4 to 4, 4 to 6, 5 to 0, 5 to 1,
            6 to 1, 6 to 2, 6 to 3, 6 to 5, 6 to 6, 6 to 7, 7 to 2, 7 to 3, 7 to 6, 7 to 7,
        )
        val state = AnimalDokuSolverState(8, puzzle.regions)
        state.place(1, 6)
        excluded.forEach { (row, col) -> state.eliminated[row][col] = true }

        val hint = findAnimalDokuHint(puzzle, placed = setOf(1 to 6), excluded = excluded) as AnimalDokuHint.Deduction
        val step = hint.step as AnimalDokuStep.Elimination
        assertEquals(AnimalDokuTechnique.CHAIN, step.technique)
        assertTrue(step.chainDepth <= 3)
        val chainUnits = step.forced.map { it.unit } + step.sources
        assertEquals(chainUnits.flatMap { state.candidatesIn(it) }.toSet(), hint.chainOpenCells)
        assertEquals(step.sources.flatMap { state.candidatesIn(it) }.toSet(), hint.openCells)
    }

    @Test
    fun `non-chain hints have no chain open cells`() {
        val hint = findAnimalDokuHint(HINT_PUZZLE, placed = emptySet(), excluded = emptySet()) as AnimalDokuHint.Deduction
        assertTrue(hint.chainOpenCells.isEmpty())
    }

    @Test
    fun `units list their cells`() {
        assertEquals(listOf(2 to 0, 2 to 1, 2 to 2, 2 to 3, 2 to 4), AnimalDokuUnit(AnimalDokuUnitKind.ROW, 2).cells(HINT_PUZZLE.regions))
        assertEquals(listOf(0 to 4, 1 to 4, 2 to 4, 3 to 4, 4 to 4), AnimalDokuUnit(AnimalDokuUnitKind.COLUMN, 4).cells(HINT_PUZZLE.regions))
        assertEquals(listOf(3 to 2, 3 to 3), AnimalDokuUnit(AnimalDokuUnitKind.REGION, 3).cells(HINT_PUZZLE.regions))
    }
}
