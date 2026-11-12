package com.quietgrid.app.games.sudoku

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.session.advanceAfterSolve
import com.quietgrid.app.session.startChallengerSession
import com.quietgrid.engine.sudoku.SudokuPuzzleEntry

private fun sudokuChallengerNextPuzzleSession(previous: SudokuSession, nextPuzzle: SudokuPuzzleEntry): SudokuSession {
    val fresh = createSudokuSession(nextPuzzle)
    return if (previous.autoCandidateMode) applySudokuToggleAutoCandidateMode(fresh) else fresh.copy(inputMode = previous.inputMode)
}

fun createInitialSudokuChallengerSession(firstPuzzle: SudokuPuzzleEntry): SudokuChallengerSession =
    startChallengerSession(SUDOKU_CHALLENGER_CONFIG, createSudokuSession(firstPuzzle), firstPuzzle.id)

fun sudokuChallengerPuzzleScore(session: SudokuChallengerSession): Int =
    sudokuScore(session.tier, session.secondsOnCurrentPuzzle.toInt(), session.puzzleSession.accuracyDrops)

fun advanceSudokuChallengerAfterSolve(
    session: SudokuChallengerSession,
    nextTier: Difficulty,
    nextSolvesInTier: Int,
    nextPuzzle: SudokuPuzzleEntry,
): SudokuChallengerSession = session.advanceAfterSolve(
    config = SUDOKU_CHALLENGER_CONFIG,
    gainedScore = sudokuChallengerPuzzleScore(session),
    nextTier = nextTier,
    nextSolvesInTier = nextSolvesInTier,
    nextPuzzleSession = sudokuChallengerNextPuzzleSession(session.puzzleSession, nextPuzzle),
    nextPuzzleId = nextPuzzle.id,
)
