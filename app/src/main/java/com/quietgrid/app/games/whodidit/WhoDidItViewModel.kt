package com.quietgrid.app.games.whodidit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.daily.selectDaily
import com.quietgrid.app.data.PlayHistoryStore
import com.quietgrid.app.data.SessionStore
import com.quietgrid.app.data.StatsStore
import com.quietgrid.app.data.recentlyPlayedPuzzleIds
import com.quietgrid.app.session.PuzzleAdapter
import com.quietgrid.app.session.PuzzleOutcome
import com.quietgrid.app.session.PuzzleSessionController
import com.quietgrid.engine.whodidit.WhoDidItPuzzleEntry
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

data class WhoDidItResult(
    val difficulty: Difficulty,
    val solved: Boolean,
    val score: Int,
    val elapsedSeconds: Int,
    val lossReason: String?,
    val isFirstSolve: Boolean = false,
    val isNewHighScore: Boolean = false,
    val reveal: WhoDidItPuzzleEntry? = null,
)

data class WhoDidItWrongPickEvent(val id: Int, val block: Int, val row: Int, val col: Int)

private class WhoDidItPuzzleAdapter(
    private val appContext: android.content.Context,
    private val historyStore: PlayHistoryStore,
) : PuzzleAdapter<WhoDidItSession, WhoDidItResult> {
    override val gameId: GameId = GameId.WHODIDIT

    override suspend fun freshSession(difficulty: Difficulty): WhoDidItSession? {
        val recentIds = historyStore.recentlyPlayedPuzzleIds(gameId, difficulty)
        val puzzle = WhoDidItPuzzleBank.randomPuzzle(appContext, difficulty, recentIds) ?: return null
        return createWhoDidItSession(puzzle)
    }

    override suspend fun dailySession(difficulty: Difficulty, date: LocalDate): WhoDidItSession? {
        val puzzle = selectDaily(WhoDidItPuzzleBank.dailyPool(appContext, difficulty), { it.id }, gameId.key, difficulty.key, date) ?: return null
        return createWhoDidItSession(puzzle)
    }

    override fun restoreSession(payload: String, elapsedSeconds: Double): WhoDidItSession? = runCatching {
        val persisted = json.decodeFromString<WhoDidItPersistedSession>(payload)
        if (persisted.status != WhoDidItStatus.PLAYING.name) return@runCatching null
        if (!isPlayable(persisted.puzzle)) return@runCatching null
        WhoDidItSession(
            puzzle = persisted.puzzle,
            blocks = decodeWhoDidItMarks(persisted.marks, persisted.puzzle),
            status = WhoDidItStatus.valueOf(persisted.status),
        )
    }.getOrNull()

    override fun difficultyOf(session: WhoDidItSession): Difficulty = Difficulty.fromKey(session.puzzle.difficulty)

    override fun hasMeaningfulProgress(session: WhoDidItSession): Boolean =
        session.blocks.any { block -> block.any { row -> row.any { it != WhoDidItCellMark.EMPTY } } }

    override fun encode(session: WhoDidItSession): String = json.encodeToString(
        WhoDidItPersistedSession(
            puzzle = session.puzzle,
            marks = encodeWhoDidItMarks(session.blocks),
            status = session.status.name,
        ),
    )

    override fun puzzleIdOf(session: WhoDidItSession): String? = session.puzzle.id

    override fun scoreOnWin(session: WhoDidItSession, difficulty: Difficulty, elapsedSeconds: Int): Int =
        whoDidItScore(elapsedSeconds)

    override fun buildResult(session: WhoDidItSession?, outcome: PuzzleOutcome): WhoDidItResult = WhoDidItResult(
        difficulty = outcome.difficulty,
        solved = outcome.solved,
        score = outcome.score,
        elapsedSeconds = outcome.elapsedSeconds,
        lossReason = outcome.lossReason,
        isFirstSolve = outcome.isFirstSolve,
        isNewHighScore = outcome.isNewHighScore,
        reveal = if (outcome.solved) session?.puzzle else null,
    )
}

@HiltViewModel(assistedFactory = WhoDidItPlayViewModel.Factory::class)
class WhoDidItPlayViewModel @AssistedInject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext appContext: android.content.Context,
    sessionRepository: SessionStore,
    statsRepository: StatsStore,
    historyRepository: PlayHistoryStore,
    @Assisted requestedDifficulty: Difficulty,
    @Assisted resume: Boolean,
    @Assisted dailyDate: LocalDate?,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(requestedDifficulty: Difficulty, resume: Boolean, dailyDate: LocalDate?): WhoDidItPlayViewModel
    }

    private val controller = PuzzleSessionController(
        scope = viewModelScope,
        sessionStore = sessionRepository,
        statsStore = statsRepository,
        historyStore = historyRepository,
        adapter = WhoDidItPuzzleAdapter(appContext, historyRepository),
    )

    val session get() = controller.session
    val elapsedSeconds get() = controller.elapsedSeconds
    val result = controller.result
    val finishing = controller.finishing

    var wrongPickEvent by mutableStateOf<WhoDidItWrongPickEvent?>(null)
        private set
    val showNotQuite: Boolean
        get() = session?.let(::whoDidItShowsNotQuite) ?: false
    var hint by mutableStateOf<WhoDidItHint?>(null)
        private set
    var isComputingHint by mutableStateOf(false)
        private set

    private val undoStack = ArrayDeque<WhoDidItSession>()
    private var hintJob: Job? = null
    private var wrongPickSeq = 0
    internal var hintDispatcher: CoroutineDispatcher = Dispatchers.Default

    init {
        controller.start(requestedDifficulty, resume, dailyDate)
    }

    private fun clearHint() {
        hintJob?.cancel()
        hintJob = null
        isComputingHint = false
        hint = null
    }

    fun onCellTap(block: Int, row: Int, col: Int) {
        if (controller.isFinalized) return
        val current = session ?: return
        val checksPicks = whoDidItChecksPicks(Difficulty.fromKey(current.puzzle.difficulty))
        val tapped = applyWhoDidItTap(current, block, row, col, checksPicks) ?: return
        if (tapped.wrongPick) {
            wrongPickEvent = WhoDidItWrongPickEvent(++wrongPickSeq, block, row, col)
            return
        }
        clearHint()
        val next = tapped.session
        undoStack.addLast(current)
        val stillPlaying = next.status == WhoDidItStatus.PLAYING
        controller.updateSession(next, persist = stillPlaying)
        if (!stillPlaying) {
            controller.finishAsWin()
        }
    }

    fun onUndo() {
        if (controller.isFinalized) return
        val previous = undoStack.removeLastOrNull() ?: return
        clearHint()
        controller.updateSession(previous)
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
            val computed = withContext(hintDispatcher) { computeWhoDidItHint(current) }
            isComputingHint = false
            if (!controller.isFinalized && session == current) hint = computed
        }
    }

    fun endPuzzle() = controller.endPuzzle()
}
