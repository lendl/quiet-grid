package com.quietgrid.app.games.takuzu

import android.content.Context
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
import com.quietgrid.engine.takuzu.TakuzuGrid
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }
private const val VALIDATION_DELAY_MS = 800L

data class TakuzuResult(
    val difficulty: Difficulty,
    val solved: Boolean,
    val score: Int,
    val accuracyPct: Int,
    val elapsedSeconds: Int,
    val lossReason: String?,
    val isFirstSolve: Boolean = false,
    val isNewHighScore: Boolean = false,
    val analyzerSnapshot: String? = null,
)

private class TakuzuPuzzleAdapter(
    private val appContext: Context,
    private val historyStore: PlayHistoryStore,
) : PuzzleAdapter<TakuzuSession, TakuzuResult> {
    override val gameId: GameId = GameId.TAKUZU

    override suspend fun freshSession(difficulty: Difficulty): TakuzuSession? {
        val recentIds = historyStore.recentlyPlayedPuzzleIds(gameId, difficulty)
        val puzzle = TakuzuPuzzleBank.randomPuzzle(appContext, difficulty, recentIds) ?: return null
        return createTakuzuSession(puzzle)
    }

    override fun restoreSession(payload: String, elapsedSeconds: Double): TakuzuSession? {
        val persisted = runCatching { json.decodeFromString<TakuzuPersistedSession>(payload) }.getOrNull() ?: return null
        val size = persisted.puzzle.size
        return TakuzuSession(
            puzzle = persisted.puzzle,
            board = List(size) { r -> List(size) { c -> persisted.board[r * size + c] } },
            solution = decodeSolution(persisted.puzzle.solution, size),
            isGiven = decodeMask(persisted.puzzle.mask, size),
            finishedCells = List(size) { r -> List(size) { c -> persisted.finishedCells[r * size + c] } },
            accuracyDrops = persisted.accuracyDrops,
            penalizedLineKeys = persisted.penalizedLineKeys,
        )
    }

    override fun difficultyOf(session: TakuzuSession): Difficulty = Difficulty.fromKey(session.puzzle.difficulty)

    override fun hasMeaningfulProgress(session: TakuzuSession): Boolean = takuzuHasMeaningfulProgress(session)

    override fun encode(session: TakuzuSession): String = json.encodeToString(
        TakuzuPersistedSession(
            puzzle = session.puzzle,
            board = session.board.flatten(),
            finishedCells = session.finishedCells.flatten(),
            accuracyDrops = session.accuracyDrops,
            penalizedLineKeys = session.penalizedLineKeys,
        ),
    )

    override fun puzzleIdOf(session: TakuzuSession): String? = session.puzzle.id

    override fun scoreOnWin(session: TakuzuSession, difficulty: Difficulty, elapsedSeconds: Int): Int =
        takuzuScore(difficulty, elapsedSeconds, session.accuracyDrops)

    private fun analyzerSnapshotFor(session: TakuzuSession, solved: Boolean): String {
        val board = if (solved) decodePuzzleBoard(session.puzzle.solution, session.puzzle.mask, session.puzzle.size) else session.board
        return encode(session.copy(board = board))
    }

    override fun buildResult(session: TakuzuSession?, outcome: PuzzleOutcome): TakuzuResult = TakuzuResult(
        difficulty = outcome.difficulty,
        solved = outcome.solved,
        score = outcome.score,
        accuracyPct = if (outcome.solved) takuzuAccuracyPct(session?.accuracyDrops ?: 0) else 0,
        elapsedSeconds = outcome.elapsedSeconds,
        lossReason = outcome.lossReason,
        isFirstSolve = outcome.isFirstSolve,
        isNewHighScore = outcome.isNewHighScore,
        analyzerSnapshot = session?.let { analyzerSnapshotFor(it, outcome.solved) },
    )
}

@HiltViewModel(assistedFactory = TakuzuPlayViewModel.Factory::class)
class TakuzuPlayViewModel @AssistedInject constructor(
    @ApplicationContext appContext: Context,
    sessionRepository: SessionStore,
    statsRepository: StatsStore,
    historyRepository: PlayHistoryStore,
    @Assisted requestedDifficulty: Difficulty,
    @Assisted resume: Boolean,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(requestedDifficulty: Difficulty, resume: Boolean): TakuzuPlayViewModel
    }

    private val controller = PuzzleSessionController(
        scope = viewModelScope,
        sessionStore = sessionRepository,
        statsStore = statsRepository,
        historyStore = historyRepository,
        adapter = TakuzuPuzzleAdapter(appContext, historyRepository),
    )

    val session get() = controller.session
    val elapsedSeconds get() = controller.elapsedSeconds
    val result = controller.result

    var feedbackCorrectRows by mutableStateOf<Set<Int>>(emptySet())
        private set
    var feedbackCorrectCols by mutableStateOf<Set<Int>>(emptySet())
        private set
    var feedbackIncorrectRows by mutableStateOf<Set<Int>>(emptySet())
        private set
    var feedbackIncorrectCols by mutableStateOf<Set<Int>>(emptySet())
        private set
    var nextMoveHint by mutableStateOf<TakuzuNextMoveHint?>(null)
        private set
    var isComputingHint by mutableStateOf(false)
        private set

    internal var hintDispatcher: CoroutineDispatcher = Dispatchers.Default

    private var pendingLineKeys = mutableSetOf<LineKey>()
    private var pendingBoard: TakuzuGrid? = null
    private var validationJob: kotlinx.coroutines.Job? = null
    private var hintJob: Job? = null

    init {
        controller.start(requestedDifficulty, resume)
    }

    private fun clearHint() {
        hintJob?.cancel()
        hintJob = null
        isComputingHint = false
        nextMoveHint = null
    }

    fun onCellPress(row: Int, col: Int) {
        val current = session ?: return
        val updated = applyTakuzuPressCell(current, row, col) ?: return
        clearHint()
        controller.updateSession(updated)

        pendingLineKeys.add("r$row")
        pendingLineKeys.add("c$col")
        pendingBoard = updated.board

        validationJob?.cancel()
        validationJob = viewModelScope.launch {
            delay(VALIDATION_DELAY_MS)
            runFinalizeValidation()
        }
    }

    private fun runFinalizeValidation() {
        val current = session ?: return
        val board = pendingBoard ?: return
        val lineKeys = pendingLineKeys.toList()
        pendingLineKeys = mutableSetOf()
        pendingBoard = null

        val result = applyTakuzuFinalizeValidation(current, board, lineKeys)
        controller.updateSession(result.session)

        feedbackCorrectRows = result.effect.correctRowIndexes.toSet()
        feedbackCorrectCols = result.effect.correctColIndexes.toSet()
        feedbackIncorrectRows = result.effect.incorrectRowIndexes.toSet()
        feedbackIncorrectCols = result.effect.incorrectColIndexes.toSet()
        if (feedbackCorrectRows.isNotEmpty() || feedbackCorrectCols.isNotEmpty() ||
            feedbackIncorrectRows.isNotEmpty() || feedbackIncorrectCols.isNotEmpty()
        ) {
            viewModelScope.launch {
                delay(500)
                feedbackCorrectRows = emptySet()
                feedbackCorrectCols = emptySet()
                feedbackIncorrectRows = emptySet()
                feedbackIncorrectCols = emptySet()
            }
        }

        if (isBoardSolved(result.session.board, result.session.solution)) {
            controller.finishAsWin()
        }
    }

    fun endPuzzle() = controller.endPuzzle()

    fun toggleNextMoveHint() {
        if (controller.isFinalized || isComputingHint) return
        if (nextMoveHint != null) {
            clearHint()
            return
        }
        val current = session ?: return
        isComputingHint = true
        hintJob = viewModelScope.launch {
            val hint = withContext(hintDispatcher) { getTakuzuNextMoveHint(current.board, current.solution) }
            isComputingHint = false
            if (!controller.isFinalized && session == current) nextMoveHint = hint
        }
    }
}
