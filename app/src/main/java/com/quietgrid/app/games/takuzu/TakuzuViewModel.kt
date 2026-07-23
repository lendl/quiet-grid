package com.quietgrid.app.games.takuzu

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

data class TakuzuResult(
    val difficulty: Difficulty,
    val solved: Boolean,
    val score: Int,
    val accuracyPct: Int,
    val elapsedSeconds: Int,
    val lossReason: String?,
    val isFirstSolve: Boolean = false,
    val isNewHighScore: Boolean = false,
)

class TakuzuPlayViewModel(
    private val appContext: Context,
    private val sessionRepository: SessionRepository,
    private val statsRepository: StatsRepository,
    private val requestedDifficulty: Difficulty,
    private val resume: Boolean,
) : ViewModel() {

    var session by mutableStateOf<TakuzuSession?>(null)
        private set
    var elapsedSeconds by mutableStateOf(0.0)
        private set
    var feedbackCorrectRows by mutableStateOf<Set<Int>>(emptySet())
        private set
    var feedbackCorrectCols by mutableStateOf<Set<Int>>(emptySet())
        private set
    var feedbackIncorrectRows by mutableStateOf<Set<Int>>(emptySet())
        private set
    var feedbackIncorrectCols by mutableStateOf<Set<Int>>(emptySet())
        private set
    var nextMoveHint by mutableStateOf<TakuzuNextMoveHint?>(null)
        private set

    private var difficulty: Difficulty = requestedDifficulty
    private var finalized = false
    private var pendingLineKeys = mutableSetOf<LineKey>()
    private var pendingBoard: TakuzuGrid? = null
    private var validationJob: kotlinx.coroutines.Job? = null

    private val _result = MutableSharedFlow<TakuzuResult>(extraBufferCapacity = 1)
    val result: SharedFlow<TakuzuResult> = _result

    init {
        viewModelScope.launch {
            session = if (resume) restoreOrCreate() else freshSession(requestedDifficulty)
            runTicker()
        }
    }

    private suspend fun freshSession(difficulty: Difficulty): TakuzuSession? {
        val puzzle = TakuzuPuzzleBank.randomPuzzle(appContext, difficulty) ?: return null
        return createTakuzuSession(puzzle)
    }

    private suspend fun restoreOrCreate(): TakuzuSession? {
        val envelope = sessionRepository.activeSession.first()
        if (envelope != null && envelope.gameId == GameId.TAKUZU.key) {
            val persisted = runCatching { json.decodeFromString<TakuzuPersistedSession>(envelope.payload) }.getOrNull()
            if (persisted != null) {
                elapsedSeconds = envelope.elapsedSeconds
                difficulty = Difficulty.fromKey(persisted.puzzle.difficulty)
                val size = persisted.puzzle.size
                return TakuzuSession(
                    puzzle = persisted.puzzle,
                    board = List(size) { r -> List(size) { c -> persisted.board[r * size + c] } },
                    solution = decodeSolution(persisted.puzzle.solution, size),
                    isGiven = decodeMask(persisted.puzzle.mask, size),
                    finishedCells = List(size) { r -> List(size) { c -> persisted.finishedCells[r * size + c] } },
                    accuracyDrops = persisted.accuracyDrops,
                    penalizedLineKeys = persisted.penalizedLineKeys,
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
        if (finalized) return

        val updated = applyTakuzuPressCell(current, row, col) ?: return
        nextMoveHint = null
        session = updated
        persistIfMeaningful()

        pendingLineKeys.add("r$row")
        pendingLineKeys.add("c$col")
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
        val lineKeys = pendingLineKeys.toList()
        pendingLineKeys = mutableSetOf()
        pendingBoard = null

        val result = applyTakuzuFinalizeValidation(current, board, lineKeys)
        session = result.session

        feedbackCorrectRows = result.effect.correctRowIndexes.toSet()
        feedbackCorrectCols = result.effect.correctColIndexes.toSet()
        feedbackIncorrectRows = result.effect.incorrectRowIndexes.toSet()
        feedbackIncorrectCols = result.effect.incorrectColIndexes.toSet()
        if (feedbackCorrectRows.isNotEmpty() || feedbackCorrectCols.isNotEmpty() ||
            feedbackIncorrectRows.isNotEmpty() || feedbackIncorrectCols.isNotEmpty()
        ) {
            viewModelScope.launch {
                delay(500)
                feedbackCorrectRows = emptySet()
                feedbackCorrectCols = emptySet()
                feedbackIncorrectRows = emptySet()
                feedbackIncorrectCols = emptySet()
            }
        }

        persistIfMeaningful()

        if (isBoardSolved(result.session.board, result.session.solution)) {
            finishAsWin()
        }
    }

    fun endPuzzle() {
        if (finalized) return
        finishAsLoss("abandoned")
    }

    fun toggleNextMoveHint() {
        if (finalized) return
        if (nextMoveHint != null) {
            nextMoveHint = null
            return
        }
        val current = session ?: return
        nextMoveHint = getTakuzuNextMoveHint(current.board)
    }

    private fun finishAsWin() {
        if (finalized) return
        finalized = true
        val current = session ?: return
        val score = takuzuScore(difficulty, elapsedSeconds.toInt(), current.accuracyDrops)
        viewModelScope.launch {
            val previous = statsRepository.statsFor(GameId.TAKUZU).first().forDifficulty(difficulty)
            statsRepository.recordResult(GameId.TAKUZU, difficulty, solved = true, score = score)
            sessionRepository.clear()
            _result.emit(
                TakuzuResult(
                    difficulty = difficulty,
                    solved = true,
                    score = score,
                    accuracyPct = takuzuAccuracyPct(current.accuracyDrops),
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
            statsRepository.recordResult(GameId.TAKUZU, difficulty, solved = false, score = 0)
            sessionRepository.clear()
            _result.emit(
                TakuzuResult(
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
        if (!takuzuHasMeaningfulProgress(current)) return
        val payload = json.encodeToString(
            TakuzuPersistedSession(
                puzzle = current.puzzle,
                board = current.board.flatten(),
                finishedCells = current.finishedCells.flatten(),
                accuracyDrops = current.accuracyDrops,
                penalizedLineKeys = current.penalizedLineKeys,
            ),
        )
        viewModelScope.launch {
            sessionRepository.save(
                ActiveSessionEnvelope(
                    gameId = GameId.TAKUZU.key,
                    elapsedSeconds = elapsedSeconds,
                    payload = payload,
                ),
            )
        }
    }
}
