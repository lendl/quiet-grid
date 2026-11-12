package com.quietgrid.app.games.sudoku

import com.quietgrid.app.session.ChallengerConfig
import com.quietgrid.app.session.ChallengerSession

const val SUDOKU_CHALLENGER_STARTING_LIVES = 3
const val SUDOKU_CHALLENGER_STARTING_SECONDS = 160.0
const val SUDOKU_CHALLENGER_BONUS_SECONDS = 50.0
const val SUDOKU_CHALLENGER_SOLVES_PER_TIER = 3

val SUDOKU_CHALLENGER_CONFIG = ChallengerConfig(
    startingSeconds = SUDOKU_CHALLENGER_STARTING_SECONDS,
    bonusSeconds = SUDOKU_CHALLENGER_BONUS_SECONDS,
    solvesPerTier = SUDOKU_CHALLENGER_SOLVES_PER_TIER,
    startingLives = SUDOKU_CHALLENGER_STARTING_LIVES,
)

typealias SudokuChallengerSession = ChallengerSession<SudokuSession>
