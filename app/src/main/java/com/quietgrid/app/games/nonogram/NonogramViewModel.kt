package com.quietgrid.app.games.nonogram

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.journey.JourneyRequest
import com.quietgrid.app.core.journey.JourneySignals
import com.quietgrid.app.data.JourneyProgressStore
import com.quietgrid.app.data.PlayHistoryStore
import com.quietgrid.app.data.SessionStore
import com.quietgrid.app.data.StatsStore
import com.quietgrid.app.data.recentlyPlayedPuzzleIds
import com.quietgrid.app.session.PuzzleAdapter
import com.quietgrid.app.session.PuzzleOutcome
import com.quietgrid.app.session.PuzzleSessionController
import com.quietgrid.engine.journey.gridTransformById
import com.quietgrid.engine.nonogram.NonogramPuzzleEntry
import com.quietgrid.engine.nonogram.transformed
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

data class NonogramResult(
    val difficulty: Difficulty,
    val solved: Boolean,
    val score: Int,
    val elapsedSeconds: Int,
    val lossReason: String?,
    val isFirstSolve: Boolean = false,
    val isNewHighScore: Boolean = false,
    val solution: List<List<Boolean>> = emptyList(),
    val journeySignals: JourneySignals = JourneySignals(0, 0),
)

internal class NonogramPuzzleAdapter(
    private val appContext: Context,
    private val historyStore: PlayHistoryStore,
) : PuzzleAdapter<NonogramSession, NonogramResult> {
    override val gameId: GameId = GameId.NONOGRAM

    override suspend fun freshSession(difficulty: Difficulty): NonogramSession? {
        val recentIds = historyStore.recentlyPlayedPuzzleIds(gameId, difficulty)
        val entry = NonogramPuzzleBank.randomPuzzle(appContext, difficulty, recentIds) ?: return null
        return createNonogramSession(entry)
    }

    override suspend fun journeySession(difficulty: Difficulty, puzzleId: String, transform: Int): NonogramSession? =
        NonogramPuzzleBank.puzzleById(appContext, difficulty, puzzleId)?.transformed(gridTransformById(transform))?.let(::createNonogramSession)

    override fun journeySignals(session: NonogramSession): JourneySignals = JourneySignals(session.hintsUsed, 0)

    override suspend fun restoreSession(payload: String, elapsedSeconds: Double): NonogramSession? {
        val persisted = runCatching { json.decodeFromString<NonogramPersistedSession>(payload) }.getOrNull() ?: return null
        val puzzle = buildNonogramPuzzle(persisted.entry)
        val cols = persisted.entry.cols
        return NonogramSession(
            puzzle = puzzle,
            board = List(persisted.entry.rows) { r -> List(cols) { c -> persisted.board[r * cols + c] } },
            solution = persisted.entry.solution,
            hintsUsed = persisted.hintsUsed,
        )
    }

    override fun difficultyOf(session: NonogramSession): Difficulty = Difficulty.fromKey(session.puzzle.difficulty)

    override fun hasMeaningfulProgress(session: NonogramSession): Boolean = nonogramHasMeaningfulProgress(session)

    override fun encode(session: NonogramSession): String {
        val entry = NonogramPuzzleEntry(
            id = session.puzzle.id,
            difficulty = session.puzzle.difficulty,
            rows = session.puzzle.rows,
            cols = session.puzzle.cols,
            solution = session.solution,
        )
        return json.encodeToString(NonogramPersistedSession(entry = entry, board = session.board.flatten(), hintsUsed = session.hintsUsed))
    }

    override fun puzzleIdOf(session: NonogramSession): String? = session.puzzle.id

    override fun scoreOnWin(session: NonogramSession, difficulty: Difficulty, elapsedSeconds: Int): Int =
        nonogramScore(elapsedSeconds)

    override fun buildResult(session: NonogramSession?, outcome: PuzzleOutcome): NonogramResult = NonogramResult(
        difficulty = outcome.difficulty,
        solved = outcome.solved,
        score = outcome.score,
        elapsedSeconds = outcome.elapsedSeconds,
        lossReason = outcome.lossReason,
        isFirstSolve = outcome.isFirstSolve,
        isNewHighScore = outcome.isNewHighScore,
        solution = session?.solution ?: emptyList(),
        journeySignals = outcome.journeySignals,
    )
}

@HiltViewModel(assistedFactory = NonogramPlayViewModel.Factory::class)
class NonogramPlayViewModel @AssistedInject constructor(
    @ApplicationContext appContext: Context,
    sessionRepository: SessionStore,
    statsRepository: StatsStore,
    historyRepository: PlayHistoryStore,
    journeyStore: JourneyProgressStore,
    @Assisted requestedDifficulty: Difficulty,
    @Assisted resume: Boolean,
    @Assisted journey: JourneyRequest?,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(requestedDifficulty: Difficulty, resume: Boolean, journey: JourneyRequest?): NonogramPlayViewModel
    }

    private val controller = PuzzleSessionController(
        scope = viewModelScope,
        sessionStore = sessionRepository,
        statsStore = statsRepository,
        historyStore = historyRepository,
        adapter = NonogramPuzzleAdapter(appContext, historyRepository),
        journeyStore = journeyStore,
    )

    val session get() = controller.session
    val elapsedSeconds get() = controller.elapsedSeconds
    val result = controller.result
    val finishing = controller.finishing
    val restarts = controller.restarts

    var inputMode by mutableStateOf(NonogramInputMode.FILL)
    var nextMoveHint by mutableStateOf<NonogramNextMoveHint?>(null)
        private set
    var nextMoveHintActive by mutableStateOf(false)
        private set
    var isComputingHint by mutableStateOf(false)
        private set

    internal var hintDispatcher: CoroutineDispatcher = Dispatchers.Default
    private var hintJob: Job? = null

    init {
        controller.start(requestedDifficulty, resume, journey = journey)
    }

    private fun clearHint() {
        hintJob?.cancel()
        hintJob = null
        isComputingHint = false
        nextMoveHintActive = false
        nextMoveHint = null
    }

    fun onCellTap(row: Int, col: Int) {
        val current = session ?: return
        val updated = applyNonogramTap(current, row, col, inputMode) ?: return
        clearHint()
        controller.updateSession(updated)
        checkSolved(updated)
    }

    fun onDragPaint(cells: List<Pair<Int, Int>>) {
        val current = session ?: return
        if (cells.isEmpty()) return
        val value = if (inputMode == NonogramInputMode.FILL) 1 else 0
        val updated = applyNonogramPaint(current, cells, value) ?: return
        clearHint()
        controller.updateSession(updated)
        checkSolved(updated)
    }

    fun toggleNextMoveHint() {
        if (controller.isFinalized || isComputingHint) return
        if (nextMoveHintActive) {
            clearHint()
            return
        }
        val current = session ?: return
        nextMoveHintActive = true
        isComputingHint = true
        hintJob = viewModelScope.launch {
            val hint = withContext(hintDispatcher) { getNonogramNextMoveHint(current.puzzle, current.board, current.solution) }
            isComputingHint = false
            if (!controller.isFinalized && session == current && nextMoveHintActive) {
                nextMoveHint = hint
                if (hint != null) controller.updateSession(current.copy(hintsUsed = current.hintsUsed + 1))
            }
        }
    }

    private fun checkSolved(current: NonogramSession) {
        if (isNonogramSolved(current.board, current.solution)) {
            controller.finishAsWin()
        }
    }

    fun endPuzzle() = controller.endPuzzle()

    fun restartPuzzle() {
        clearHint()
        controller.restartPuzzle()
    }
}
