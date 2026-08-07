package com.quietgrid.app.games.blockfill

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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlin.math.max
import kotlin.math.round
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

private const val PUZZLE_SCORE_BASE = 50_000
private const val PUZZLE_SCORE_PENALTY_PER_SECOND = 50
private const val MIN_SCORE = 100

private fun computeBlockFillPuzzleScore(elapsedSeconds: Int): Int =
    max(MIN_SCORE, PUZZLE_SCORE_BASE - round(elapsedSeconds * PUZZLE_SCORE_PENALTY_PER_SECOND.toDouble()).toInt())

data class BlockFillResult(
    val difficulty: Difficulty,
    val solved: Boolean,
    val score: Int,
    val elapsedSeconds: Int,
    val lossReason: String?,
    val isFirstSolve: Boolean = false,
    val isNewHighScore: Boolean = false,
)

private class BlockFillPuzzleAdapter : PuzzleAdapter<BlockFillSession, BlockFillResult> {
    override val gameId: GameId = GameId.BLOCKFILL

    override suspend fun freshSession(difficulty: Difficulty): BlockFillSession =
        createBlockFillSession(difficulty.key)

    override fun restoreSession(payload: String, elapsedSeconds: Double): BlockFillSession? {
        val persisted = runCatching { json.decodeFromString<BlockFillPersistedSession>(payload) }.getOrNull() ?: return null
        if (persisted.status != BlockFillStatus.PLAYING) return null
        return BlockFillSession(
            puzzle = persisted.puzzle,
            board = persisted.board,
            tray = persisted.tray,
            score = persisted.score,
            comboStreak = persisted.comboStreak,
            status = persisted.status,
        )
    }

    override fun difficultyOf(session: BlockFillSession): Difficulty = Difficulty.fromKey(session.puzzle.difficulty)

    override fun hasMeaningfulProgress(session: BlockFillSession): Boolean = session.score > 0

    override fun encode(session: BlockFillSession): String = json.encodeToString(
        BlockFillPersistedSession(
            puzzle = session.puzzle,
            board = session.board,
            tray = session.tray,
            score = session.score,
            comboStreak = session.comboStreak,
            status = session.status,
        ),
    )

    override fun scoreOnWin(session: BlockFillSession, difficulty: Difficulty, elapsedSeconds: Int): Int =
        computeBlockFillPuzzleScore(elapsedSeconds)

    override fun buildResult(session: BlockFillSession?, outcome: PuzzleOutcome): BlockFillResult = BlockFillResult(
        difficulty = outcome.difficulty,
        solved = outcome.solved,
        score = outcome.score,
        elapsedSeconds = outcome.elapsedSeconds,
        lossReason = outcome.lossReason,
        isFirstSolve = outcome.isFirstSolve,
        isNewHighScore = outcome.isNewHighScore,
    )
}

@HiltViewModel(assistedFactory = BlockFillPlayViewModel.Factory::class)
class BlockFillPlayViewModel @AssistedInject constructor(
    sessionRepository: SessionStore,
    statsRepository: StatsStore,
    @Assisted requestedDifficulty: Difficulty,
    @Assisted resume: Boolean,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(requestedDifficulty: Difficulty, resume: Boolean): BlockFillPlayViewModel
    }

    private val controller = PuzzleSessionController(
        scope = viewModelScope,
        sessionStore = sessionRepository,
        statsStore = statsRepository,
        adapter = BlockFillPuzzleAdapter(),
    )

    val session get() = controller.session
    val elapsedSeconds get() = controller.elapsedSeconds
    val result = controller.result

    init {
        controller.start(requestedDifficulty, resume)
    }

    fun onPlacePiece(pieceIndex: Int, anchorRow: Int, anchorCol: Int) {
        val current = session ?: return
        val next = applyBlockFillPlacement(current, pieceIndex, anchorRow, anchorCol) ?: return
        when (next.status) {
            BlockFillStatus.WON -> {
                controller.updateSession(next, persist = false)
                controller.finishAsWin()
            }
            BlockFillStatus.LOST -> {
                controller.updateSession(next, persist = false)
                controller.finishAsLoss("rule-failure")
            }
            BlockFillStatus.PLAYING -> controller.updateSession(next)
        }
    }

    fun endPuzzle() = controller.endPuzzle()
}
