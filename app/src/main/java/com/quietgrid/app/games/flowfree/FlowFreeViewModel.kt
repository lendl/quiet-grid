package com.quietgrid.app.games.flowfree

import android.content.Context
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
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlin.math.max
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

private const val FLOWFREE_MAX_SCORE = 1000
private const val FLOWFREE_MIN_SCORE = 100
private const val FLOWFREE_TIME_PENALTY_PER_SECOND = 6

internal fun flowFreeScore(elapsedSeconds: Int): Int =
    max(FLOWFREE_MIN_SCORE, FLOWFREE_MAX_SCORE - elapsedSeconds * FLOWFREE_TIME_PENALTY_PER_SECOND)

data class FlowFreeResult(
    val difficulty: Difficulty,
    val solved: Boolean,
    val score: Int,
    val elapsedSeconds: Int,
    val lossReason: String?,
    val isFirstSolve: Boolean = false,
    val isNewHighScore: Boolean = false,
)

private class FlowFreePuzzleAdapter(
    private val appContext: Context,
    private val historyStore: PlayHistoryStore,
) : PuzzleAdapter<FlowFreeSession, FlowFreeResult> {
    override val gameId: GameId = GameId.FLOWFREE

    override suspend fun freshSession(difficulty: Difficulty): FlowFreeSession? {
        val recentIds = historyStore.recentlyPlayedPuzzleIds(gameId, difficulty)
        val puzzle = FlowFreePuzzleBank.randomPuzzle(appContext, difficulty, recentIds) ?: return null
        return createFlowFreeSession(puzzle)
    }

    override fun restoreSession(payload: String, elapsedSeconds: Double): FlowFreeSession? {
        val persisted = runCatching { json.decodeFromString<FlowFreePersistedSession>(payload) }.getOrNull() ?: return null
        return FlowFreeSession(puzzle = persisted.puzzle, paths = persisted.paths)
    }

    override fun difficultyOf(session: FlowFreeSession): Difficulty = Difficulty.fromKey(session.puzzle.difficulty)

    override fun hasMeaningfulProgress(session: FlowFreeSession): Boolean = flowFreeHasMeaningfulProgress(session)

    override fun encode(session: FlowFreeSession): String =
        json.encodeToString(FlowFreePersistedSession(puzzle = session.puzzle, paths = session.paths))

    override fun puzzleIdOf(session: FlowFreeSession): String? = session.puzzle.id

    override fun scoreOnWin(session: FlowFreeSession, difficulty: Difficulty, elapsedSeconds: Int): Int =
        flowFreeScore(elapsedSeconds)

    override fun buildResult(session: FlowFreeSession?, outcome: PuzzleOutcome): FlowFreeResult = FlowFreeResult(
        difficulty = outcome.difficulty,
        solved = outcome.solved,
        score = outcome.score,
        elapsedSeconds = outcome.elapsedSeconds,
        lossReason = outcome.lossReason,
        isFirstSolve = outcome.isFirstSolve,
        isNewHighScore = outcome.isNewHighScore,
    )
}

@HiltViewModel(assistedFactory = FlowFreePlayViewModel.Factory::class)
class FlowFreePlayViewModel @AssistedInject constructor(
    @ApplicationContext appContext: Context,
    sessionRepository: SessionStore,
    statsRepository: StatsStore,
    historyRepository: PlayHistoryStore,
    @Assisted requestedDifficulty: Difficulty,
    @Assisted resume: Boolean,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(requestedDifficulty: Difficulty, resume: Boolean): FlowFreePlayViewModel
    }

    private val controller = PuzzleSessionController(
        scope = viewModelScope,
        sessionStore = sessionRepository,
        statsStore = statsRepository,
        historyStore = historyRepository,
        adapter = FlowFreePuzzleAdapter(appContext, historyRepository),
    )

    val session get() = controller.session
    val elapsedSeconds get() = controller.elapsedSeconds
    val result = controller.result

    init {
        controller.start(requestedDifficulty, resume)
    }

    fun onDragStart(row: Int, col: Int) {
        val current = session ?: return
        controller.updateSession(flowFreeStartDrag(current, row, col))
    }

    fun onDragMove(row: Int, col: Int) {
        val current = session ?: return
        controller.updateSession(flowFreeExtendDrag(current, row, col))
    }

    fun onDragEnd() {
        val current = session ?: return
        val ended = flowFreeEndDrag(current)
        controller.updateSession(ended)
        if (flowFreeIsSolved(ended)) controller.finishAsWin()
    }

    fun endPuzzle() = controller.endPuzzle()
}
