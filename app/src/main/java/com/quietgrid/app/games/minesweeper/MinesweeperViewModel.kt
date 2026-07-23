package com.quietgrid.app.games.minesweeper

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

data class MinesweeperResult(
    val difficulty: Difficulty,
    val solved: Boolean,
    val score: Int,
    val elapsedSeconds: Int,
    val lossReason: String?,
    val isFirstSolve: Boolean = false,
    val isNewHighScore: Boolean = false,
)

class MinesweeperPlayViewModel(
    private val sessionRepository: SessionRepository,
    private val statsRepository: StatsRepository,
    private val requestedDifficulty: Difficulty,
    private val resume: Boolean,
) : ViewModel() {

    var session by mutableStateOf<MinesweeperSession?>(null)
        private set
    var elapsedSeconds by mutableStateOf(0.0)
        private set
    var nextMoveHint by mutableStateOf<MinesweeperNextMoveHint?>(null)
        private set

    private var difficulty: Difficulty = requestedDifficulty
    private var finalized = false

    private val _result = MutableSharedFlow<MinesweeperResult>(extraBufferCapacity = 1)
    val result: SharedFlow<MinesweeperResult> = _result

    init {
        viewModelScope.launch {
            session = if (resume) restoreOrCreate() else createMinesweeperSession(requestedDifficulty)
            runTicker()
        }
    }

    private suspend fun restoreOrCreate(): MinesweeperSession {
        val envelope = sessionRepository.activeSession.first()
        if (envelope != null && envelope.gameId == GameId.MINESWEEPER.key) {
            val persisted = runCatching { json.decodeFromString<MinesweeperPersistedSession>(envelope.payload) }.getOrNull()
            if (persisted != null && persisted.board.status == MinesweeperStatus.PLAYING) {
                elapsedSeconds = envelope.elapsedSeconds
                difficulty = Difficulty.fromKey(persisted.puzzle.difficulty)
                return MinesweeperSession(puzzle = persisted.puzzle, board = persisted.board)
            }
        }
        return createMinesweeperSession(requestedDifficulty)
    }

    private suspend fun runTicker() {
        while (true) {
            delay(1000)
            if (finalized || session == null) continue
            elapsedSeconds += 1.0
            persistIfMeaningful()
        }
    }

    fun onReveal(row: Int, col: Int) {
        val current = session ?: return
        if (finalized) return
        val nextBoard = revealMinesweeperCell(current.board, current.puzzle, row, col)
        if (nextBoard == current.board) return
        nextMoveHint = null
        applyBoard(current, nextBoard)
    }

    fun onToggleFlag(row: Int, col: Int) {
        val current = session ?: return
        if (finalized) return
        val nextBoard = toggleMinesweeperFlag(current.board, row, col)
        if (nextBoard == current.board) return
        nextMoveHint = null
        applyBoard(current, nextBoard)
    }

    fun toggleNextMoveHint() {
        if (finalized) return
        if (nextMoveHint != null) {
            nextMoveHint = null
            return
        }
        val current = session ?: return
        nextMoveHint = getMinesweeperNextMoveHint(current.board)
    }

    private fun applyBoard(current: MinesweeperSession, nextBoard: MinesweeperBoard) {
        val updated = current.copy(board = nextBoard)
        session = updated
        when (nextBoard.status) {
            MinesweeperStatus.WON -> finishAsWin()
            MinesweeperStatus.LOST -> finishAsLoss("rule-failure")
            MinesweeperStatus.PLAYING -> persistIfMeaningful()
        }
    }

    fun endPuzzle() {
        if (finalized) return
        finishAsLoss("abandoned")
    }

    private fun finishAsWin() {
        if (finalized) return
        finalized = true
        val score = minesweeperScore(difficulty, elapsedSeconds.toInt())
        viewModelScope.launch {
            val previous = statsRepository.statsFor(GameId.MINESWEEPER).first().forDifficulty(difficulty)
            statsRepository.recordResult(GameId.MINESWEEPER, difficulty, solved = true, score = score)
            sessionRepository.clear()
            _result.emit(
                MinesweeperResult(
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
            statsRepository.recordResult(GameId.MINESWEEPER, difficulty, solved = false, score = 0)
            sessionRepository.clear()
            _result.emit(
                MinesweeperResult(
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
        if (!minesweeperHasMeaningfulProgress(current)) return
        val payload = json.encodeToString(MinesweeperPersistedSession(puzzle = current.puzzle, board = current.board))
        viewModelScope.launch {
            sessionRepository.save(
                ActiveSessionEnvelope(
                    gameId = GameId.MINESWEEPER.key,
                    elapsedSeconds = elapsedSeconds,
                    payload = payload,
                ),
            )
        }
    }
}
