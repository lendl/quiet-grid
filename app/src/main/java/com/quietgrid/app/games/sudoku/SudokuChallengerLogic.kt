package com.quietgrid.app.games.sudoku

import com.quietgrid.app.core.ChallengerPuzzleSolve
import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.sudoku.SudokuPuzzleEntry

private val SUDOKU_CHALLENGER_TIER_ORDER = listOf(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD, Difficulty.EXPERT)

fun createInitialSudokuChallengerSession(firstPuzzle: SudokuPuzzleEntry): SudokuChallengerSession =
    SudokuChallengerSession(
        puzzleSession = createSudokuSession(firstPuzzle),
        livesRemaining = SUDOKU_CHALLENGER_STARTING_LIVES,
        tier = Difficulty.EASY,
        solvesInTier = 0,
        puzzlesSolved = 0,
        score = 0,
        secondsRemaining = SUDOKU_CHALLENGER_STARTING_SECONDS,
        secondsOnCurrentPuzzle = 0.0,
        servedPuzzleIds = setOf(firstPuzzle.id),
    )

fun sudokuChallengerTierAfterSolve(currentTier: Difficulty, solvesInTier: Int): Pair<Difficulty, Int> {
    val nextSolves = solvesInTier + 1
    val currentIndex = SUDOKU_CHALLENGER_TIER_ORDER.indexOf(currentTier)
    return if (nextSolves >= SUDOKU_CHALLENGER_SOLVES_PER_TIER && currentIndex < SUDOKU_CHALLENGER_TIER_ORDER.lastIndex) {
        SUDOKU_CHALLENGER_TIER_ORDER[currentIndex + 1] to 0
    } else {
        currentTier to nextSolves
    }
}

fun sudokuChallengerFastestSolve(session: SudokuChallengerSession): Double =
    session.fastestSolveSeconds?.let { minOf(it, session.secondsOnCurrentPuzzle) } ?: session.secondsOnCurrentPuzzle

fun advanceSudokuChallengerAfterSolve(
    session: SudokuChallengerSession,
    nextTier: Difficulty,
    nextSolvesInTier: Int,
    nextPuzzle: SudokuPuzzleEntry,
): SudokuChallengerSession {
    val gainedScore = sudokuScore(session.tier, session.secondsOnCurrentPuzzle.toInt(), session.puzzleSession.accuracyDrops)
    val fastestSolveSeconds = sudokuChallengerFastestSolve(session)
    return session.copy(
        puzzleSession = createSudokuSession(nextPuzzle),
        tier = nextTier,
        solvesInTier = nextSolvesInTier,
        puzzlesSolved = session.puzzlesSolved + 1,
        score = session.score + gainedScore,
        secondsRemaining = session.secondsRemaining + SUDOKU_CHALLENGER_BONUS_SECONDS,
        secondsOnCurrentPuzzle = 0.0,
        servedPuzzleIds = session.servedPuzzleIds + nextPuzzle.id,
        fastestSolveSeconds = fastestSolveSeconds,
        puzzleHistory = session.puzzleHistory + ChallengerPuzzleSolve(session.tier, session.secondsOnCurrentPuzzle),
    )
}

fun tickSudokuChallenger(session: SudokuChallengerSession): SudokuChallengerSession = session.copy(
    secondsRemaining = session.secondsRemaining - 1.0,
    secondsOnCurrentPuzzle = session.secondsOnCurrentPuzzle + 1.0,
)
