package com.quietgrid.app.games.sudoku

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietgrid.app.core.ChallengerPuzzleSolve
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.PlayHistoryStore
import com.quietgrid.app.data.StatsStore
import com.quietgrid.app.session.AppForeground
import com.quietgrid.app.session.ChallengerRunController
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SudokuChallengerViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    statsStore: StatsStore,
    historyStore: PlayHistoryStore,
    appForeground: AppForeground,
) : ViewModel() {

    private val controller = ChallengerRunController(
        scope = viewModelScope,
        gameId = GameId.SUDOKU,
        statsStore = statsStore,
        historyStore = historyStore,
        appForeground = appForeground,
        tick = ::tickSudokuChallenger,
    )

    val session get() = controller.session
    val result = controller.result

    var wrongMoveTrigger by mutableStateOf(0)
        private set

    init {
        viewModelScope.launch {
            val firstPuzzle = SudokuPuzzleBank.randomPuzzle(appContext, Difficulty.EASY)
            if (firstPuzzle != null) {
                controller.start(createInitialSudokuChallengerSession(firstPuzzle))
            }
        }
    }

    fun onDigit(row: Int, col: Int, digit: Int) {
        if (controller.isFinalized) return
        val current = session ?: return
        val updated = applySudokuSetDigit(current.puzzleSession, row, col, digit) ?: return
        applyValidationAndAdvance(current, updated, row, col)
    }

    fun onErase(row: Int, col: Int) {
        if (controller.isFinalized) return
        val current = session ?: return
        val updated = applySudokuClearCell(current.puzzleSession, row, col) ?: return
        controller.session = current.copy(puzzleSession = updated)
    }

    private fun applyValidationAndAdvance(current: SudokuChallengerSession, updatedPuzzleSession: SudokuSession, row: Int, col: Int) {
        val unitKeys = listOf("r$row", "c$col", "b${sudokuBoxIndex(row, col)}")
        val result = applySudokuFinalizeValidation(updatedPuzzleSession, updatedPuzzleSession.board, unitKeys)
        val newPenalties = result.session.accuracyDrops - updatedPuzzleSession.accuracyDrops
        val livesRemaining = current.livesRemaining - newPenalties
        if (newPenalties > 0) wrongMoveTrigger++

        val withValidation = current.copy(puzzleSession = result.session, livesRemaining = livesRemaining)
        controller.session = withValidation

        when {
            livesRemaining <= 0 -> controller.finalizeRun(withValidation, "lives_exhausted")
            isSudokuSolved(result.session.board, result.session.puzzle.solution) -> onSolved(withValidation)
            else -> Unit
        }
    }

    private fun onSolved(withValidation: SudokuChallengerSession) {
        val (nextTier, nextSolvesInTier) = sudokuChallengerTierAfterSolve(withValidation.tier, withValidation.solvesInTier)
        viewModelScope.launch {
            val nextPuzzle = SudokuPuzzleBank.randomPuzzle(appContext, nextTier, withValidation.servedPuzzleIds)
            if (nextPuzzle == null) {
                val creditedScore = withValidation.score + sudokuScore(withValidation.tier, withValidation.secondsOnCurrentPuzzle.toInt(), withValidation.puzzleSession.accuracyDrops)
                val credited = withValidation.copy(
                    puzzlesSolved = withValidation.puzzlesSolved + 1,
                    score = creditedScore,
                    fastestSolveSeconds = sudokuChallengerFastestSolve(withValidation),
                    puzzleHistory = withValidation.puzzleHistory + ChallengerPuzzleSolve(withValidation.tier, withValidation.secondsOnCurrentPuzzle),
                )
                controller.finalizeRun(credited, "bank_exhausted")
            } else {
                controller.session = advanceSudokuChallengerAfterSolve(withValidation, nextTier, nextSolvesInTier, nextPuzzle)
            }
        }
    }

    fun endRun() = controller.endRun()
}
