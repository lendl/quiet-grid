package com.quietgrid.app.games.nonogram

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

data class NonogramResult(
    val difficulty: Difficulty,
    val solved: Boolean,
    val score: Int,
    val elapsedSeconds: Int,
    val lossReason: String?,
    val isFirstSolve: Boolean = false,
    val isNewHighScore: Boolean = false,
)

class NonogramPlayViewModel(
    private val appContext: Context,
    private val sessionRepository: SessionRepository,
    private val statsRepository: StatsRepository,
    private val requestedDifficulty: Difficulty,
    private val resume: Boolean,
) : ViewModel() {

    var session by mutableStateOf<NonogramSession?>(null)
        private set
    var elapsedSeconds by mutableStateOf(0.0)
        private set
    var inputMode by mutableStateOf(NonogramInputMode.FILL)
    var nextMoveHint by mutableStateOf<NonogramNextMoveHint?>(null)
        private set
    var nextMoveHintActive by mutableStateOf(false)
        private set

    private var difficulty: Difficulty = requestedDifficulty
    private var finalized = false

    private val _result = MutableSharedFlow<NonogramResult>(extraBufferCapacity = 1)
    val result: SharedFlow<NonogramResult> = _result

    init {
        viewModelScope.launch {
            session = if (resume) restoreOrCreate() else freshSession(requestedDifficulty)
            runTicker()
        }
    }

    private suspend fun freshSession(difficulty: Difficulty): NonogramSession? {
        val entry = NonogramPuzzleBank.randomPuzzle(appContext, difficulty) ?: return null
        return createNonogramSession(entry)
    }

    private suspend fun restoreOrCreate(): NonogramSession? {
        val envelope = sessionRepository.activeSession.first()
        if (envelope != null && envelope.gameId == GameId.NONOGRAM.key) {
            val persisted = runCatching { json.decodeFromString<NonogramPersistedSession>(envelope.payload) }.getOrNull()
            if (persisted != null) {
                elapsedSeconds = envelope.elapsedSeconds
                difficulty = Difficulty.fromKey(persisted.entry.difficulty)
                val puzzle = buildNonogramPuzzle(persisted.entry)
                val cols = persisted.entry.cols
                return NonogramSession(
                    puzzle = puzzle,
                    board = List(persisted.entry.rows) { r -> List(cols) { c -> persisted.board[r * cols + c] } },
                    solution = persisted.entry.solution,
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

    fun onCellTap(row: Int, col: Int) {
        val current = session ?: return
        if (finalized) return
        val updated = applyNonogramTap(current, row, col, inputMode) ?: return
        nextMoveHint = null
        nextMoveHintActive = false
        session = updated
        persistIfMeaningful()
        checkSolved(updated)
    }

    fun onDragPaint(cells: List<Pair<Int, Int>>) {
        val current = session ?: return
        if (finalized || cells.isEmpty()) return
        val value = if (inputMode == NonogramInputMode.FILL) 1 else 0
        val updated = applyNonogramPaint(current, cells, value) ?: return
        nextMoveHint = null
        nextMoveHintActive = false
        session = updated
        persistIfMeaningful()
        checkSolved(updated)
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
        nextMoveHint = getNonogramNextMoveHint(current.puzzle, current.board)
    }

    private fun checkSolved(current: NonogramSession) {
        if (isNonogramSolved(current.board, current.solution)) {
            finishAsWin()
        }
    }

    fun endPuzzle() {
        if (finalized) return
        finishAsLoss("abandoned")
    }

    private fun finishAsWin() {
        if (finalized) return
        finalized = true
        val score = nonogramScore(elapsedSeconds.toInt())
        viewModelScope.launch {
            val previous = statsRepository.statsFor(GameId.NONOGRAM).first().forDifficulty(difficulty)
            statsRepository.recordResult(GameId.NONOGRAM, difficulty, solved = true, score = score)
            sessionRepository.clear()
            _result.emit(
                NonogramResult(
                    difficulty = difficulty,
                    solved = true,
                    score = score,
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
            statsRepository.recordResult(GameId.NONOGRAM, difficulty, solved = false, score = 0)
            sessionRepository.clear()
            _result.emit(
                NonogramResult(
                    difficulty = difficulty,
                    solved = false,
                    score = 0,
                    elapsedSeconds = elapsedSeconds.toInt(),
                    lossReason = reason,
                ),
            )
        }
    }

    private fun persistIfMeaningful() {
        val current = session ?: return
        if (finalized) return
        if (!nonogramHasMeaningfulProgress(current)) return
        val entry = NonogramPuzzleEntry(
            id = current.puzzle.id,
            difficulty = current.puzzle.difficulty,
            rows = current.puzzle.rows,
            cols = current.puzzle.cols,
            solution = current.solution,
        )
        val payload = json.encodeToString(
            NonogramPersistedSession(entry = entry, board = current.board.flatten()),
        )
        viewModelScope.launch {
            sessionRepository.save(
                ActiveSessionEnvelope(
                    gameId = GameId.NONOGRAM.key,
                    elapsedSeconds = elapsedSeconds,
                    payload = payload,
                ),
            )
        }
    }
}
