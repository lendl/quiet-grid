package com.quietgrid.app.games.sudoku

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.SessionStore
import com.quietgrid.app.data.StatsStore
import com.quietgrid.app.session.PuzzleAdapter
import com.quietgrid.app.session.PuzzleOutcome
import com.quietgrid.app.session.PuzzleSessionController
import com.quietgrid.engine.sudoku.SudokuGrid
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }
private const val VALIDATION_DELAY_MS = 800L

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

private class SudokuPuzzleAdapter(private val appContext: Context) : PuzzleAdapter<SudokuSession, SudokuResult> {
    override val gameId: GameId = GameId.SUDOKU

    override suspend fun freshSession(difficulty: Difficulty): SudokuSession? {
        val entry = SudokuPuzzleBank.randomPuzzle(appContext, difficulty) ?: return null
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
        ),
    )

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
        adapter = SudokuPuzzleAdapter(appContext),
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
    var nextMoveHint by mutableStateOf<SudokuNextMoveHint?>(null)
        private set
    var nextMoveHintActive by mutableStateOf(false)
        private set

    private var pendingUnitKeys = mutableSetOf<SudokuUnitKey>()
    private var pendingBoard: SudokuGrid? = null
    private var validationJob: kotlinx.coroutines.Job? = null

    init {
        controller.start(requestedDifficulty, resume)
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
        if (selectedCell == null) return
        controller.updateSession(
            current.copy(inputMode = if (current.inputMode == SudokuInputMode.DIGIT) SudokuInputMode.NOTES else SudokuInputMode.DIGIT),
            persist = false,
        )
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

        nextMoveHint = null
        nextMoveHintActive = false
        controller.updateSession(updated)

        if (current.inputMode == SudokuInputMode.NOTES) return

        for (key in sudokuTouchedUnitKeys(row, col)) {
            val before = getCompletedSudokuUnitState(current.board, current.puzzle.solution, key)
            val after = getCompletedSudokuUnitState(updated.board, current.puzzle.solution, key)
            if (before != SudokuUnitState.INCOMPLETE || after != SudokuUnitState.INCOMPLETE) {
                pendingUnitKeys.add(key)
            }
        }
        if (pendingUnitKeys.isEmpty()) return
        pendingBoard = updated.board

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
        if (controller.isFinalized) return
        if (nextMoveHintActive) {
            nextMoveHintActive = false
            nextMoveHint = null
            return
        }
        val current = session ?: return
        nextMoveHintActive = true
        nextMoveHint = getSudokuNextMoveHint(current.board)
    }
}
