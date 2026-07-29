package com.quietgrid.engine.sudoku.techniques

import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.sudoku.*

object SwordfishTechnique : SudokuTechniqueDispatcher {
    override val technique = SudokuTechnique.SWORDFISH
    override val tier = Difficulty.HARD

    override fun findMove(state: SudokuBitmaskState): SudokuCanonicalMove? {
        var best: SudokuCanonicalMove? = null
        var bestComplexity = Int.MAX_VALUE

        for (digit in 1..9) {
            val rowCandidates = rowCellIndexes.mapIndexed { row, cells -> row to getHouseDigitMatches(state, cells, digit).map { it % 9 } }
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
                                columnCellIndexes[col].filter { index -> fish.none { it.first == index / 9 } }
                                    .filter { state.board[it] == 0 && (state.candidateMask[it] and digitToBit[digit]) != 0 }
                                    .map { it to digit }
                            }
                            val move = buildCandidateEliminationMove(
                                SudokuTechnique.SWORDFISH, eliminations,
                                fish.flatMap { entry -> entry.second.map { col -> entry.first * 9 + col } },
                                fish.map { SudokuHouseRef("row", it.first) } + cols.map { SudokuHouseRef("column", it) },
                                rowComplexity,
                            )
                            if (move != null) { best = move; bestComplexity = rowComplexity; break@outer }
                        }
                    }
                }
            }

            val columnCandidates = columnCellIndexes.mapIndexed { col, cells -> col to getHouseDigitMatches(state, cells, digit).map { it / 9 } }
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
                                rowCellIndexes[row].filter { index -> fish.none { it.first == index % 9 } }
                                    .filter { state.board[it] == 0 && (state.candidateMask[it] and digitToBit[digit]) != 0 }
                                    .map { it to digit }
                            }
                            val move = buildCandidateEliminationMove(
                                SudokuTechnique.SWORDFISH, eliminations,
                                fish.flatMap { entry -> entry.second.map { row -> row * 9 + entry.first } },
                                fish.map { SudokuHouseRef("column", it.first) } + rows.map { SudokuHouseRef("row", it) },
                                colComplexity,
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
