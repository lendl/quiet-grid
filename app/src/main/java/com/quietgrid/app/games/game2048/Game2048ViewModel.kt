package com.quietgrid.app.games.game2048

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
import com.quietgrid.app.session.PuzzleAdapter
import com.quietgrid.app.session.PuzzleOutcome
import com.quietgrid.app.session.PuzzleSessionController
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

data class Game2048Result(
    val difficulty: Difficulty,
    val solved: Boolean,
    val score: Int,
    val elapsedSeconds: Int,
    val lossReason: String?,
    val isFirstSolve: Boolean = false,
    val isNewHighScore: Boolean = false,
    val bestTile: Int = 0,
)

private class Game2048PuzzleAdapter : PuzzleAdapter<Game2048Session, Game2048Result> {
    override val gameId: GameId = GameId.GAME_2048

    override suspend fun freshSession(difficulty: Difficulty): Game2048Session =
        createGame2048Session(difficulty)

    override fun restoreSession(payload: String, elapsedSeconds: Double): Game2048Session? {
        val persisted = runCatching { json.decodeFromString<Game2048PersistedSession>(payload) }.getOrNull()
            ?: return null
        if (persisted.board.status != Game2048Status.PLAYING) return null
        return Game2048Session(puzzle = persisted.puzzle, board = persisted.board)
    }

    override fun difficultyOf(session: Game2048Session): Difficulty = Difficulty.fromKey(session.puzzle.difficulty)

    override fun hasMeaningfulProgress(session: Game2048Session): Boolean =
        game2048HasMeaningfulProgress(session)

    override fun encode(session: Game2048Session): String =
        json.encodeToString(Game2048PersistedSession(puzzle = session.puzzle, board = session.board))

    override fun scoreOnWin(session: Game2048Session, difficulty: Difficulty, elapsedSeconds: Int): Int =
        session.board.score

    override fun scoreOnLoss(session: Game2048Session, difficulty: Difficulty, elapsedSeconds: Int): Int =
        session.board.score

    override fun buildResult(session: Game2048Session?, outcome: PuzzleOutcome): Game2048Result =
        Game2048Result(
            difficulty = outcome.difficulty,
            solved = outcome.solved,
            score = outcome.score,
            elapsedSeconds = outcome.elapsedSeconds,
            lossReason = outcome.lossReason,
            isFirstSolve = outcome.isFirstSolve,
            isNewHighScore = outcome.isNewHighScore,
            bestTile = session?.board?.let { game2048BestTile(it) } ?: 0,
        )
}

@HiltViewModel(assistedFactory = Game2048PlayViewModel.Factory::class)
class Game2048PlayViewModel @AssistedInject constructor(
    sessionRepository: SessionStore,
    statsRepository: StatsStore,
    historyRepository: PlayHistoryStore,
    @Assisted requestedDifficulty: Difficulty,
    @Assisted resume: Boolean,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(requestedDifficulty: Difficulty, resume: Boolean): Game2048PlayViewModel
    }

    private val controller = PuzzleSessionController(
        scope = viewModelScope,
        sessionStore = sessionRepository,
        statsStore = statsRepository,
        historyStore = historyRepository,
        adapter = Game2048PuzzleAdapter(),
    )

    val session get() = controller.session
    val elapsedSeconds get() = controller.elapsedSeconds
    val result = controller.result

    var lastMove by mutableStateOf<Game2048MoveResult?>(null)
        private set

    init {
        controller.start(requestedDifficulty, resume)
    }

    fun onSwipe(direction: Game2048Direction) {
        val current = session ?: return
        if (current.board.status != Game2048Status.PLAYING) return
        val moveResult = applyGame2048MoveDetailed(current.board, current.puzzle, direction)
        val nextBoard = moveResult.board
        if (nextBoard == current.board) return
        lastMove = moveResult

        val updated = current.copy(board = nextBoard)
        when (nextBoard.status) {
            Game2048Status.WON -> {
                controller.updateSession(updated, persist = false)
                controller.finishAsWin()
            }
            Game2048Status.LOST -> {
                controller.updateSession(updated, persist = false)
                controller.finishAsLoss("no-moves-left")
            }
            Game2048Status.PLAYING -> controller.updateSession(updated)
        }
    }

    fun endPuzzle() = controller.endPuzzle()
}
