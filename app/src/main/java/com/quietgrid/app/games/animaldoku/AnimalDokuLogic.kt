// app/src/main/java/com/quietgrid/app/games/animaldoku/AnimalDokuLogic.kt
package com.quietgrid.app.games.animaldoku

import com.quietgrid.app.R
import com.quietgrid.engine.animaldoku.AnimalDokuHint
import com.quietgrid.engine.animaldoku.AnimalDokuPuzzleEntry
import com.quietgrid.engine.animaldoku.AnimalDokuStep
import com.quietgrid.engine.animaldoku.AnimalDokuTechnique
import com.quietgrid.engine.animaldoku.AnimalDokuUnitKind
import com.quietgrid.engine.animaldoku.cells
import com.quietgrid.engine.animaldoku.findAnimalDokuHint

private fun List<List<AnimalDokuCellState>>.replaceCell(row: Int, col: Int, value: AnimalDokuCellState): List<List<AnimalDokuCellState>> =
    mapIndexed { r, line -> if (r != row) line else line.mapIndexed { c, cell -> if (c != col) cell else value } }

fun applyAnimalDokuTap(session: AnimalDokuSession, row: Int, col: Int): AnimalDokuSession? {
    if (session.status != AnimalDokuStatus.PLAYING) return null
    val next = when (session.cells[row][col]) {
        AnimalDokuCellState.EMPTY -> AnimalDokuCellState.MARKED
        AnimalDokuCellState.MARKED -> AnimalDokuCellState.EMPTY
        else -> return null
    }
    return session.copy(cells = session.cells.replaceCell(row, col, next))
}

fun applyAnimalDokuDrag(session: AnimalDokuSession, markAll: Boolean, visited: List<Pair<Int, Int>>): AnimalDokuSession? {
    if (session.status != AnimalDokuStatus.PLAYING) return null

    var cells = session.cells
    for ((row, col) in visited) {
        val current = cells[row][col]
        if (current != AnimalDokuCellState.EMPTY && current != AnimalDokuCellState.MARKED) continue
        cells = cells.replaceCell(row, col, if (markAll) AnimalDokuCellState.MARKED else AnimalDokuCellState.EMPTY)
    }
    return session.copy(cells = cells)
}

data class AnimalDokuOpenResult(val session: AnimalDokuSession, val wasCorrect: Boolean)

fun applyAnimalDokuOpen(session: AnimalDokuSession, row: Int, col: Int): AnimalDokuOpenResult? {
    if (session.status != AnimalDokuStatus.PLAYING) return null
    val current = session.cells[row][col]
    if (current != AnimalDokuCellState.EMPTY && current != AnimalDokuCellState.MARKED) return null

    val isCorrect = session.puzzle.solution[row] == col
    val cells = session.cells.replaceCell(row, col, if (isCorrect) AnimalDokuCellState.LOCKED_CORRECT else AnimalDokuCellState.LOCKED_WRONG)
    val lives = if (isCorrect) session.lives else session.lives - 1

    val status = when {
        isAnimalDokuSolved(cells) -> AnimalDokuStatus.WON
        lives <= 0 -> AnimalDokuStatus.LOST
        else -> AnimalDokuStatus.PLAYING
    }

    return AnimalDokuOpenResult(session.copy(cells = cells, lives = lives, status = status), isCorrect)
}

fun isAnimalDokuSolved(cells: List<List<AnimalDokuCellState>>): Boolean =
    cells.all { line -> line.any { it == AnimalDokuCellState.LOCKED_CORRECT } }

fun withoutWrongAnimalDokuMarks(puzzle: AnimalDokuPuzzleEntry, cells: List<List<AnimalDokuCellState>>): List<List<AnimalDokuCellState>> =
    cells.mapIndexed { row, line ->
        line.mapIndexed { col, cell -> if (cell == AnimalDokuCellState.MARKED && puzzle.solution[row] == col) AnimalDokuCellState.EMPTY else cell }
    }

fun applyAnimalDokuHintToCells(cells: List<List<AnimalDokuCellState>>, hint: AnimalDokuHint): List<List<AnimalDokuCellState>> = when (hint) {
    is AnimalDokuHint.WrongMark -> cells.replaceCell(hint.row, hint.col, AnimalDokuCellState.EMPTY)
    is AnimalDokuHint.Reveal -> cells.replaceCell(hint.row, hint.col, AnimalDokuCellState.LOCKED_CORRECT)
    is AnimalDokuHint.Deduction -> when (val step = hint.step) {
        is AnimalDokuStep.Placement -> cells.replaceCell(step.row, step.col, AnimalDokuCellState.LOCKED_CORRECT)
        is AnimalDokuStep.Elimination -> step.cells.fold(cells) { current, (row, col) ->
            if (current[row][col] == AnimalDokuCellState.EMPTY) current.replaceCell(row, col, AnimalDokuCellState.MARKED) else current
        }
    }
}

fun findAnimalDokuSessionHint(session: AnimalDokuSession): AnimalDokuHint? {
    if (session.status != AnimalDokuStatus.PLAYING) return null
    return findAnimalDokuCellsHint(session.puzzle, session.cells)
}

fun findAnimalDokuCellsHint(puzzle: AnimalDokuPuzzleEntry, cells: List<List<AnimalDokuCellState>>): AnimalDokuHint? {
    val placed = mutableSetOf<Pair<Int, Int>>()
    val excluded = mutableSetOf<Pair<Int, Int>>()
    cells.forEachIndexed { row, line ->
        line.forEachIndexed { col, cell ->
            when (cell) {
                AnimalDokuCellState.LOCKED_CORRECT -> placed.add(row to col)
                AnimalDokuCellState.MARKED, AnimalDokuCellState.LOCKED_WRONG -> excluded.add(row to col)
                AnimalDokuCellState.EMPTY -> Unit
            }
        }
    }
    return findAnimalDokuHint(puzzle, placed, excluded)
}

fun animalDokuHintHighlight(hint: AnimalDokuHint, regions: List<List<Int>>): AnimalDokuHintHighlight = when (hint) {
    is AnimalDokuHint.WrongMark -> AnimalDokuHintHighlight(focusCells = setOf(hint.row to hint.col))
    is AnimalDokuHint.Reveal -> AnimalDokuHintHighlight(focusCells = setOf(hint.row to hint.col), animalCell = hint.row to hint.col)
    is AnimalDokuHint.Deduction -> when (val step = hint.step) {
        is AnimalDokuStep.Placement -> AnimalDokuHintHighlight(
            focusCells = hint.openCells + (step.row to step.col),
            unitCells = step.unit.cells(regions).toSet(),
            animalCell = step.row to step.col,
        )
        is AnimalDokuStep.Elimination -> AnimalDokuHintHighlight(
            focusCells = hint.openCells + step.cells,
            unitCells = (step.sources + step.targets).flatMap { it.cells(regions) }.toSet(),
            crossCells = step.cells.toSet(),
        )
    }
}

private fun techniqueTitleRes(technique: AnimalDokuTechnique): Int = when (technique) {
    AnimalDokuTechnique.SINGLETON -> R.string.animaldoku_technique_last_spot
    AnimalDokuTechnique.STRUCTURAL_CONFINEMENT, AnimalDokuTechnique.CONFINEMENT -> R.string.animaldoku_technique_confinement
    AnimalDokuTechnique.PAIRING_2 -> R.string.animaldoku_technique_pairing
    AnimalDokuTechnique.PAIRING_3 -> R.string.animaldoku_technique_triple
    AnimalDokuTechnique.PAIRING_4_PLUS -> R.string.animaldoku_technique_group
    AnimalDokuTechnique.CHAIN -> R.string.animaldoku_technique_chain
}

fun animalDokuHintCopy(hint: AnimalDokuHint): AnimalDokuHintCopy = when (hint) {
    is AnimalDokuHint.WrongMark -> AnimalDokuHintCopy(R.string.animaldoku_hint_wrong_mark_title, R.string.animaldoku_hint_wrong_mark_body)
    is AnimalDokuHint.Reveal -> AnimalDokuHintCopy(R.string.animaldoku_hint_reveal_title, R.string.animaldoku_hint_reveal_body)
    is AnimalDokuHint.Deduction -> {
        val titleRes = techniqueTitleRes(hint.step.technique)
        when (val step = hint.step) {
            is AnimalDokuStep.Placement -> AnimalDokuHintCopy(titleRes, R.string.animaldoku_hint_last_spot_body, listOf(listOf(step.unit)))
            is AnimalDokuStep.Elimination -> {
                val fromRegions = step.sources.first().kind == AnimalDokuUnitKind.REGION
                val bodyRes = when {
                    step.technique == AnimalDokuTechnique.CHAIN ->
                        if (step.chainDepth > 1) R.string.animaldoku_hint_chain_long_body else R.string.animaldoku_hint_chain_body
                    fromRegions && step.sources.size == 1 -> R.string.animaldoku_hint_confined_to_line_body
                    fromRegions -> R.string.animaldoku_hint_regions_fill_lines_body
                    step.sources.size == 1 -> R.string.animaldoku_hint_line_confined_to_region_body
                    else -> R.string.animaldoku_hint_lines_fill_regions_body
                }
                AnimalDokuHintCopy(titleRes, bodyRes, listOf(step.sources, step.targets).filter { it.isNotEmpty() })
            }
        }
    }
}
