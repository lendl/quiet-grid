package com.quietgrid.engine.sudoku.techniques

import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.sudoku.*

object SwordfishTechnique : SudokuTechniqueDispatcher {
    override val technique = SudokuTechnique.SWORDFISH
    override val tier = Difficulty.HARD

    override fun findMove(state: SudokuBitmaskState): SudokuCanonicalMove? {
        val layout = state.layout
        val size = layout.size
        var best: SudokuCanonicalMove? = null
        var bestComplexity = Int.MAX_VALUE

        for (digit in layout.digits) {
            val rowCandidates = layout.rowCellIndexes.mapIndexed { row, cells -> row to getHouseDigitMatches(state, cells, digit).map { it % size } }
                .filter { it.second.size in 2..3 }
            val rowComplexity = rowCandidates.size
            if (rowComplexity < bestComplexity) {
                outer@ for (first in rowCandidates.indices) {
                    for (second in (first + 1) until rowCandidates.size) {
                        for (third in (second + 1) until rowCandidates.size) {
                            val fish = listOf(rowCandidates[first], rowCandidates[second], rowCandidates[third])
                            val cols = fish.flatMap { it.second }.toSortedSet().toList()
                            if (cols.size != 3) continue
                            val eliminations = cols.flatMap { col ->
                                layout.columnCellIndexes[col].filter { index -> fish.none { it.first == index / size } }
                                    .filter { state.board[it] == 0 && (state.candidateMask[it] and digitToBit[digit]) != 0 }
                                    .map { it to digit }
                            }
                            val move = buildCandidateEliminationMove(
                                SudokuTechnique.SWORDFISH, eliminations,
                                fish.flatMap { entry -> entry.second.map { col -> entry.first * size + col } },
                                fish.map { SudokuHouseRef("row", it.first) } + cols.map { SudokuHouseRef("column", it) },
                                rowComplexity, layout,
                            )
                            if (move != null) { best = move; bestComplexity = rowComplexity; break@outer }
                        }
                    }
                }
            }

            val columnCandidates = layout.columnCellIndexes.mapIndexed { col, cells -> col to getHouseDigitMatches(state, cells, digit).map { it / size } }
                .filter { it.second.size in 2..3 }
            val colComplexity = columnCandidates.size
            if (colComplexity < bestComplexity) {
                outer@ for (first in columnCandidates.indices) {
                    for (second in (first + 1) until columnCandidates.size) {
                        for (third in (second + 1) until columnCandidates.size) {
                            val fish = listOf(columnCandidates[first], columnCandidates[second], columnCandidates[third])
                            val rows = fish.flatMap { it.second }.toSortedSet().toList()
                            if (rows.size != 3) continue
                            val eliminations = rows.flatMap { row ->
                                layout.rowCellIndexes[row].filter { index -> fish.none { it.first == index % size } }
                                    .filter { state.board[it] == 0 && (state.candidateMask[it] and digitToBit[digit]) != 0 }
                                    .map { it to digit }
                            }
                            val move = buildCandidateEliminationMove(
                                SudokuTechnique.SWORDFISH, eliminations,
                                fish.flatMap { entry -> entry.second.map { row -> row * size + entry.first } },
                                fish.map { SudokuHouseRef("column", it.first) } + rows.map { SudokuHouseRef("row", it) },
                                colComplexity, layout,
                            )
                            if (move != null) { best = move; bestComplexity = colComplexity; break@outer }
                        }
                    }
                }
            }
        }
        return best
    }
}
