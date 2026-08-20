package com.quietgrid.engine.sudoku

data class SudokuCellRef(val row: Int, val col: Int)
data class SudokuHouseRef(val kind: String, val index: Int)

sealed class SudokuCanonicalMove {
    abstract val technique: SudokuTechnique
    abstract val complexity: Int
    abstract val evidenceCells: List<SudokuCellRef>
    abstract val houses: List<SudokuHouseRef>
}

data class SudokuPlacementMove(
    override val technique: SudokuTechnique,
    override val complexity: Int,
    val targetRow: Int,
    val targetCol: Int,
    val digit: Int,
    override val evidenceCells: List<SudokuCellRef>,
    override val houses: List<SudokuHouseRef>,
) : SudokuCanonicalMove()

data class SudokuCandidateEliminationMove(
    override val technique: SudokuTechnique,
    override val complexity: Int,
    val eliminations: List<Triple<Int, Int, Int>>,
    override val evidenceCells: List<SudokuCellRef>,
    override val houses: List<SudokuHouseRef>,
) : SudokuCanonicalMove()

fun countMoveTargets(move: SudokuCanonicalMove): Int = when (move) {
    is SudokuPlacementMove -> 1
    is SudokuCandidateEliminationMove -> move.eliminations.size
}
