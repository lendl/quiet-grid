package com.quietgrid.app.games.killersudoku

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
import com.quietgrid.app.games.sudoku.SudokuInputMode
import com.quietgrid.app.games.sudoku.SudokuSession
import com.quietgrid.app.games.sudoku.SudokuUnitKey
import com.quietgrid.app.games.sudoku.SudokuUnitState
import com.quietgrid.app.games.sudoku.applySudokuClearCell
import com.quietgrid.app.games.sudoku.applySudokuFinalizeValidation
import com.quietgrid.app.games.sudoku.applySudokuSetDigit
import com.quietgrid.app.games.sudoku.applySudokuToggleNote
import com.quietgrid.app.games.sudoku.getCompletedSudokuUnitState
import com.quietgrid.app.games.sudoku.isSudokuSolved
import com.quietgrid.app.games.sudoku.sudokuAccuracyPct
import com.quietgrid.app.games.sudoku.sudokuHasMeaningfulProgress
import com.quietgrid.app.games.sudoku.sudokuScore
import com.quietgrid.app.games.sudoku.sudokuTouchedUnitKeys
import com.quietgrid.app.session.PuzzleAdapter
import com.quietgrid.app.session.PuzzleOutcome
import com.quietgrid.app.session.PuzzleSessionController
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

data class KillerSudokuFeedbackEvent(val id: Int, val correct: Boolean, val incorrect: Boolean)

data class KillerSudokuResult(
    val difficulty: Difficulty,
    val solved: Boolean,
    val score: Int,
    val accuracyPct: Int,
    val elapsedSeconds: Int,
    val lossReason: String?,
    val isFirstSolve: Boolean = false,
    val isNewHighScore: Boolean = false,
)

private class KillerSudokuPuzzleAdapter(
    private val appContext: Context,
    private val historyStore: PlayHistoryStore,
) : PuzzleAdapter<KillerSudokuSession, KillerSudokuResult> {
    override val gameId: GameId = GameId.KILLERSUDOKU

    override suspend fun freshSession(difficulty: Difficulty): KillerSudokuSession? {
        val recentIds = historyStore.recentlyPlayedPuzzleIds(gameId, difficulty)
        val entry = KillerSudokuPuzzleBank.randomPuzzle(appContext, difficulty, recentIds) ?: return null
        return createKillerSudokuSession(entry)
    }

    override fun restoreSession(payload: String, elapsedSeconds: Double): KillerSudokuSession? {
        val persisted = runCatching { json.decodeFromString<KillerSudokuPersistedSession>(payload) }.getOrNull() ?: return null
        return KillerSudokuSession(
            puzzle = persisted.puzzle,
            grid = SudokuSession(
                puzzle = killerGridEntry(persisted.puzzle),
                board = List(9) { r -> List(9) { c -> persisted.board[r * 9 + c] } },
                notes = List(9) { r -> List(9) { c -> persisted.notes[r * 9 + c].toSet() } },
                inputMode = persisted.inputMode,
                accuracyDrops = persisted.accuracyDrops,
                finishedCells = List(9) { r -> List(9) { c -> persisted.finishedCells[r * 9 + c] } },
                penalizedUnitKeys = persisted.penalizedUnitKeys,
                autoCandidateMode = persisted.autoCandidateMode,
            ),
        )
    }

    override fun difficultyOf(session: KillerSudokuSession): Difficulty = Difficulty.fromKey(session.puzzle.difficulty)

    override fun hasMeaningfulProgress(session: KillerSudokuSession): Boolean = sudokuHasMeaningfulProgress(session.grid)

    override fun encode(session: KillerSudokuSession): String = json.encodeToString(
        KillerSudokuPersistedSession(
            puzzle = session.puzzle,
            board = session.grid.board.flatten(),
            notes = session.grid.notes.flatten().map { it.toList() },
            inputMode = session.grid.inputMode,
            accuracyDrops = session.grid.accuracyDrops,
            finishedCells = session.grid.finishedCells.flatten(),
            penalizedUnitKeys = session.grid.penalizedUnitKeys,
            autoCandidateMode = session.grid.autoCandidateMode,
        ),
    )

    override fun puzzleIdOf(session: KillerSudokuSession): String? = session.puzzle.id

    override fun scoreOnWin(session: KillerSudokuSession, difficulty: Difficulty, elapsedSeconds: Int): Int =
        sudokuScore(difficulty, elapsedSeconds, session.grid.accuracyDrops)

    override fun buildResult(session: KillerSudokuSession?, outcome: PuzzleOutcome): KillerSudokuResult = KillerSudokuResult(
        difficulty = outcome.difficulty,
        solved = outcome.solved,
        score = outcome.score,
        accuracyPct = if (outcome.solved) sudokuAccuracyPct(session?.grid?.accuracyDrops ?: 0) else 0,
        elapsedSeconds = outcome.elapsedSeconds,
        lossReason = outcome.lossReason,
        isFirstSolve = outcome.isFirstSolve,
        isNewHighScore = outcome.isNewHighScore,
    )
}

@HiltViewModel(assistedFactory = KillerSudokuPlayViewModel.Factory::class)
class KillerSudokuPlayViewModel @AssistedInject constructor(
    @ApplicationContext appContext: Context,
    sessionRepository: SessionStore,
    statsRepository: StatsStore,
    historyRepository: PlayHistoryStore,
    @Assisted requestedDifficulty: Difficulty,
    @Assisted resume: Boolean,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(requestedDifficulty: Difficulty, resume: Boolean): KillerSudokuPlayViewModel
    }

    private val controller = PuzzleSessionController(
        scope = viewModelScope,
        sessionStore = sessionRepository,
        statsStore = statsRepository,
        historyStore = historyRepository,
        adapter = KillerSudokuPuzzleAdapter(appContext, historyRepository),
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
    var feedbackEvent by mutableStateOf<KillerSudokuFeedbackEvent?>(null)
        private set
    var hint by mutableStateOf<KillerSudokuHint?>(null)
        private set
    var hintActive by mutableStateOf(false)
        private set
    var isComputingHint by mutableStateOf(false)
        private set
    var canUndo by mutableStateOf(false)
        private set

    internal var hintDispatcher: CoroutineDispatcher = Dispatchers.Default
    private var hintJob: Job? = null

    private var pendingUnitKeys = mutableSetOf<SudokuUnitKey>()
    private var pendingBoard: List<List<Int?>>? = null
    private var validationJob: Job? = null
    private var feedbackEventSeq = 0
    private val undoHistory = mutableListOf<KillerSudokuSession>()

    init {
        controller.start(requestedDifficulty, resume, null)
    }

    private fun clearHint() {
        hintJob?.cancel()
        hintJob = null
        isComputingHint = false
        hintActive = false
        hint = null
    }

    private fun clearFeedback() {
        feedbackCorrectRows = emptySet()
        feedbackCorrectCols = emptySet()
        feedbackCorrectBoxes = emptySet()
        feedbackIncorrectRows = emptySet()
        feedbackIncorrectCols = emptySet()
        feedbackIncorrectBoxes = emptySet()
    }

    fun onCellPress(row: Int, col: Int) {
        val current = session ?: return
        if (current.grid.finishedCells[row][col]) {
            selectedCell = null
            return
        }
        selectedCell = row to col
    }

    fun onToggleNoteMode() {
        val current = session ?: return
        if (current.grid.autoCandidateMode) return
        val nextMode = if (current.grid.inputMode == SudokuInputMode.DIGIT) SudokuInputMode.NOTES else SudokuInputMode.DIGIT
        controller.updateSession(current.copy(grid = current.grid.copy(inputMode = nextMode)), persist = false)
    }

    fun onToggleAutoCandidateMode() {
        val current = session ?: return
        val enabling = !current.grid.autoCandidateMode
        val grid = if (enabling) {
            current.grid.copy(autoCandidateMode = true, notes = computeKillerAutoNotes(current.layout, current.grid.board), inputMode = SudokuInputMode.DIGIT)
        } else {
            current.grid.copy(autoCandidateMode = false, notes = current.grid.notes.map { row -> row.map { emptySet<Int>() } })
        }
        controller.updateSession(current.copy(grid = grid))
    }

    fun onPressDigit(digit: Int) {
        val current = session ?: return
        val (row, col) = selectedCell ?: return
        val grid = current.grid
        val nextGrid: SudokuSession = when {
            grid.inputMode == SudokuInputMode.NOTES -> applySudokuToggleNote(grid, row, col, digit)
            grid.board[row][col] == digit -> applySudokuClearCell(grid, row, col)
            else -> applySudokuSetDigit(grid, row, col, digit)
        } ?: return
        pushUndoSnapshot(current)
        finishBoardMutation(current, current.copy(grid = nextGrid), row, col)
    }

    fun onClearCell() {
        val current = session ?: return
        val (row, col) = selectedCell ?: return
        val nextGrid = applySudokuClearCell(current.grid, row, col) ?: return
        pushUndoSnapshot(current)
        finishBoardMutation(current, current.copy(grid = nextGrid), row, col)
    }

    fun onUndo() {
        val previous = undoHistory.removeLastOrNull() ?: return
        canUndo = undoHistory.isNotEmpty()
        validationJob?.cancel()
        pendingUnitKeys = mutableSetOf()
        pendingBoard = null
        clearHint()
        clearFeedback()
        controller.updateSession(previous)
    }

    private fun pushUndoSnapshot(snapshot: KillerSudokuSession) {
        undoHistory.add(snapshot)
        if (undoHistory.size > MAX_UNDO_HISTORY) undoHistory.removeAt(0)
        canUndo = true
    }

    private fun finishBoardMutation(previous: KillerSudokuSession, updated: KillerSudokuSession, row: Int, col: Int) {
        clearHint()

        val finalUpdated = if (updated.grid.autoCandidateMode) {
            updated.copy(grid = updated.grid.copy(notes = computeKillerAutoNotes(updated.layout, updated.grid.board)))
        } else {
            updated
        }
        controller.updateSession(finalUpdated)

        val solution = previous.puzzle.solution
        for (key in sudokuTouchedUnitKeys(row, col)) {
            val before = getCompletedSudokuUnitState(previous.grid.board, solution, key)
            val after = getCompletedSudokuUnitState(finalUpdated.grid.board, solution, key)
            if (before != SudokuUnitState.INCOMPLETE || after != SudokuUnitState.INCOMPLETE) pendingUnitKeys.add(key)
        }
        if (pendingUnitKeys.isEmpty()) return
        pendingBoard = finalUpdated.grid.board

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

        val result = applySudokuFinalizeValidation(current.grid, board, unitKeys)
        controller.updateSession(current.copy(grid = result.session))

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
            feedbackEvent = KillerSudokuFeedbackEvent(feedbackEventSeq, correct, incorrect)
            viewModelScope.launch {
                delay(500)
                clearFeedback()
            }
        }

        if (isSudokuSolved(result.session.board, result.session.puzzle.solution)) {
            controller.finishAsWin()
        }
    }

    fun endPuzzle() = controller.endPuzzle()

    fun toggleHint() {
        if (controller.isFinalized || isComputingHint) return
        if (hintActive) {
            clearHint()
            return
        }
        val current = session ?: return
        hintActive = true
        isComputingHint = true
        hintJob = viewModelScope.launch {
            val computed = withContext(hintDispatcher) { getKillerSudokuHint(current.layout, current.grid.board) }
            isComputingHint = false
            if (!controller.isFinalized && session == current && hintActive) hint = computed
        }
    }
}
