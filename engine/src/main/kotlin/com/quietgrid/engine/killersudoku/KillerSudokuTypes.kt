package com.quietgrid.engine.killersudoku

import kotlinx.serialization.Serializable

@Serializable
data class KillerSudokuCage(
    val sum: Int,
    val cells: List<Int>,
)

@Serializable
data class KillerSudokuPuzzleEntry(
    val id: String,
    val difficulty: String,
    val cages: List<KillerSudokuCage>,
    val solution: List<List<Int>>,
)

class KillerCageLayout(val cages: List<KillerSudokuCage>) {
    val cageOfCell: IntArray = IntArray(81) { -1 }.also { owner ->
        cages.forEachIndexed { cageIndex, cage ->
            cage.cells.forEach { cell ->
                check(owner[cell] == -1) { "Cell $cell belongs to more than one cage." }
                owner[cell] = cageIndex
            }
        }
        check(owner.all { it != -1 }) { "Every cell must belong to a cage." }
    }

    fun cageMates(cell: Int): List<Int> = cages[cageOfCell[cell]].cells.filter { it != cell }
}
