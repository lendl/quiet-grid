// app/src/main/java/com/quietgrid/app/games/arrowescape/ArrowEscapeViewModel.kt
package com.quietgrid.app.games.arrowescape

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
private const val ARROW_ESCAPE_BASE_SCORE = 50_000
private const val ARROW_ESCAPE_TIME_PENALTY_PER_SECOND = 100
private const val ARROW_ESCAPE_MIN_SCORE = 0

data class ArrowEscapeResult(
    val difficulty: Difficulty,
    val solved: Boolean,
    val score: Int,
    val elapsedSeconds: Int,
    val lossReason: String?,
    val isFirstSolve: Boolean = false,
    val isNewHighScore: Boolean = false,
)

data class ArrowEscapeTapEvent(val id: Int, val pieceIndex: Int, val removed: Boolean)

private fun arrowEscapeScore(elapsedSeconds: Int): Int =
    maxOf(ARROW_ESCAPE_MIN_SCORE, ARROW_ESCAPE_BASE_SCORE - elapsedSeconds * ARROW_ESCAPE_TIME_PENALTY_PER_SECOND)

private class ArrowEscapePuzzleAdapter(
    private val appContext: android.content.Context,
    private val historyStore: PlayHistoryStore,
) : PuzzleAdapter<ArrowEscapeSession, ArrowEscapeResult> {
    override val gameId: GameId = GameId.ARROWESCAPE

    override suspend fun freshSession(difficulty: Difficulty): ArrowEscapeSession? {
        val recentIds = historyStore.recentlyPlayedPuzzleIds(gameId, difficulty)
        val puzzle = ArrowEscapePuzzleBank.randomPuzzle(appContext, difficulty, recentIds) ?: return null
        return createArrowEscapeSession(puzzle)
    }

    override fun restoreSession(payload: String, elapsedSeconds: Double): ArrowEscapeSession? = runCatching {
        val persisted = json.decodeFromString<ArrowEscapePersistedSession>(payload)
        if (persisted.status != ArrowEscapeStatus.PLAYING.name) return@runCatching null
        ArrowEscapeSession(
            puzzle = persisted.puzzle,
            removedIndices = persisted.removedIndices.toSet(),
            lives = persisted.lives,
            selectedIndex = persisted.selectedIndex,
            status = ArrowEscapeStatus.valueOf(persisted.status),
        )
    }.getOrNull()

    override fun difficultyOf(session: ArrowEscapeSession): Difficulty = Difficulty.fromKey(session.puzzle.difficulty)

    override fun hasMeaningfulProgress(session: ArrowEscapeSession): Boolean = session.removedIndices.isNotEmpty()

    override fun encode(session: ArrowEscapeSession): String = json.encodeToString(
        ArrowEscapePersistedSession(
            puzzle = session.puzzle,
            removedIndices = session.removedIndices.toList(),
            lives = session.lives,
            selectedIndex = session.selectedIndex,
            status = session.status.name,
        ),
    )

    override fun puzzleIdOf(session: ArrowEscapeSession): String? = session.puzzle.id

    override fun scoreOnWin(session: ArrowEscapeSession, difficulty: Difficulty, elapsedSeconds: Int): Int = arrowEscapeScore(elapsedSeconds)

    override fun buildResult(session: ArrowEscapeSession?, outcome: PuzzleOutcome): ArrowEscapeResult = ArrowEscapeResult(
        difficulty = outcome.difficulty,
        solved = outcome.solved,
        score = outcome.score,
        elapsedSeconds = outcome.elapsedSeconds,
        lossReason = outcome.lossReason,
        isFirstSolve = outcome.isFirstSolve,
        isNewHighScore = outcome.isNewHighScore,
    )
}

@HiltViewModel(assistedFactory = ArrowEscapePlayViewModel.Factory::class)
class ArrowEscapePlayViewModel @AssistedInject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext appContext: android.content.Context,
    sessionRepository: SessionStore,
    statsRepository: StatsStore,
    historyRepository: PlayHistoryStore,
    @Assisted requestedDifficulty: Difficulty,
    @Assisted resume: Boolean,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(requestedDifficulty: Difficulty, resume: Boolean): ArrowEscapePlayViewModel
    }

    private val controller = PuzzleSessionController(
        scope = viewModelScope,
        sessionStore = sessionRepository,
        statsStore = statsRepository,
        historyStore = historyRepository,
        adapter = ArrowEscapePuzzleAdapter(appContext, historyRepository),
    )

    val session get() = controller.session
    val elapsedSeconds get() = controller.elapsedSeconds
    val result = controller.result

    var lastBlockedIndex by mutableStateOf<Int?>(null)
        private set
    var lastTapEvent by mutableStateOf<ArrowEscapeTapEvent?>(null)
        private set
    var isComputingHint by mutableStateOf(false)
        private set

    internal var hintDispatcher: CoroutineDispatcher = Dispatchers.Default
    private var hintJob: Job? = null
    private var tapEventSeq = 0

    init {
        controller.start(requestedDifficulty, resume)
    }

    fun onPieceTap(pieceIndex: Int) {
        if (controller.isFinalized) return
        val current = session ?: return
        val attempt = applyArrowEscapeAttempt(current, pieceIndex) ?: return
        hintJob?.cancel()
        hintJob = null
        isComputingHint = false
        lastBlockedIndex = if (attempt.removed) null else pieceIndex
        lastTapEvent = ArrowEscapeTapEvent(++tapEventSeq, pieceIndex, attempt.removed)
        val stillPlaying = attempt.session.status == ArrowEscapeStatus.PLAYING
        controller.updateSession(attempt.session, persist = stillPlaying)
        when (attempt.session.status) {
            ArrowEscapeStatus.WON -> controller.finishAsWin()
            ArrowEscapeStatus.LOST -> controller.finishAsLoss("out_of_lives")
            ArrowEscapeStatus.PLAYING -> Unit
        }
    }

    fun onHint() {
        if (controller.isFinalized || isComputingHint) return
        val current = session ?: return
        isComputingHint = true
        hintJob = viewModelScope.launch {
            val next = withContext(hintDispatcher) { applyArrowEscapeHint(current) }
            isComputingHint = false
            if (next != null && !controller.isFinalized && session == current) controller.updateSession(next, persist = false)
        }
    }

    fun endPuzzle() = controller.endPuzzle()
}
