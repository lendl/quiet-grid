package com.quietgrid.app.games.nonogram

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
import com.quietgrid.engine.nonogram.NonogramPuzzleEntry
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
)

private class NonogramPuzzleAdapter(private val appContext: Context) : PuzzleAdapter<NonogramSession, NonogramResult> {
    override val gameId: GameId = GameId.NONOGRAM

    override suspend fun freshSession(difficulty: Difficulty): NonogramSession? {
        val entry = NonogramPuzzleBank.randomPuzzle(appContext, difficulty) ?: return null
        return createNonogramSession(entry)
    }

    override fun restoreSession(payload: String, elapsedSeconds: Double): NonogramSession? {
        val persisted = runCatching { json.decodeFromString<NonogramPersistedSession>(payload) }.getOrNull() ?: return null
        val puzzle = buildNonogramPuzzle(persisted.entry)
        val cols = persisted.entry.cols
        return NonogramSession(
            puzzle = puzzle,
            board = List(persisted.entry.rows) { r -> List(cols) { c -> persisted.board[r * cols + c] } },
            solution = persisted.entry.solution,
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
        return json.encodeToString(NonogramPersistedSession(entry = entry, board = session.board.flatten()))
    }

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
    )
}

@HiltViewModel(assistedFactory = NonogramPlayViewModel.Factory::class)
class NonogramPlayViewModel @AssistedInject constructor(
    @ApplicationContext appContext: Context,
    sessionRepository: SessionStore,
    statsRepository: StatsStore,
    @Assisted requestedDifficulty: Difficulty,
    @Assisted resume: Boolean,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(requestedDifficulty: Difficulty, resume: Boolean): NonogramPlayViewModel
    }

    private val controller = PuzzleSessionController(
        scope = viewModelScope,
        sessionStore = sessionRepository,
        statsStore = statsRepository,
        adapter = NonogramPuzzleAdapter(appContext),
    )

    val session get() = controller.session
    val elapsedSeconds get() = controller.elapsedSeconds
    val result = controller.result

    var inputMode by mutableStateOf(NonogramInputMode.FILL)
    var nextMoveHint by mutableStateOf<NonogramNextMoveHint?>(null)
        private set
    var nextMoveHintActive by mutableStateOf(false)
        private set

    init {
        controller.start(requestedDifficulty, resume)
    }

    fun onCellTap(row: Int, col: Int) {
        val current = session ?: return
        val updated = applyNonogramTap(current, row, col, inputMode) ?: return
        nextMoveHint = null
        nextMoveHintActive = false
        controller.updateSession(updated)
        checkSolved(updated)
    }

    fun onDragPaint(cells: List<Pair<Int, Int>>) {
        val current = session ?: return
        if (cells.isEmpty()) return
        val value = if (inputMode == NonogramInputMode.FILL) 1 else 0
        val updated = applyNonogramPaint(current, cells, value) ?: return
        nextMoveHint = null
        nextMoveHintActive = false
        controller.updateSession(updated)
        checkSolved(updated)
    }

    fun toggleNextMoveHint() {
        if (controller.isFinalized) return
        if (nextMoveHintActive) {
            nextMoveHintActive = false
            nextMoveHint = null
            return
        }
        val current = session ?: return
        nextMoveHintActive = true
        nextMoveHint = getNonogramNextMoveHint(current.puzzle, current.board)
    }

    private fun checkSolved(current: NonogramSession) {
        if (isNonogramSolved(current.board, current.solution)) {
            controller.finishAsWin()
        }
    }

    fun endPuzzle() = controller.endPuzzle()
}
