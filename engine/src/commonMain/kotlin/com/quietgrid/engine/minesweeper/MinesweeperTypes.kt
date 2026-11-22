package com.quietgrid.engine.minesweeper

const val MINESWEEPER_HIDDEN = -1

enum class MinesweeperTechnique { CLUE_COUNT, CLUE_PAIR, MINE_COUNT }

data class MinesweeperDeduction(
    val technique: MinesweeperTechnique,
    val clues: List<Int>,
    val safe: List<Int>,
    val mines: List<Int>,
)
