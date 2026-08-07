package com.quietgrid.app.games.wordsearch

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
import com.quietgrid.engine.wordsearch.WSCellRef
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

data class WordSearchResult(
    val difficulty: Difficulty,
    val solved: Boolean,
    val score: Int,
    val accuracyPct: Int,
    val elapsedSeconds: Int,
    val lossReason: String?,
    val isFirstSolve: Boolean = false,
    val isNewHighScore: Boolean = false,
    val words: List<String> = emptyList(),
    val hiddenWord: String = "",
)

private class WordSearchPuzzleAdapter(private val appContext: Context) : PuzzleAdapter<WordSearchSession, WordSearchResult> {
    override val gameId: GameId = GameId.WORDSEARCH

    override suspend fun freshSession(difficulty: Difficulty): WordSearchSession? {
        val entry = WordSearchPuzzleBank.randomPuzzle(appContext, difficulty) ?: return null
        return WordSearchSession(
            puzzle = entry,
            foundWordIds = emptyList(),
            tempSelection = null,
            hiddenWordMode = false,
            hiddenWordProgress = emptyList(),
            hiddenWordSolved = false,
            accuracyDrops = 0,
        )
    }

    override fun restoreSession(payload: String, elapsedSeconds: Double): WordSearchSession? {
        val persisted = runCatching { json.decodeFromString<WordSearchPersistedSession>(payload) }.getOrNull() ?: return null
        return WordSearchSession(
            puzzle = persisted.puzzle,
            foundWordIds = persisted.foundWordIds,
            tempSelection = null,
            hiddenWordMode = persisted.hiddenWordMode,
            hiddenWordProgress = persisted.hiddenWordProgress,
            hiddenWordSolved = persisted.hiddenWordSolved,
            accuracyDrops = persisted.accuracyDrops,
        )
    }

    override fun difficultyOf(session: WordSearchSession): Difficulty = Difficulty.fromKey(session.puzzle.difficulty)

    override fun hasMeaningfulProgress(session: WordSearchSession): Boolean = wordSearchHasMeaningfulProgress(session)

    override fun encode(session: WordSearchSession): String = json.encodeToString(
        WordSearchPersistedSession(
            puzzle = session.puzzle,
            foundWordIds = session.foundWordIds,
            hiddenWordMode = session.hiddenWordMode,
            hiddenWordProgress = session.hiddenWordProgress,
            hiddenWordSolved = session.hiddenWordSolved,
            accuracyDrops = session.accuracyDrops,
        ),
    )

    override fun scoreOnWin(session: WordSearchSession, difficulty: Difficulty, elapsedSeconds: Int): Int =
        wordSearchScore(elapsedSeconds, session)

    override fun buildResult(session: WordSearchSession?, outcome: PuzzleOutcome): WordSearchResult = WordSearchResult(
        difficulty = outcome.difficulty,
        solved = outcome.solved,
        score = outcome.score,
        accuracyPct = if (outcome.solved) wordSearchAccuracyPct(session?.accuracyDrops ?: 0) else 0,
        elapsedSeconds = outcome.elapsedSeconds,
        lossReason = outcome.lossReason,
        isFirstSolve = outcome.isFirstSolve,
        isNewHighScore = outcome.isNewHighScore,
        words = session?.puzzle?.words?.map { it.word } ?: emptyList(),
        hiddenWord = session?.puzzle?.hiddenWord?.word ?: "",
    )
}

@HiltViewModel(assistedFactory = WordSearchPlayViewModel.Factory::class)
class WordSearchPlayViewModel @AssistedInject constructor(
    @ApplicationContext appContext: Context,
    sessionRepository: SessionStore,
    statsRepository: StatsStore,
    @Assisted requestedDifficulty: Difficulty,
    @Assisted resume: Boolean,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(requestedDifficulty: Difficulty, resume: Boolean): WordSearchPlayViewModel
    }

    private val controller = PuzzleSessionController(
        scope = viewModelScope,
        sessionStore = sessionRepository,
        statsStore = statsRepository,
        adapter = WordSearchPuzzleAdapter(appContext),
    )

    val session get() = controller.session
    val elapsedSeconds get() = controller.elapsedSeconds
    val result = controller.result

    var nextMoveHint by mutableStateOf<WSNextMoveHint?>(null)
        private set

    init {
        controller.start(requestedDifficulty, resume)
    }

    private fun onCommitSelection() {
        val current = session ?: return
        if (current.hiddenWordMode) return
        val updated = wsCommitSelection(current) ?: return
        controller.updateSession(updated)
    }

    /**
     * Tap-to-select: tapping a cell already in the active selection clears it; tapping with no
     * selection starts one; tapping again tries to extend the selection in a straight line and
     * commits immediately if it lines up, otherwise starts a fresh selection at the tapped cell.
     * This is the fallback path for a gesture that never left its starting cell — a continuous
     * drag across cells is handled instead by [onCellDragStart]/[onCellDragMove]/[onCellDragEnd].
     */
    fun onCellTap(row: Int, col: Int) {
        val current = session ?: return
        if (current.hiddenWordMode) {
            onHiddenWordCellTap(row, col)
            return
        }
        nextMoveHint = null
        val cell = WSCellRef(row, col)
        val existingSelection = current.tempSelection
        if (existingSelection != null && cell in existingSelection.path) {
            controller.updateSession(wsClearSelection(current) ?: current, persist = false)
            return
        }
        if (existingSelection == null) {
            val started = wsBeginSelection(current, cell) ?: return
            controller.updateSession(started, persist = false)
            return
        }
        val updated = wsUpdateSelection(current, cell)
        if (updated != null) {
            controller.updateSession(updated, persist = false)
            onCommitSelection()
        } else {
            val started = wsBeginSelection(current, cell) ?: return
            controller.updateSession(started, persist = false)
        }
    }

    /**
     * Drag-to-select: continuous press-and-drag across cells, as an alternative to tap-to-select.
     * Starts a fresh selection at the drag's origin cell, extends it live as the finger crosses
     * new cells (only while they stay in a straight line from the origin), and commits on release.
     */
    fun onCellDragStart(row: Int, col: Int) {
        val current = session ?: return
        if (current.hiddenWordMode) return
        nextMoveHint = null
        val started = wsBeginSelection(current, WSCellRef(row, col)) ?: return
        controller.updateSession(started, persist = false)
    }

    fun onCellDragMove(row: Int, col: Int) {
        val current = session ?: return
        if (current.hiddenWordMode) return
        val updated = wsUpdateSelection(current, WSCellRef(row, col)) ?: return
        controller.updateSession(updated, persist = false)
    }

    fun onCellDragEnd() {
        onCommitSelection()
    }

    fun onHiddenWordCellTap(row: Int, col: Int) {
        val current = session ?: return
        nextMoveHint = null
        val updated = wsInputHiddenWordCell(current, WSCellRef(row, col)) ?: return
        controller.updateSession(updated)
        if (updated.hiddenWordSolved) controller.finishAsWin()
    }

    fun onToggleHiddenWordMode() {
        val current = session ?: return
        if (current.hiddenWordSolved) return
        nextMoveHint = null
        controller.updateSession(wsToggleHiddenWordMode(current) ?: current, persist = false)
    }

    fun toggleNextMoveHint() {
        if (controller.isFinalized) return
        if (nextMoveHint != null) {
            nextMoveHint = null
            return
        }
        val current = session ?: return
        nextMoveHint = wsNextMoveHint(current)
    }

    fun endPuzzle() = controller.endPuzzle()
}
