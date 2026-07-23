package com.quietgrid.app.games.sudoku

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.ActiveSessionEnvelope
import com.quietgrid.app.data.SessionRepository
import com.quietgrid.app.data.StatsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
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

class SudokuPlayViewModel(
    private val appContext: Context,
    private val sessionRepository: SessionRepository,
    private val statsRepository: StatsRepository,
    private val requestedDifficulty: Difficulty,
    private val resume: Boolean,
) : ViewModel() {

    var session by mutableStateOf<SudokuSession?>(null)
        private set
    var elapsedSeconds by mutableStateOf(0.0)
        private set
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

    private var difficulty: Difficulty = requestedDifficulty
    private var finalized = false
    private var pendingUnitKeys = mutableSetOf<SudokuUnitKey>()
    private var pendingBoard: SudokuGrid? = null
    private var validationJob: kotlinx.coroutines.Job? = null

    private val _result = MutableSharedFlow<SudokuResult>(extraBufferCapacity = 1)
    val result: SharedFlow<SudokuResult> = _result

    init {
        viewModelScope.launch {
            session = if (resume) restoreOrCreate() else freshSession(requestedDifficulty)
            runTicker()
        }
    }

    private suspend fun freshSession(difficulty: Difficulty): SudokuSession? {
        val entry = SudokuPuzzleBank.randomPuzzle(appContext, difficulty) ?: return null
        return createSudokuSession(entry)
    }

    private suspend fun restoreOrCreate(): SudokuSession? {
        val envelope = sessionRepository.activeSession.first()
        if (envelope != null && envelope.gameId == GameId.SUDOKU.key) {
            val persisted = runCatching { json.decodeFromString<SudokuPersistedSession>(envelope.payload) }.getOrNull()
            if (persisted != null) {
                elapsedSeconds = envelope.elapsedSeconds
                difficulty = Difficulty.fromKey(persisted.puzzle.difficulty)
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
        }
        return freshSession(requestedDifficulty)
    }

    private suspend fun runTicker() {
        while (true) {
            delay(1000)
            if (finalized || session == null) continue
            elapsedSeconds += 1.0
            persistIfMeaningful()
        }
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
        session = current.copy(inputMode = if (current.inputMode == SudokuInputMode.DIGIT) SudokuInputMode.NOTES else SudokuInputMode.DIGIT)
    }

    fun onPressDigit(digit: Int) {
        val current = session ?: return
        val (row, col) = selectedCell ?: return
        if (finalized) return

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
        session = updated
        persistIfMeaningful()

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
        session = result.session

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

        persistIfMeaningful()

        if (isSudokuSolved(result.session.board, result.session.puzzle.solution)) {
            finishAsWin()
        }
    }

    fun endPuzzle() {
        if (finalized) return
        finishAsLoss("abandoned")
    }

    fun toggleNextMoveHint() {
        if (finalized) return
        if (nextMoveHintActive) {
            nextMoveHintActive = false
            nextMoveHint = null
            return
        }
        val current = session ?: return
        nextMoveHintActive = true
        nextMoveHint = getSudokuNextMoveHint(current.board)
    }

    private fun finishAsWin() {
        if (finalized) return
        finalized = true
        val current = session ?: return
        val score = sudokuScore(difficulty, elapsedSeconds.toInt(), current.accuracyDrops)
        viewModelScope.launch {
            val previous = statsRepository.statsFor(GameId.SUDOKU).first().forDifficulty(difficulty)
            statsRepository.recordResult(GameId.SUDOKU, difficulty, solved = true, score = score)
            sessionRepository.clear()
            _result.emit(
                SudokuResult(
                    difficulty = difficulty,
                    solved = true,
                    score = score,
                    accuracyPct = sudokuAccuracyPct(current.accuracyDrops),
                    elapsedSeconds = elapsedSeconds.toInt(),
                    lossReason = null,
                    isFirstSolve = previous.solved == 0,
                    isNewHighScore = previous.solved > 0 && score > previous.bestScore,
                ),
            )
        }
    }

    private fun finishAsLoss(reason: String) {
        if (finalized) return
        finalized = true
        viewModelScope.launch {
            statsRepository.recordResult(GameId.SUDOKU, difficulty, solved = false, score = 0)
            sessionRepository.clear()
            _result.emit(
                SudokuResult(
                    difficulty = difficulty,
                    solved = false,
                    score = 0,
                    accuracyPct = 0,
                    elapsedSeconds = elapsedSeconds.toInt(),
                    lossReason = reason,
                ),
            )
        }
    }

    private fun persistIfMeaningful() {
        val current = session ?: return
        if (finalized) return
        if (!sudokuHasMeaningfulProgress(current)) return
        val payload = json.encodeToString(
            SudokuPersistedSession(
                puzzle = current.puzzle,
                board = current.board.flatten(),
                notes = current.notes.flatten().map { it.toList() },
                inputMode = current.inputMode,
                accuracyDrops = current.accuracyDrops,
                finishedCells = current.finishedCells.flatten(),
                penalizedUnitKeys = current.penalizedUnitKeys,
            ),
        )
        viewModelScope.launch {
            sessionRepository.save(
                ActiveSessionEnvelope(
                    gameId = GameId.SUDOKU.key,
                    elapsedSeconds = elapsedSeconds,
                    payload = payload,
                ),
            )
        }
    }
}
