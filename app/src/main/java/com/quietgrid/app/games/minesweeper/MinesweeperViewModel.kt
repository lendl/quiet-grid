package com.quietgrid.app.games.minesweeper

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
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
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

private class MinesweeperPuzzleAdapter : PuzzleAdapter<MinesweeperSession, MinesweeperResult> {
    override val gameId: GameId = GameId.MINESWEEPER

    override suspend fun freshSession(difficulty: Difficulty): MinesweeperSession =
        createMinesweeperSession(difficulty)

    override fun restoreSession(payload: String, elapsedSeconds: Double): MinesweeperSession? {
        val persisted = runCatching { json.decodeFromString<MinesweeperPersistedSession>(payload) }.getOrNull()
            ?: return null
        if (persisted.board.status != MinesweeperStatus.PLAYING) return null
        return MinesweeperSession(puzzle = persisted.puzzle, board = persisted.board)
    }

    override fun difficultyOf(session: MinesweeperSession): Difficulty = Difficulty.fromKey(session.puzzle.difficulty)

    override fun hasMeaningfulProgress(session: MinesweeperSession): Boolean =
        minesweeperHasMeaningfulProgress(session)

    override fun encode(session: MinesweeperSession): String =
        json.encodeToString(MinesweeperPersistedSession(puzzle = session.puzzle, board = session.board))

    override fun scoreOnWin(session: MinesweeperSession, difficulty: Difficulty, elapsedSeconds: Int): Int =
        minesweeperScore(difficulty, elapsedSeconds)

    override fun buildResult(session: MinesweeperSession?, outcome: PuzzleOutcome): MinesweeperResult =
        MinesweeperResult(
            difficulty = outcome.difficulty,
            solved = outcome.solved,
            score = outcome.score,
            elapsedSeconds = outcome.elapsedSeconds,
            lossReason = outcome.lossReason,
            isFirstSolve = outcome.isFirstSolve,
            isNewHighScore = outcome.isNewHighScore,
        )
}

class MinesweeperPlayViewModel @AssistedInject constructor(
    sessionRepository: SessionStore,
    statsRepository: StatsStore,
    @Assisted requestedDifficulty: Difficulty,
    @Assisted resume: Boolean,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(requestedDifficulty: Difficulty, resume: Boolean): MinesweeperPlayViewModel
    }

    private val controller = PuzzleSessionController(
        scope = viewModelScope,
        sessionStore = sessionRepository,
        statsStore = statsRepository,
        adapter = MinesweeperPuzzleAdapter(),
    )

    val session get() = controller.session
    val elapsedSeconds get() = controller.elapsedSeconds
    val result = controller.result

    var nextMoveHint by mutableStateOf<MinesweeperNextMoveHint?>(null)
        private set

    init {
        controller.start(requestedDifficulty, resume)
    }

    fun onReveal(row: Int, col: Int) {
        val current = session ?: return
        val nextBoard = revealMinesweeperCell(current.board, current.puzzle, row, col)
        if (nextBoard == current.board) return
        nextMoveHint = null
        applyBoard(current, nextBoard)
    }

    fun onToggleFlag(row: Int, col: Int) {
        val current = session ?: return
        val nextBoard = toggleMinesweeperFlag(current.board, row, col)
        if (nextBoard == current.board) return
        nextMoveHint = null
        applyBoard(current, nextBoard)
    }

    fun toggleNextMoveHint() {
        if (controller.isFinalized) return
        if (nextMoveHint != null) {
            nextMoveHint = null
            return
        }
        val current = session ?: return
        nextMoveHint = getMinesweeperNextMoveHint(current.board)
    }

    private fun applyBoard(current: MinesweeperSession, nextBoard: MinesweeperBoard) {
        val updated = current.copy(board = nextBoard)
        when (nextBoard.status) {
            MinesweeperStatus.WON -> {
                controller.updateSession(updated, persist = false)
                controller.finishAsWin()
            }
            MinesweeperStatus.LOST -> {
                controller.updateSession(updated, persist = false)
                controller.finishAsLoss("rule-failure")
            }
            MinesweeperStatus.PLAYING -> controller.updateSession(updated)
        }
    }

    fun endPuzzle() = controller.endPuzzle()
}
