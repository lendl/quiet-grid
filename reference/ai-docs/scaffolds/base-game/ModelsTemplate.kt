package com.quietgrid.app.games.__gameId__

// __GameName__Models.kt template — shared data classes for the game.
// Replace __GameName__ with the PascalCase game id (e.g. Takuzu, Nonogram).

data class __GameName__Puzzle(
    val id: String,
    val difficulty: __GameName__Difficulty,
    // __PUZZLE_SHAPE__ e.g. size, clues, solution
)

enum class __GameName__Difficulty {
    EASY, MEDIUM, HARD, EXPERT,
}

data class __GameName__ActiveState(
    val puzzle: __GameName__Puzzle,
    val score: Int,
    val mistakes: Int,
    val isLoss: Boolean,
    // __BOARD_STATE__ e.g. current cell values
)
