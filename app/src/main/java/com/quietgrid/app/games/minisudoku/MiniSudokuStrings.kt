package com.quietgrid.app.games.minisudoku

import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.games.sudoku.SudokuBoxRulePicture
import com.quietgrid.app.games.sudoku.SudokuColumnRulePicture
import com.quietgrid.app.games.sudoku.SudokuRowRulePicture
import com.quietgrid.app.games.sudoku.sudokuDifficultyLabelRes
import com.quietgrid.app.ui.components.QuickRule
import com.quietgrid.app.ui.components.QuickStartContent

fun miniSudokuDifficultyLabelRes(difficulty: Difficulty): Int = sudokuDifficultyLabelRes(difficulty)

fun miniSudokuDifficultyDescriptionRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.minisudoku_difficulty_desc_easy
    Difficulty.MEDIUM -> R.string.minisudoku_difficulty_desc_medium
    Difficulty.HARD -> R.string.minisudoku_difficulty_desc_hard
    Difficulty.EXPERT -> R.string.minisudoku_difficulty_desc_expert
}

val MiniSudokuQuickStart = QuickStartContent(
    goalRes = R.string.minisudoku_quickstart_goal,
    bulletRes = listOf(R.string.minisudoku_quickstart_bullet_1, R.string.minisudoku_quickstart_bullet_2),
    hookRes = R.string.minisudoku_quickstart_hook,
    rules = listOf(
        QuickRule(R.string.sudoku_quickrule_row) { SudokuRowRulePicture(first = 2, middle = 4) },
        QuickRule(R.string.sudoku_quickrule_column) { SudokuColumnRulePicture(first = 3, middle = 1) },
        QuickRule(R.string.sudoku_quickrule_box) { SudokuBoxRulePicture(boxRows = 2, boxCols = 3, digit = 5) },
    ),
)
