package com.quietgrid.app.games.circuit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.journey.JourneyRequest
import com.quietgrid.app.core.journey.JourneySignals
import com.quietgrid.app.data.BadgeStore
import com.quietgrid.app.data.JourneyProgressStore
import com.quietgrid.app.data.PlayHistoryStore
import com.quietgrid.app.data.SessionStore
import com.quietgrid.app.data.StatsStore
import com.quietgrid.app.data.recentlyPlayedPuzzleIds
import com.quietgrid.app.session.LossReason
import com.quietgrid.app.session.PuzzleAdapter
import com.quietgrid.app.session.PuzzleOutcome
import com.quietgrid.app.session.PuzzleSessionController
import com.quietgrid.engine.circuit.CircuitHint
import com.quietgrid.engine.circuit.toBoard
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

data class CircuitResult(
    val difficulty: Difficulty,
    val solved: Boolean,
    val score: Int,
    val elapsedSeconds: Int,
    val lossReason: LossReason?,
    val isFirstSolve: Boolean = false,
    val isNewHighScore: Boolean = false,
    val journeySignals: JourneySignals = JourneySignals(0, 0),
)

internal class CircuitPuzzleAdapter(
    private val appContext: android.content.Context,
    private val historyStore: PlayHistoryStore,
) : PuzzleAdapter<CircuitSession, CircuitResult> {
    override val gameId: GameId = GameId.CIRCUIT

    override suspend fun freshSession(difficulty: Difficulty): CircuitSession? {
        val recentIds = historyStore.recentlyPlayedPuzzleIds(gameId, difficulty)
        val puzzle = CircuitPuzzleBank.randomPuzzle(appContext, difficulty, recentIds) ?: return null
        return createCircuitSession(puzzle)
    }

    override suspend fun journeySession(difficulty: Difficulty, puzzleId: String, transform: Int): CircuitSession? =
        CircuitPuzzleBank.puzzleById(appContext, difficulty, puzzleId)?.let(::createCircuitSession)

    override fun journeySignals(session: CircuitSession): JourneySignals = JourneySignals(session.hintsUsed, 0)

    override suspend fun restoreSession(payload: String, elapsedSeconds: Double): CircuitSession? = runCatching {
        val persisted = json.decodeFromString<CircuitPersistedSession>(payload)
        if (persisted.status != CircuitStatus.PLAYING.name) return@runCatching null
        val board = persisted.puzzle.toBoard()
        if (persisted.turns.size != board.size || persisted.locked.size != board.size) return@runCatching null
        CircuitSession(
            puzzle = persisted.puzzle,
            board = board,
            turns = persisted.turns,
            locked = persisted.locked,
            moves = persisted.moves,
            hintsUsed = persisted.hintsUsed,
            status = CircuitStatus.PLAYING,
        )
    }.getOrNull()

    override fun difficultyOf(session: CircuitSession): Difficulty = Difficulty.fromKey(session.puzzle.difficulty)

    override fun hasMeaningfulProgress(session: CircuitSession): Boolean = session.moves > 0 || session.locked.any { it }

    override fun encode(session: CircuitSession): String = json.encodeToString(
        CircuitPersistedSession(
            puzzle = session.puzzle,
            turns = session.turns,
            locked = session.locked,
            moves = session.moves,
            hintsUsed = session.hintsUsed,
            status = session.status.name,
        ),
    )

    override fun puzzleIdOf(session: CircuitSession): String? = session.puzzle.id

    override fun scoreOnWin(session: CircuitSession, difficulty: Difficulty, elapsedSeconds: Int): Int =
        circuitScore(session.hintsUsed, elapsedSeconds)

    override fun buildResult(session: CircuitSession?, outcome: PuzzleOutcome): CircuitResult = CircuitResult(
        difficulty = outcome.difficulty,
        solved = outcome.solved,
        score = outcome.score,
        elapsedSeconds = outcome.elapsedSeconds,
        lossReason = outcome.lossReason,
        isFirstSolve = outcome.isFirstSolve,
        isNewHighScore = outcome.isNewHighScore,
        journeySignals = outcome.journeySignals,
    )
}

@HiltViewModel(assistedFactory = CircuitPlayViewModel.Factory::class)
class CircuitPlayViewModel @AssistedInject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext appContext: android.content.Context,
    sessionRepository: SessionStore,
    statsRepository: StatsStore,
    historyRepository: PlayHistoryStore,
    journeyStore: JourneyProgressStore,
    badgeStore: BadgeStore,
    @Assisted requestedDifficulty: Difficulty,
    @Assisted resume: Boolean,
    @Assisted journey: JourneyRequest?,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(requestedDifficulty: Difficulty, resume: Boolean, journey: JourneyRequest?): CircuitPlayViewModel
    }

    private val controller = PuzzleSessionController(
        scope = viewModelScope,
        sessionStore = sessionRepository,
        statsStore = statsRepository,
        historyStore = historyRepository,
        adapter = CircuitPuzzleAdapter(appContext, historyRepository),
        journeyStore = journeyStore,
        badgeStore = badgeStore,
    )

    val session get() = controller.session
    val elapsedSeconds get() = controller.elapsedSeconds
    val restarting get() = controller.restarting
    val result = controller.result
    val finishing = controller.finishing
    val restarts = controller.restarts

    var lastTapEvent by mutableStateOf<CircuitTapEvent?>(null)
        private set
    var isComputingHint by mutableStateOf(false)
        private set
    var hint by mutableStateOf<CircuitHint?>(null)
        private set

    internal var hintDispatcher: CoroutineDispatcher = Dispatchers.Default
    private var hintJob: Job? = null
    private var tapEventSeq = 0

    init {
        controller.start(requestedDifficulty, resume, journey = journey)
    }

    private fun clearHint() {
        hintJob?.cancel()
        hintJob = null
        isComputingHint = false
        hint = null
    }

    private fun commit(next: CircuitSession) {
        val won = next.status == CircuitStatus.WON
        controller.updateSession(next, persist = !won)
        if (won) controller.finishAsWin()
    }

    fun onTileTap(cell: Int) {
        if (controller.isFinalized) return
        val current = session ?: return
        when (val outcome = applyCircuitRotate(current, cell) ?: return) {
            CircuitTapOutcome.Blocked -> lastTapEvent = CircuitTapEvent(++tapEventSeq, cell, blocked = true)
            is CircuitTapOutcome.Changed -> {
                clearHint()
                lastTapEvent = CircuitTapEvent(++tapEventSeq, cell, blocked = false)
                commit(outcome.session)
            }
        }
    }

    fun onTileLongPress(cell: Int) {
        if (controller.isFinalized) return
        val current = session ?: return
        val next = applyCircuitLockToggle(current, cell) ?: return
        clearHint()
        lastTapEvent = CircuitTapEvent(++tapEventSeq, cell, blocked = false)
        controller.updateSession(next)
    }

    fun toggleHint() {
        if (controller.isFinalized || isComputingHint) return
        if (hint != null) {
            clearHint()
            return
        }
        val current = session ?: return
        isComputingHint = true
        hintJob = viewModelScope.launch {
            val result = withContext(hintDispatcher) { applyCircuitHint(current) }
            isComputingHint = false
            if (result == null || controller.isFinalized || session != current) return@launch
            hint = result.hint
            commit(result.session)
        }
    }

    fun endPuzzle() = controller.endPuzzle()

    fun restartPuzzle() {
        controller.restartPuzzle()
    }
}
