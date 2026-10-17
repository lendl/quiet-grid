package com.quietgrid.app.games.sudoku

import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.ui.components.QuickRule
import com.quietgrid.app.ui.components.QuickStartContent

fun sudokuDifficultyLabelRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.sudoku_difficulty_easy
    Difficulty.MEDIUM -> R.string.sudoku_difficulty_medium
    Difficulty.HARD -> R.string.sudoku_difficulty_hard
    Difficulty.EXPERT -> R.string.sudoku_difficulty_expert
}

fun sudokuDifficultyDescriptionRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.sudoku_difficulty_desc_easy
    Difficulty.MEDIUM -> R.string.sudoku_difficulty_desc_medium
    Difficulty.HARD -> R.string.sudoku_difficulty_desc_hard
    Difficulty.EXPERT -> R.string.sudoku_difficulty_desc_expert
}

val SudokuQuickStart = QuickStartContent(
    goalRes = R.string.sudoku_quickstart_goal,
    bulletRes = listOf(R.string.sudoku_quickstart_bullet_1, R.string.sudoku_quickstart_bullet_2),
    hookRes = R.string.sudoku_quickstart_hook,
    rules = listOf(
        QuickRule(R.string.sudoku_quickrule_row) { SudokuRowRulePicture(first = 3, middle = 8) },
        QuickRule(R.string.sudoku_quickrule_column) { SudokuColumnRulePicture(first = 6, middle = 1) },
        QuickRule(R.string.sudoku_quickrule_box) { SudokuBoxRulePicture(boxRows = 3, boxCols = 3, digit = 5) },
    ),
)
