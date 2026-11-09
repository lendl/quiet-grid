package com.quietgrid.app.games.wordswap

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.quietgrid.engine.wordswap.WORDSWAP_HOLE
import com.quietgrid.engine.wordswap.WordSwapHint
import com.quietgrid.engine.wordswap.WordSwapMove
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

data class WordSwapResult(
    val difficulty: Difficulty,
    val solved: Boolean,
    val score: Int,
    val accuracyPct: Int,
    val elapsedSeconds: Int,
    val lossReason: String?,
    val isFirstSolve: Boolean = false,
    val isNewHighScore: Boolean = false,
)

private class WordSwapPuzzleAdapter(
    private val appContext: android.content.Context,
    private val historyStore: PlayHistoryStore,
) : PuzzleAdapter<WordSwapSession, WordSwapResult> {
    override val gameId: GameId = GameId.WORDSWAP

    override suspend fun freshSession(difficulty: Difficulty): WordSwapSession? {
        val recentIds = historyStore.recentlyPlayedPuzzleIds(gameId, difficulty)
        val puzzle = WordSwapPuzzleBank.randomPuzzle(appContext, difficulty, recentIds) ?: return null
        return createWordSwapSession(puzzle)
    }

    override fun restoreSession(payload: String, elapsedSeconds: Double): WordSwapSession? = runCatching {
        val persisted = json.decodeFromString<WordSwapPersistedSession>(payload)
        if (persisted.status != WordSwapStatus.PLAYING.name) return@runCatching null
        WordSwapSession(
            puzzle = persisted.puzzle,
            board = persisted.board,
            swaps = persisted.swaps,
            hintsUsed = persisted.hintsUsed,
            status = WordSwapStatus.PLAYING,
        ).takeIf { isRestorableWordSwapSession(it) }
    }.getOrNull()

    override fun difficultyOf(session: WordSwapSession): Difficulty = Difficulty.fromKey(session.puzzle.difficulty)

    override fun hasMeaningfulProgress(session: WordSwapSession): Boolean = session.swaps > 0

    override fun encode(session: WordSwapSession): String = json.encodeToString(
        WordSwapPersistedSession(
            puzzle = session.puzzle,
            board = session.board,
            swaps = session.swaps,
            hintsUsed = session.hintsUsed,
            status = session.status.name,
        ),
    )

    override fun puzzleIdOf(session: WordSwapSession): String? = session.puzzle.id

    override fun scoreOnWin(session: WordSwapSession, difficulty: Difficulty, elapsedSeconds: Int): Int =
        wordSwapScore(session, elapsedSeconds)

    override fun buildResult(session: WordSwapSession?, outcome: PuzzleOutcome): WordSwapResult = WordSwapResult(
        difficulty = outcome.difficulty,
        solved = outcome.solved,
        score = outcome.score,
        accuracyPct = session?.let { wordSwapAccuracy(it) } ?: 0,
        elapsedSeconds = outcome.elapsedSeconds,
        lossReason = outcome.lossReason,
        isFirstSolve = outcome.isFirstSolve,
        isNewHighScore = outcome.isNewHighScore,
    )
}

@HiltViewModel(assistedFactory = WordSwapPlayViewModel.Factory::class)
class WordSwapPlayViewModel @AssistedInject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext appContext: android.content.Context,
    sessionRepository: SessionStore,
    statsRepository: StatsStore,
    historyRepository: PlayHistoryStore,
    @Assisted requestedDifficulty: Difficulty,
    @Assisted resume: Boolean,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(requestedDifficulty: Difficulty, resume: Boolean): WordSwapPlayViewModel
    }

    private val controller = PuzzleSessionController(
        scope = viewModelScope,
        sessionStore = sessionRepository,
        statsStore = statsRepository,
        historyStore = historyRepository,
        adapter = WordSwapPuzzleAdapter(appContext, historyRepository),
    )

    val session get() = controller.session
    val elapsedSeconds get() = controller.elapsedSeconds
    val result = controller.result
    val finishing = controller.finishing

    var selectedCell by mutableStateOf<Int?>(null)
        private set

    var lastSwap by mutableStateOf<WordSwapMove?>(null)
        private set

    var swapSerial by mutableIntStateOf(0)
        private set

    var isComputingHint by mutableStateOf(false)
        private set

    var hint by mutableStateOf<WordSwapHint?>(null)
        private set

    private var hintJob: Job? = null

    var canUndo by mutableStateOf(false)
        private set

    internal var hintDispatcher: CoroutineDispatcher = Dispatchers.Default

    private val undoHistory = mutableListOf<WordSwapSession>()

    init {
        controller.start(requestedDifficulty, resume)
    }

    fun onTileTap(cell: Int) {
        if (controller.isFinalized || isComputingHint) return
        val current = session ?: return
        if (current.board.getOrNull(cell) == null || current.board[cell] == WORDSWAP_HOLE) return
        val selected = selectedCell
        when {
            selected == null -> selectedCell = cell
            selected == cell -> selectedCell = null
            else -> {
                selectedCell = null
                val move = WordSwapMove(selected, cell)
                val next = applyWordSwapTiles(current, move) ?: return
                clearHint()
                pushUndo(current)
                lastSwap = move
                swapSerial++
                commit(next)
            }
        }
    }

    fun onUndo() {
        if (controller.isFinalized || isComputingHint) return
        val current = session ?: return
        val previous = undoHistory.removeLastOrNull() ?: return
        canUndo = undoHistory.isNotEmpty()
        clearHint()
        selectedCell = null
        lastSwap = null
        controller.updateSession(undoWordSwap(current, previous))
    }

    fun onRestart() {
        if (controller.isFinalized || isComputingHint) return
        val current = session ?: return
        val next = restartWordSwap(current) ?: return
        clearHint()
        pushUndo(current)
        selectedCell = null
        lastSwap = null
        commit(next)
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
            val next = withContext(hintDispatcher) { findWordSwapSessionHint(current) }
            isComputingHint = false
            if (controller.isFinalized || session != current) return@launch
            hint = next
            if (next != null) controller.updateSession(current.copy(hintsUsed = current.hintsUsed + 1))
        }
    }

    private fun clearHint() {
        hintJob?.cancel()
        hintJob = null
        isComputingHint = false
        hint = null
    }

    private fun pushUndo(snapshot: WordSwapSession) {
        undoHistory += snapshot
        canUndo = true
    }

    private fun commit(next: WordSwapSession) {
        val won = next.status == WordSwapStatus.WON
        controller.updateSession(next, persist = !won)
        if (won) controller.finishAsWin()
    }

    fun endPuzzle() {
        clearHint()
        controller.endPuzzle()
    }
}
