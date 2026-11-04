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
import com.quietgrid.engine.sudoku.SudokuLayout
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    var selectedCell by mutableStateOf<Pair<Int, Int>?>(null)
        private set
    var canUndo by mutableStateOf(false)
        private set

    private val undoHistory = mutableListOf<SudokuSession>()
    private var advancing = false
    private var pendingUnitKeys = mutableSetOf<SudokuUnitKey>()
    private var validationJob: Job? = null

    init {
        viewModelScope.launch {
            val firstPuzzle = SudokuPuzzleBank.randomPuzzle(appContext, Difficulty.EASY)
            if (firstPuzzle != null) {
                controller.start(createInitialSudokuChallengerSession(firstPuzzle))
            }
        }
    }

    private val acceptsInput get() = !controller.isFinalized && !advancing

    fun onCellPress(row: Int, col: Int) {
        val puzzleSession = session?.puzzleSession ?: return
        selectedCell = if (puzzleSession.puzzle.givens[row][col] != null || puzzleSession.finishedCells[row][col]) null else row to col
    }

    fun onToggleNoteMode() {
        if (!acceptsInput) return
        val current = session ?: return
        val updated = applySudokuToggleNoteMode(current.puzzleSession) ?: return
        controller.session = current.copy(puzzleSession = updated)
    }

    fun onToggleAutoCandidateMode() {
        if (!acceptsInput) return
        val current = session ?: return
        controller.session = current.copy(puzzleSession = applySudokuToggleAutoCandidateMode(current.puzzleSession))
    }

    fun onPressDigit(digit: Int) {
        if (!acceptsInput) return
        val current = session ?: return
        val (row, col) = selectedCell ?: return
        val updated = applySudokuPressDigit(current.puzzleSession, row, col, digit) ?: return
        commitMove(current, updated, row, col)
    }

    fun onClearCell() {
        if (!acceptsInput) return
        val current = session ?: return
        val (row, col) = selectedCell ?: return
        val updated = applySudokuClearCell(current.puzzleSession, row, col) ?: return
        commitMove(current, updated, row, col)
    }

    fun onUndo() {
        if (!acceptsInput) return
        val current = session ?: return
        val previous = undoHistory.removeLastOrNull() ?: return
        canUndo = undoHistory.isNotEmpty()
        cancelPendingValidation()
        controller.session = current.copy(puzzleSession = previous.copy(accuracyDrops = current.puzzleSession.accuracyDrops))
    }

    private fun cancelPendingValidation() {
        validationJob?.cancel()
        validationJob = null
        pendingUnitKeys = mutableSetOf()
    }

    private fun commitMove(current: SudokuChallengerSession, updated: SudokuSession, row: Int, col: Int) {
        undoHistory.add(current.puzzleSession)
        if (undoHistory.size > MAX_UNDO_HISTORY) undoHistory.removeAt(0)
        canUndo = true
        val refreshed = refreshSudokuAutoNotes(updated)
        controller.session = current.copy(puzzleSession = refreshed)

        val solution = refreshed.puzzle.solution
        for (key in sudokuTouchedUnitKeys(row, col, SudokuLayout.forSize(refreshed.board.size))) {
            val before = getCompletedSudokuUnitState(current.puzzleSession.board, solution, key)
            val after = getCompletedSudokuUnitState(refreshed.board, solution, key)
            if (before != SudokuUnitState.INCOMPLETE || after != SudokuUnitState.INCOMPLETE) pendingUnitKeys.add(key)
        }
        if (pendingUnitKeys.isEmpty()) return

        validationJob?.cancel()
        if (isSudokuSolved(refreshed.board, solution)) {
            runFinalizeValidation()
        } else {
            validationJob = viewModelScope.launch {
                delay(SUDOKU_VALIDATION_DELAY_MS)
                runFinalizeValidation()
            }
        }
    }

    private fun runFinalizeValidation() {
        if (!acceptsInput) return
        val current = session ?: return
        val unitKeys = pendingUnitKeys.toList()
        pendingUnitKeys = mutableSetOf()

        val puzzleSession = current.puzzleSession
        val result = applySudokuFinalizeValidation(puzzleSession, puzzleSession.board, unitKeys)
        val newPenalties = result.session.accuracyDrops - puzzleSession.accuracyDrops
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
        advancing = true
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
                selectedCell = null
                undoHistory.clear()
                canUndo = false
                advancing = false
            }
        }
    }

    fun endRun() = controller.endRun()
}
