package com.quietgrid.app.games.takuzu

import android.content.Context
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
import com.quietgrid.engine.takuzu.TakuzuGrid
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
)

private class TakuzuPuzzleAdapter(private val appContext: Context) : PuzzleAdapter<TakuzuSession, TakuzuResult> {
    override val gameId: GameId = GameId.TAKUZU

    override suspend fun freshSession(difficulty: Difficulty): TakuzuSession? {
        val puzzle = TakuzuPuzzleBank.randomPuzzle(appContext, difficulty) ?: return null
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

    override fun scoreOnWin(session: TakuzuSession, difficulty: Difficulty, elapsedSeconds: Int): Int =
        takuzuScore(difficulty, elapsedSeconds, session.accuracyDrops)

    override fun buildResult(session: TakuzuSession?, outcome: PuzzleOutcome): TakuzuResult = TakuzuResult(
        difficulty = outcome.difficulty,
        solved = outcome.solved,
        score = outcome.score,
        accuracyPct = if (outcome.solved) takuzuAccuracyPct(session?.accuracyDrops ?: 0) else 0,
        elapsedSeconds = outcome.elapsedSeconds,
        lossReason = outcome.lossReason,
        isFirstSolve = outcome.isFirstSolve,
        isNewHighScore = outcome.isNewHighScore,
    )
}

class TakuzuPlayViewModel @AssistedInject constructor(
    @ApplicationContext appContext: Context,
    sessionRepository: SessionStore,
    statsRepository: StatsStore,
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
        adapter = TakuzuPuzzleAdapter(appContext),
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

    private var pendingLineKeys = mutableSetOf<LineKey>()
    private var pendingBoard: TakuzuGrid? = null
    private var validationJob: kotlinx.coroutines.Job? = null

    init {
        controller.start(requestedDifficulty, resume)
    }

    fun onCellPress(row: Int, col: Int) {
        val current = session ?: return
        val updated = applyTakuzuPressCell(current, row, col) ?: return
        nextMoveHint = null
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
        if (controller.isFinalized) return
        if (nextMoveHint != null) {
            nextMoveHint = null
            return
        }
        val current = session ?: return
        nextMoveHint = getTakuzuNextMoveHint(current.board)
    }
}
