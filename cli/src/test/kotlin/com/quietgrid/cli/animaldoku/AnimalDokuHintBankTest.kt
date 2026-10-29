package com.quietgrid.cli.animaldoku

import com.quietgrid.engine.animaldoku.AnimalDokuHint
import com.quietgrid.engine.animaldoku.AnimalDokuPuzzleEntry
import com.quietgrid.engine.animaldoku.AnimalDokuSolverState
import com.quietgrid.engine.animaldoku.AnimalDokuStep
import com.quietgrid.engine.animaldoku.AnimalDokuTechnique
import com.quietgrid.engine.animaldoku.candidatesIn
import com.quietgrid.engine.animaldoku.cells
import com.quietgrid.engine.animaldoku.findAnimalDokuHint
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

class AnimalDokuHintBankTest {
    private val bank: List<AnimalDokuPuzzleEntry> =
        Json.decodeFromString(ListSerializer(AnimalDokuPuzzleEntry.serializer()), File("app/src/main/assets/animaldoku_puzzles.json").readText())

    @Test
    fun `hints solve every bank puzzle from an empty board without reveals`() {
        bank.forEach { entry ->
            assertEquals(entry.id, 0, followHints(entry, mutableSetOf()))
        }
    }

    @Test
    fun `hints stay correct when the player has extra X marks`() {
        val random = Random(7)
        bank.forEach { entry ->
            val excluded = mutableSetOf<Pair<Int, Int>>()
            for (row in 0 until entry.size) for (col in 0 until entry.size) {
                if (entry.solution[row] != col && random.nextInt(4) == 0) excluded.add(row to col)
            }
            followHints(entry, excluded)
        }
    }

    private fun followHints(entry: AnimalDokuPuzzleEntry, excluded: MutableSet<Pair<Int, Int>>): Int {
        val placed = mutableSetOf<Pair<Int, Int>>()
        var reveals = 0
        repeat(entry.size * entry.size + entry.size + 1) {
            when (val hint = findAnimalDokuHint(entry, placed, excluded)) {
                null -> {
                    assertEquals(entry.id, entry.size, placed.size)
                    return reveals
                }
                is AnimalDokuHint.WrongMark -> error("${entry.id}: unexpected wrong mark $hint")
                is AnimalDokuHint.Reveal -> {
                    assertEquals(entry.id, entry.solution[hint.row], hint.col)
                    reveals++
                    placed.add(hint.row to hint.col)
                }
                is AnimalDokuHint.Deduction -> {
                    checkDeduction(entry, hint, placed, excluded)
                    when (val step = hint.step) {
                        is AnimalDokuStep.Placement -> placed.add(step.row to step.col)
                        is AnimalDokuStep.Elimination -> excluded.addAll(step.cells)
                    }
                }
            }
        }
        error("${entry.id}: hints did not finish the puzzle")
    }

    private fun checkDeduction(
        entry: AnimalDokuPuzzleEntry,
        hint: AnimalDokuHint.Deduction,
        placed: Set<Pair<Int, Int>>,
        excluded: Set<Pair<Int, Int>>,
    ) {
        val regions = entry.regions
        assertTrue(entry.id, hint.openCells.isNotEmpty())
        when (val step = hint.step) {
            is AnimalDokuStep.Placement -> {
                assertEquals(entry.id, entry.solution[step.row], step.col)
                assertEquals(entry.id, setOf(step.row to step.col), hint.openCells)
                assertTrue(entry.id, (step.row to step.col) in step.unit.cells(regions))
            }
            is AnimalDokuStep.Elimination -> {
                assertTrue(entry.id, step.cells.isNotEmpty())
                step.cells.forEach { (row, col) -> assertNotEquals(entry.id, entry.solution[row], col) }
                val sourceCells = step.sources.flatMap { it.cells(regions) }.toSet()
                assertTrue(entry.id, sourceCells.containsAll(hint.openCells))
                if (step.technique == AnimalDokuTechnique.CHAIN) {
                    assertEquals(entry.id, 1, step.sources.size)
                    assertEquals(entry.id, 1, step.cells.size)
                    assertTrue(entry.id, step.cells.none { it in sourceCells })
                    assertEquals(entry.id, 1 + step.forced.size, step.chainDepth)
                    assertEquals(entry.id, hint.openCells, hint.chainOpenCells.filter { it in sourceCells }.toSet())
                    val hypothesis = AnimalDokuSolverState(entry.size, regions)
                    placed.forEach { (row, col) -> hypothesis.place(row, col) }
                    excluded.forEach { (row, col) -> hypothesis.eliminated[row][col] = true }
                    val (triedRow, triedCol) = step.cells.single()
                    hypothesis.place(triedRow, triedCol)
                    step.forced.forEach { link ->
                        assertTrue(entry.id, (link.row to link.col) in hint.chainOpenCells)
                        assertEquals(entry.id, listOf(link.row to link.col), hypothesis.candidatesIn(link.unit))
                        hypothesis.place(link.row, link.col)
                    }
                    assertTrue(entry.id, hypothesis.candidatesIn(step.sources.single()).isEmpty())
                } else {
                    val targetCells = step.targets.flatMap { it.cells(regions) }.toSet()
                    assertTrue(entry.id, targetCells.containsAll(hint.openCells))
                    assertTrue(entry.id, targetCells.containsAll(step.cells))
                    assertTrue(entry.id, step.cells.none { it in hint.openCells })
                }
            }
        }
    }
}
