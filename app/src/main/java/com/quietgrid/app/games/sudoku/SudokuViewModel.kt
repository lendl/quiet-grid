package com.quietgrid.app.games.sudoku

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.PlayHistoryStore
import com.quietgrid.app.data.SessionStore
import com.quietgrid.app.data.StatsStore
import com.quietgrid.app.data.recentlyPlayedPuzzleIds
import com.quietgrid.app.session.PuzzleAdapter
import com.quietgrid.app.session.PuzzleOutcome
import com.quietgrid.app.session.PuzzleSessionController
import com.quietgrid.engine.sudoku.SudokuGrid
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }
private const val VALIDATION_DELAY_MS = 800L
private const val MAX_UNDO_HISTORY = 50

data class SudokuFeedbackEvent(val id: Int, val correct: Boolean, val incorrect: Boolean)

data class SudokuResult(
    val difficulty: Difficulty,
    val solved: Boolean,
    val score: Int,
    val accuracyPct: Int,
    val elapsedSeconds: Int,
    val lossReason: String?,
    val isFirstSolve: Boolean = false,
    val isNewHighScore: Boolean = false,
)

private class SudokuPuzzleAdapter(
    private val appContext: Context,
    private val historyStore: PlayHistoryStore,
) : PuzzleAdapter<SudokuSession, SudokuResult> {
    override val gameId: GameId = GameId.SUDOKU

    override suspend fun freshSession(difficulty: Difficulty): SudokuSession? {
        val recentIds = historyStore.recentlyPlayedPuzzleIds(gameId, difficulty)
        val entry = SudokuPuzzleBank.randomPuzzle(appContext, difficulty, recentIds) ?: return null
        return createSudokuSession(entry)
    }

    override fun restoreSession(payload: String, elapsedSeconds: Double): SudokuSession? {
        val persisted = runCatching { json.decodeFromString<SudokuPersistedSession>(payload) }.getOrNull() ?: return null
        return SudokuSession(
            puzzle = persisted.puzzle,
            board = List(9) { r -> List(9) { c -> persisted.board[r * 9 + c] } },
            notes = List(9) { r -> List(9) { c -> persisted.notes[r * 9 + c].toSet() } },
            inputMode = persisted.inputMode,
            accuracyDrops = persisted.accuracyDrops,
            finishedCells = List(9) { r -> List(9) { c -> persisted.finishedCells[r * 9 + c] } },
            penalizedUnitKeys = persisted.penalizedUnitKeys,
            autoCandidateMode = persisted.autoCandidateMode,
        )
    }

    override fun difficultyOf(session: SudokuSession): Difficulty = Difficulty.fromKey(session.puzzle.difficulty)

    override fun hasMeaningfulProgress(session: SudokuSession): Boolean = sudokuHasMeaningfulProgress(session)

    override fun encode(session: SudokuSession): String = json.encodeToString(
        SudokuPersistedSession(
            puzzle = session.puzzle,
            board = session.board.flatten(),
            notes = session.notes.flatten().map { it.toList() },
            inputMode = session.inputMode,
            accuracyDrops = session.accuracyDrops,
            finishedCells = session.finishedCells.flatten(),
            penalizedUnitKeys = session.penalizedUnitKeys,
            autoCandidateMode = session.autoCandidateMode,
        ),
    )

    override fun puzzleIdOf(session: SudokuSession): String? = session.puzzle.id

    override fun scoreOnWin(session: SudokuSession, difficulty: Difficulty, elapsedSeconds: Int): Int =
        sudokuScore(difficulty, elapsedSeconds, session.accuracyDrops)

    override fun buildResult(session: SudokuSession?, outcome: PuzzleOutcome): SudokuResult = SudokuResult(
        difficulty = outcome.difficulty,
        solved = outcome.solved,
        score = outcome.score,
        accuracyPct = if (outcome.solved) sudokuAccuracyPct(session?.accuracyDrops ?: 0) else 0,
        elapsedSeconds = outcome.elapsedSeconds,
        lossReason = outcome.lossReason,
        isFirstSolve = outcome.isFirstSolve,
        isNewHighScore = outcome.isNewHighScore,
    )
}

@HiltViewModel(assistedFactory = SudokuPlayViewModel.Factory::class)
class SudokuPlayViewModel @AssistedInject constructor(
    @ApplicationContext appContext: Context,
    sessionRepository: SessionStore,
    statsRepository: StatsStore,
    historyRepository: PlayHistoryStore,
    @Assisted requestedDifficulty: Difficulty,
    @Assisted resume: Boolean,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(requestedDifficulty: Difficulty, resume: Boolean): SudokuPlayViewModel
    }

    private val controller = PuzzleSessionController(
        scope = viewModelScope,
        sessionStore = sessionRepository,
        statsStore = statsRepository,
        historyStore = historyRepository,
        adapter = SudokuPuzzleAdapter(appContext, historyRepository),
    )

    val session get() = controller.session
    val elapsedSeconds get() = controller.elapsedSeconds
    val result = controller.result

    var selectedCell by mutableStateOf<Pair<Int, Int>?>(null)
        private set
    var feedbackCorrectRows by mutableStateOf<Set<Int>>(emptySet())
        private set
    var feedbackCorrectCols by mutableStateOf<Set<Int>>(emptySet())
        private set
    var feedbackCorrectBoxes by mutableStateOf<Set<Int>>(emptySet())
        private set
    var feedbackIncorrectRows by mutableStateOf<Set<Int>>(emptySet())
        private set
    var feedbackIncorrectCols by mutableStateOf<Set<Int>>(emptySet())
        private set
    var feedbackIncorrectBoxes by mutableStateOf<Set<Int>>(emptySet())
        private set
    var feedbackEvent by mutableStateOf<SudokuFeedbackEvent?>(null)
        private set
    var nextMoveHint by mutableStateOf<SudokuNextMoveHint?>(null)
        private set
    var nextMoveHintActive by mutableStateOf(false)
        private set
    var isComputingHint by mutableStateOf(false)
        private set
    var canUndo by mutableStateOf(false)
        private set

    internal var hintDispatcher: CoroutineDispatcher = Dispatchers.Default
    private var hintJob: Job? = null

    private var pendingUnitKeys = mutableSetOf<SudokuUnitKey>()
    private var pendingBoard: SudokuGrid? = null
    private var validationJob: kotlinx.coroutines.Job? = null
    private var feedbackEventSeq = 0
    private val undoHistory = mutableListOf<SudokuSession>()

    init {
        controller.start(requestedDifficulty, resume)
    }

    private fun clearHint() {
        hintJob?.cancel()
        hintJob = null
        isComputingHint = false
        nextMoveHintActive = false
        nextMoveHint = null
    }

    fun onCellPress(row: Int, col: Int) {
        val current = session ?: return
        if (current.puzzle.givens[row][col] != null || current.finishedCells[row][col]) {
            selectedCell = null
            return
        }
        selectedCell = row to col
    }

    fun onToggleNoteMode() {
        val current = session ?: return
        if (current.autoCandidateMode) return
        controller.updateSession(
            current.copy(inputMode = if (current.inputMode == SudokuInputMode.DIGIT) SudokuInputMode.NOTES else SudokuInputMode.DIGIT),
            persist = false,
        )
    }

    fun onToggleAutoCandidateMode() {
        val current = session ?: return
        val enabling = !current.autoCandidateMode
        var updated = current.copy(autoCandidateMode = enabling)
        updated = if (enabling) {
            updated.copy(notes = computeSudokuAutoNotes(updated.board), inputMode = SudokuInputMode.DIGIT)
        } else {
            updated.copy(notes = current.notes.map { row -> row.map { emptySet<Int>() } })
        }
        controller.updateSession(updated)
    }

    fun onPressDigit(digit: Int) {
        val current = session ?: return
        val (row, col) = selectedCell ?: return

        val nextSession: SudokuSession? = if (current.inputMode == SudokuInputMode.NOTES) {
            applySudokuToggleNote(current, row, col, digit)
        } else if (current.board[row][col] == digit) {
            applySudokuClearCell(current, row, col)
        } else {
            applySudokuSetDigit(current, row, col, digit)
        }
        val updated: SudokuSession = nextSession ?: return
        pushUndoSnapshot(current)
        finishBoardMutation(current, updated, row, col)
    }

    fun onClearCell() {
        val current = session ?: return
        val (row, col) = selectedCell ?: return
        val updated = applySudokuClearCell(current, row, col) ?: return
        pushUndoSnapshot(current)
        finishBoardMutation(current, updated, row, col)
    }

    fun onUndo() {
        val previous = undoHistory.removeLastOrNull() ?: return
        canUndo = undoHistory.isNotEmpty()

        validationJob?.cancel()
        pendingUnitKeys = mutableSetOf()
        pendingBoard = null
        clearHint()
        feedbackCorrectRows = emptySet()
        feedbackCorrectCols = emptySet()
        feedbackCorrectBoxes = emptySet()
        feedbackIncorrectRows = emptySet()
        feedbackIncorrectCols = emptySet()
        feedbackIncorrectBoxes = emptySet()

        controller.updateSession(previous)
    }

    private fun pushUndoSnapshot(snapshot: SudokuSession) {
        undoHistory.add(snapshot)
        if (undoHistory.size > MAX_UNDO_HISTORY) undoHistory.removeAt(0)
        canUndo = true
    }

    private fun finishBoardMutation(previous: SudokuSession, updated: SudokuSession, row: Int, col: Int) {
        clearHint()

        val finalUpdated = if (updated.autoCandidateMode) updated.copy(notes = computeSudokuAutoNotes(updated.board)) else updated
        controller.updateSession(finalUpdated)

        for (key in sudokuTouchedUnitKeys(row, col)) {
            val before = getCompletedSudokuUnitState(previous.board, previous.puzzle.solution, key)
            val after = getCompletedSudokuUnitState(finalUpdated.board, previous.puzzle.solution, key)
            if (before != SudokuUnitState.INCOMPLETE || after != SudokuUnitState.INCOMPLETE) {
                pendingUnitKeys.add(key)
            }
        }
        if (pendingUnitKeys.isEmpty()) return
        pendingBoard = finalUpdated.board

        validationJob?.cancel()
        validationJob = viewModelScope.launch {
            delay(VALIDATION_DELAY_MS)
            runFinalizeValidation()
        }
    }

    private fun runFinalizeValidation() {
        val current = session ?: return
        val board = pendingBoard ?: return
        val unitKeys = pendingUnitKeys.toList()
        pendingUnitKeys = mutableSetOf()
        pendingBoard = null

        val result = applySudokuFinalizeValidation(current, board, unitKeys)
        controller.updateSession(result.session)

        feedbackCorrectRows = result.effect.correctRowIndexes.toSet()
        feedbackCorrectCols = result.effect.correctColIndexes.toSet()
        feedbackCorrectBoxes = result.effect.correctBoxIndexes.toSet()
        feedbackIncorrectRows = result.effect.incorrectRowIndexes.toSet()
        feedbackIncorrectCols = result.effect.incorrectColIndexes.toSet()
        feedbackIncorrectBoxes = result.effect.incorrectBoxIndexes.toSet()
        val correct = feedbackCorrectRows.isNotEmpty() || feedbackCorrectCols.isNotEmpty() || feedbackCorrectBoxes.isNotEmpty()
        val incorrect = feedbackIncorrectRows.isNotEmpty() || feedbackIncorrectCols.isNotEmpty() || feedbackIncorrectBoxes.isNotEmpty()
        if (correct || incorrect) {
            feedbackEventSeq++
            feedbackEvent = SudokuFeedbackEvent(feedbackEventSeq, correct, incorrect)
        }
        if (feedbackCorrectRows.isNotEmpty() || feedbackCorrectCols.isNotEmpty() || feedbackCorrectBoxes.isNotEmpty() ||
            feedbackIncorrectRows.isNotEmpty() || feedbackIncorrectCols.isNotEmpty() || feedbackIncorrectBoxes.isNotEmpty()
        ) {
            viewModelScope.launch {
                delay(500)
                feedbackCorrectRows = emptySet()
                feedbackCorrectCols = emptySet()
                feedbackCorrectBoxes = emptySet()
                feedbackIncorrectRows = emptySet()
                feedbackIncorrectCols = emptySet()
                feedbackIncorrectBoxes = emptySet()
            }
        }

        if (isSudokuSolved(result.session.board, result.session.puzzle.solution)) {
            controller.finishAsWin()
        }
    }

    fun endPuzzle() = controller.endPuzzle()

    fun toggleNextMoveHint() {
        if (controller.isFinalized || isComputingHint) return
        if (nextMoveHintActive) {
            clearHint()
            return
        }
        val current = session ?: return
        nextMoveHintActive = true
        isComputingHint = true
        hintJob = viewModelScope.launch {
            val hint = withContext(hintDispatcher) { getSudokuNextMoveHint(current.board) }
            isComputingHint = false
            if (!controller.isFinalized && session == current && nextMoveHintActive) nextMoveHint = hint
        }
    }
}
