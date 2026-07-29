package com.quietgrid.engine.sudoku.techniques

import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.sudoku.*

private val HOUSE_SCAN_WEIGHTS = mapOf("row" to 1.2, "column" to 1.4, "box" to 1.0)

object HiddenSingleTechnique : SudokuTechniqueDispatcher {
    override val technique = SudokuTechnique.HIDDEN_SINGLE
    override val tier = Difficulty.EASY

    override fun findMove(state: SudokuBitmaskState): SudokuCanonicalMove? {
        var best: SudokuPlacementMove? = null
        var bestComplexity = Double.MAX_VALUE

        for (ref in allHouseRefs) {
            val emptyCells = ref.cells.count { state.board[it] == 0 }
            val houseComplexity = emptyCells * HOUSE_SCAN_WEIGHTS.getValue(ref.house.kind)
            if (houseComplexity >= bestComplexity) continue

            for (digit in 1..9) {
                val matches = getHouseDigitMatches(state, ref.cells, digit)
                if (matches.size != 1) continue

                bestComplexity = houseComplexity
                val targetIndex = matches[0]
                val row = targetIndex / 9
                val col = targetIndex % 9
                val extraHouses = listOf(
                    SudokuHouseRef("row", row),
                    SudokuHouseRef("column", col),
                    SudokuHouseRef("box", row / 3 * 3 + col / 3),
                ).filter { !(it.kind == ref.house.kind && it.index == ref.house.index) }
                best = buildPlacementMove(
                    technique = SudokuTechnique.HIDDEN_SINGLE,
                    row = row, col = col, digit = digit,
                    evidenceCells = ref.cells,
                    houses = listOf(ref.house) + extraHouses,
                    complexity = houseComplexity.toInt(),
                )
                break
            }
        }
        return best
    }
}
