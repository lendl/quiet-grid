package com.quietgrid.app.games.blockescape

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
import com.quietgrid.engine.blockescape.BlockEscapeBlock
import com.quietgrid.engine.blockescape.BlockEscapeMove
import com.quietgrid.engine.blockescape.findBlockEscapeHint
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

data class BlockEscapeResult(
    val difficulty: Difficulty,
    val solved: Boolean,
    val score: Int,
    val accuracyPct: Int,
    val elapsedSeconds: Int,
    val lossReason: LossReason?,
    val isFirstSolve: Boolean = false,
    val isNewHighScore: Boolean = false,
    val journeySignals: JourneySignals = JourneySignals(0, 0),
)

internal class BlockEscapePuzzleAdapter(
    private val appContext: android.content.Context,
    private val historyStore: PlayHistoryStore,
) : PuzzleAdapter<BlockEscapeSession, BlockEscapeResult> {
    override val gameId: GameId = GameId.BLOCKESCAPE

    override suspend fun freshSession(difficulty: Difficulty): BlockEscapeSession? {
        val recentIds = historyStore.recentlyPlayedPuzzleIds(gameId, difficulty)
        val puzzle = BlockEscapePuzzleBank.randomPuzzle(appContext, difficulty, recentIds) ?: return null
        return createBlockEscapeSession(puzzle)
    }

    override suspend fun journeySession(difficulty: Difficulty, puzzleId: String, transform: Int): BlockEscapeSession? =
        BlockEscapePuzzleBank.puzzleById(appContext, difficulty, puzzleId)?.let(::createBlockEscapeSession)

    override fun journeySignals(session: BlockEscapeSession): JourneySignals = JourneySignals(session.hintsUsed, 0)

    override suspend fun restoreSession(payload: String, elapsedSeconds: Double): BlockEscapeSession? = runCatching {
        val persisted = json.decodeFromString<BlockEscapePersistedSession>(payload)
        if (persisted.status != BlockEscapeStatus.PLAYING.name) return@runCatching null
        BlockEscapeSession(
            puzzle = persisted.puzzle,
            blocks = persisted.blocks,
            moves = persisted.moves,
            hintsUsed = persisted.hintsUsed,
            status = BlockEscapeStatus.PLAYING,
        ).takeIf { isRestorableBlockEscapeSession(it) }
    }.getOrNull()

    override fun difficultyOf(session: BlockEscapeSession): Difficulty = Difficulty.fromKey(session.puzzle.difficulty)

    override fun hasMeaningfulProgress(session: BlockEscapeSession): Boolean = session.moves > 0

    override fun encode(session: BlockEscapeSession): String = json.encodeToString(
        BlockEscapePersistedSession(
            puzzle = session.puzzle,
            blocks = session.blocks,
            moves = session.moves,
            hintsUsed = session.hintsUsed,
            status = session.status.name,
        ),
    )

    override fun puzzleIdOf(session: BlockEscapeSession): String? = session.puzzle.id

    override fun scoreOnWin(session: BlockEscapeSession, difficulty: Difficulty, elapsedSeconds: Int): Int =
        blockEscapeScore(session, elapsedSeconds)

    override fun buildResult(session: BlockEscapeSession?, outcome: PuzzleOutcome): BlockEscapeResult = BlockEscapeResult(
        difficulty = outcome.difficulty,
        solved = outcome.solved,
        score = outcome.score,
        accuracyPct = session?.let { blockEscapeAccuracy(it) } ?: 0,
        elapsedSeconds = outcome.elapsedSeconds,
        lossReason = outcome.lossReason,
        isFirstSolve = outcome.isFirstSolve,
        isNewHighScore = outcome.isNewHighScore,
        journeySignals = outcome.journeySignals,
    )
}

@HiltViewModel(assistedFactory = BlockEscapePlayViewModel.Factory::class)
class BlockEscapePlayViewModel @AssistedInject constructor(
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
        fun create(requestedDifficulty: Difficulty, resume: Boolean, journey: JourneyRequest?): BlockEscapePlayViewModel
    }

    private val controller = PuzzleSessionController(
        scope = viewModelScope,
        sessionStore = sessionRepository,
        statsStore = statsRepository,
        historyStore = historyRepository,
        adapter = BlockEscapePuzzleAdapter(appContext, historyRepository),
        journeyStore = journeyStore,
        badgeStore = badgeStore,
    )

    val session get() = controller.session
    val elapsedSeconds get() = controller.elapsedSeconds
    val restarting get() = controller.restarting
    val result = controller.result
    val finishing = controller.finishing
    val restarts = controller.restarts

    var isComputingHint by mutableStateOf(false)
        private set

    var canUndo by mutableStateOf(false)
        private set

    var hint by mutableStateOf<BlockEscapeMove?>(null)
        private set

    internal var hintDispatcher: CoroutineDispatcher = Dispatchers.Default

    private val undoHistory = mutableListOf<BlockEscapeSession>()

    private var hintJob: Job? = null

    private var lastChargedBlocks: List<BlockEscapeBlock>? = null

    init {
        controller.start(requestedDifficulty, resume, journey = journey)
    }

    fun onMove(move: BlockEscapeMove) {
        if (controller.isFinalized) return
        clearHint()
        val current = session ?: return
        val next = applyBlockEscapeSlide(current, move) ?: return
        pushUndo(current)
        commit(next)
    }

    fun onUndo() {
        if (controller.isFinalized) return
        clearHint()
        val current = session ?: return
        val previous = undoHistory.removeLastOrNull() ?: return
        canUndo = undoHistory.isNotEmpty()
        controller.updateSession(previous.copy(hintsUsed = current.hintsUsed))
    }

    fun onHint() {
        if (controller.isFinalized || isComputingHint) return
        if (hint != null) {
            hint = null
            return
        }
        val current = session ?: return
        isComputingHint = true
        hintJob = viewModelScope.launch {
            val move = withContext(hintDispatcher) { findBlockEscapeHint(current.blocks) }
            isComputingHint = false
            if (move == null || controller.isFinalized || session != current) return@launch
            if (current.blocks != lastChargedBlocks) {
                chargeBlockEscapeHint(current)?.let { controller.updateSession(it, persist = true) }
                lastChargedBlocks = current.blocks
            }
            hint = move
        }
    }

    private fun clearHint() {
        hintJob?.cancel()
        hintJob = null
        isComputingHint = false
        hint = null
    }

    private fun pushUndo(snapshot: BlockEscapeSession) {
        undoHistory += snapshot
        canUndo = true
    }

    private fun commit(next: BlockEscapeSession) {
        val won = next.status == BlockEscapeStatus.WON
        controller.updateSession(next, persist = !won)
        if (won) controller.finishAsWin()
    }

    fun endPuzzle() = controller.endPuzzle()

    fun restartPuzzle() {
        clearHint()
        controller.restartPuzzle()
    }
}
