package com.quietgrid.app.games.hashi

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
import com.quietgrid.app.session.PuzzleAdapter
import com.quietgrid.app.session.PuzzleOutcome
import com.quietgrid.app.session.PuzzleSessionController
import com.quietgrid.engine.hashi.toBoard
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }
private const val HASHI_BASE_SCORE = 1000
private const val HASHI_HINT_PENALTY = 100
private const val HASHI_TIME_PENALTY_PER_SECOND = 1

data class HashiResult(
    val difficulty: Difficulty,
    val solved: Boolean,
    val score: Int,
    val elapsedSeconds: Int,
    val lossReason: String?,
    val isFirstSolve: Boolean = false,
    val isNewHighScore: Boolean = false,
)

data class HashiTapEvent(val id: Int, val edge: Int, val blocked: Boolean)

internal fun hashiScore(hintsUsed: Int, elapsedSeconds: Int): Int =
    maxOf(0, HASHI_BASE_SCORE - hintsUsed * HASHI_HINT_PENALTY - elapsedSeconds * HASHI_TIME_PENALTY_PER_SECOND)

private class HashiPuzzleAdapter(
    private val appContext: android.content.Context,
    private val historyStore: PlayHistoryStore,
) : PuzzleAdapter<HashiSession, HashiResult> {
    override val gameId: GameId = GameId.HASHI

    override suspend fun freshSession(difficulty: Difficulty): HashiSession? {
        val recentIds = historyStore.recentlyPlayedPuzzleIds(gameId, difficulty)
        val puzzle = HashiPuzzleBank.randomPuzzle(appContext, difficulty, recentIds) ?: return null
        return createHashiSession(puzzle)
    }

    override fun restoreSession(payload: String, elapsedSeconds: Double): HashiSession? = runCatching {
        val persisted = json.decodeFromString<HashiPersistedSession>(payload)
        if (persisted.status != HashiStatus.PLAYING.name) return@runCatching null
        val board = persisted.puzzle.toBoard()
        if (persisted.bridges.size != board.edges.size) return@runCatching null
        HashiSession(
            puzzle = persisted.puzzle,
            board = board,
            bridges = persisted.bridges,
            hintsUsed = persisted.hintsUsed,
            highlight = null,
            status = HashiStatus.PLAYING,
        )
    }.getOrNull()

    override fun difficultyOf(session: HashiSession): Difficulty = Difficulty.fromKey(session.puzzle.difficulty)

    override fun hasMeaningfulProgress(session: HashiSession): Boolean = session.bridges.any { it > 0 }

    override fun encode(session: HashiSession): String = json.encodeToString(
        HashiPersistedSession(
            puzzle = session.puzzle,
            bridges = session.bridges,
            hintsUsed = session.hintsUsed,
            status = session.status.name,
        ),
    )

    override fun puzzleIdOf(session: HashiSession): String? = session.puzzle.id

    override fun scoreOnWin(session: HashiSession, difficulty: Difficulty, elapsedSeconds: Int): Int =
        hashiScore(session.hintsUsed, elapsedSeconds)

    override fun buildResult(session: HashiSession?, outcome: PuzzleOutcome): HashiResult = HashiResult(
        difficulty = outcome.difficulty,
        solved = outcome.solved,
        score = outcome.score,
        elapsedSeconds = outcome.elapsedSeconds,
        lossReason = outcome.lossReason,
        isFirstSolve = outcome.isFirstSolve,
        isNewHighScore = outcome.isNewHighScore,
    )
}

@HiltViewModel(assistedFactory = HashiPlayViewModel.Factory::class)
class HashiPlayViewModel @AssistedInject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext appContext: android.content.Context,
    sessionRepository: SessionStore,
    statsRepository: StatsStore,
    historyRepository: PlayHistoryStore,
    @Assisted requestedDifficulty: Difficulty,
    @Assisted resume: Boolean,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(requestedDifficulty: Difficulty, resume: Boolean): HashiPlayViewModel
    }

    private val controller = PuzzleSessionController(
        scope = viewModelScope,
        sessionStore = sessionRepository,
        statsStore = statsRepository,
        historyStore = historyRepository,
        adapter = HashiPuzzleAdapter(appContext, historyRepository),
    )

    val session get() = controller.session
    val elapsedSeconds get() = controller.elapsedSeconds
    val result = controller.result
    val finishing = controller.finishing

    var lastTapEvent by mutableStateOf<HashiTapEvent?>(null)
        private set
    var isComputingHint by mutableStateOf(false)
        private set

    internal var hintDispatcher: CoroutineDispatcher = Dispatchers.Default
    private var hintJob: Job? = null
    private var tapEventSeq = 0

    init {
        controller.start(requestedDifficulty, resume)
    }

    fun onEdgeTap(edge: Int) {
        if (controller.isFinalized) return
        val current = session ?: return
        when (val outcome = applyHashiTap(current, edge) ?: return) {
            HashiTapOutcome.Blocked -> lastTapEvent = HashiTapEvent(++tapEventSeq, edge, blocked = true)
            is HashiTapOutcome.Changed -> {
                hintJob?.cancel()
                hintJob = null
                isComputingHint = false
                lastTapEvent = HashiTapEvent(++tapEventSeq, edge, blocked = false)
                val won = outcome.session.status == HashiStatus.WON
                controller.updateSession(outcome.session, persist = !won)
                if (won) controller.finishAsWin()
            }
        }
    }

    fun onHint() {
        if (controller.isFinalized || isComputingHint) return
        val current = session ?: return
        isComputingHint = true
        hintJob = viewModelScope.launch {
            val next = withContext(hintDispatcher) { applyHashiHint(current) }
            isComputingHint = false
            if (next != null && !controller.isFinalized && session == current) controller.updateSession(next)
        }
    }

    fun endPuzzle() = controller.endPuzzle()
}
