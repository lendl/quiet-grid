// app/src/main/java/com/quietgrid/app/games/animaldoku/AnimalDokuViewModel.kt
package com.quietgrid.app.games.animaldoku

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.daily.selectDaily
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
import com.quietgrid.engine.animaldoku.AnimalDokuHint
import com.quietgrid.engine.animaldoku.transformed
import com.quietgrid.engine.journey.gridTransformById
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
import java.time.LocalDate

private val json = Json { ignoreUnknownKeys = true }
private const val ANIMALDOKU_BASE_SCORE = 1000
private const val ANIMALDOKU_LIFE_PENALTY = 200
private const val ANIMALDOKU_TIME_PENALTY_PER_SECOND = 2

data class AnimalDokuResult(
    val difficulty: Difficulty,
    val solved: Boolean,
    val score: Int,
    val elapsedSeconds: Int,
    val lossReason: String?,
    val isFirstSolve: Boolean = false,
    val isNewHighScore: Boolean = false,
    val analyzerSnapshot: String? = null,
    val journeySignals: JourneySignals = JourneySignals(0, 0),
)

data class AnimalDokuOpenEvent(val row: Int, val col: Int, val wasCorrect: Boolean)

internal fun animalDokuScore(livesLeft: Int, elapsedSeconds: Int): Int = maxOf(
    0,
    ANIMALDOKU_BASE_SCORE - (ANIMALDOKU_STARTING_LIVES - livesLeft) * ANIMALDOKU_LIFE_PENALTY - elapsedSeconds * ANIMALDOKU_TIME_PENALTY_PER_SECOND,
)

private fun encodeCells(cells: List<List<AnimalDokuCellState>>): List<Int> = cells.flatten().map { it.ordinal }

internal fun decodeAnimalDokuCells(flat: List<Int>, size: Int): List<List<AnimalDokuCellState>> {
    val states = AnimalDokuCellState.entries
    return List(size) { row -> List(size) { col -> states[flat[row * size + col]] } }
}

internal class AnimalDokuPuzzleAdapter(
    private val appContext: android.content.Context,
    private val historyStore: PlayHistoryStore,
) : PuzzleAdapter<AnimalDokuSession, AnimalDokuResult> {
    override val gameId: GameId = GameId.ANIMALDOKU

    override suspend fun freshSession(difficulty: Difficulty): AnimalDokuSession? {
        val recentIds = historyStore.recentlyPlayedPuzzleIds(gameId, difficulty)
        val puzzle = AnimalDokuPuzzleBank.randomPuzzle(appContext, difficulty, recentIds) ?: return null
        return createAnimalDokuSession(puzzle)
    }

    override suspend fun dailySession(difficulty: Difficulty, date: LocalDate): AnimalDokuSession? {
        val puzzle = selectDaily(AnimalDokuPuzzleBank.dailyPool(appContext, difficulty), { it.id }, gameId.key, difficulty.key, date) ?: return null
        return createAnimalDokuSession(puzzle)
    }

    override suspend fun journeySession(difficulty: Difficulty, puzzleId: String, transform: Int): AnimalDokuSession? =
        AnimalDokuPuzzleBank.puzzleById(appContext, difficulty, puzzleId)?.transformed(gridTransformById(transform))?.let(::createAnimalDokuSession)

    override fun journeySignals(session: AnimalDokuSession): JourneySignals =
        JourneySignals(session.hintsUsed, ANIMALDOKU_STARTING_LIVES - session.lives)

    override fun restoreSession(payload: String, elapsedSeconds: Double): AnimalDokuSession? = runCatching {
        val persisted = json.decodeFromString<AnimalDokuPersistedSession>(payload)
        if (persisted.status != AnimalDokuStatus.PLAYING.name) return@runCatching null
        AnimalDokuSession(
            puzzle = persisted.puzzle,
            cells = decodeAnimalDokuCells(persisted.cells, persisted.puzzle.size),
            lives = persisted.lives,
            status = AnimalDokuStatus.valueOf(persisted.status),
            hintsUsed = persisted.hintsUsed,
        )
    }.getOrNull()

    override fun difficultyOf(session: AnimalDokuSession): Difficulty = Difficulty.fromKey(session.puzzle.difficulty)

    override fun hasMeaningfulProgress(session: AnimalDokuSession): Boolean =
        session.cells.any { row -> row.any { it != AnimalDokuCellState.EMPTY } }

    override fun encode(session: AnimalDokuSession): String = json.encodeToString(
        AnimalDokuPersistedSession(
            puzzle = session.puzzle,
            cells = encodeCells(session.cells),
            lives = session.lives,
            status = session.status.name,
            hintsUsed = session.hintsUsed,
        ),
    )

    override fun puzzleIdOf(session: AnimalDokuSession): String? = session.puzzle.id

    override fun scoreOnWin(session: AnimalDokuSession, difficulty: Difficulty, elapsedSeconds: Int): Int =
        animalDokuScore(session.lives, elapsedSeconds)

    override fun buildResult(session: AnimalDokuSession?, outcome: PuzzleOutcome): AnimalDokuResult = AnimalDokuResult(
        difficulty = outcome.difficulty,
        solved = outcome.solved,
        score = outcome.score,
        elapsedSeconds = outcome.elapsedSeconds,
        lossReason = outcome.lossReason,
        isFirstSolve = outcome.isFirstSolve,
        isNewHighScore = outcome.isNewHighScore,
        analyzerSnapshot = session?.let { encode(it) },
        journeySignals = outcome.journeySignals,
    )
}

@HiltViewModel(assistedFactory = AnimalDokuPlayViewModel.Factory::class)
class AnimalDokuPlayViewModel @AssistedInject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext appContext: android.content.Context,
    sessionRepository: SessionStore,
    statsRepository: StatsStore,
    historyRepository: PlayHistoryStore,
    journeyStore: JourneyProgressStore,
    @Assisted requestedDifficulty: Difficulty,
    @Assisted resume: Boolean,
    @Assisted dailyDate: LocalDate?,
    @Assisted journey: JourneyRequest?,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(requestedDifficulty: Difficulty, resume: Boolean, dailyDate: LocalDate?, journey: JourneyRequest?): AnimalDokuPlayViewModel
    }

    private val controller = PuzzleSessionController(
        scope = viewModelScope,
        sessionStore = sessionRepository,
        statsStore = statsRepository,
        historyStore = historyRepository,
        adapter = AnimalDokuPuzzleAdapter(appContext, historyRepository),
        journeyStore = journeyStore,
    )

    val session get() = controller.session
    val elapsedSeconds get() = controller.elapsedSeconds
    val result = controller.result
    val finishing = controller.finishing
    val restarts = controller.restarts

    var lastOpenEvent by mutableStateOf<AnimalDokuOpenEvent?>(null)
        private set

    var hint by mutableStateOf<AnimalDokuHint?>(null)
        private set

    var isComputingHint by mutableStateOf(false)
        private set

    internal var hintDispatcher: CoroutineDispatcher = Dispatchers.Default

    private var hintJob: Job? = null

    private fun clearHint() {
        hintJob?.cancel()
        hintJob = null
        isComputingHint = false
        hint = null
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
            val next = withContext(hintDispatcher) { findAnimalDokuSessionHint(current) }
            isComputingHint = false
            if (controller.isFinalized || session != current) return@launch
            hint = next
            if (next != null) controller.updateSession(current.copy(hintsUsed = current.hintsUsed + 1))
        }
    }

    init {
        controller.start(requestedDifficulty, resume, dailyDate, journey)
    }

    fun onCellTap(row: Int, col: Int) {
        if (controller.isFinalized) return
        val current = session ?: return
        val next = applyAnimalDokuTap(current, row, col) ?: return
        clearHint()
        controller.updateSession(next)
    }

    fun onCellDrag(markAll: Boolean, visited: List<Pair<Int, Int>>) {
        if (controller.isFinalized) return
        val current = session ?: return
        val next = applyAnimalDokuDrag(current, markAll, visited) ?: return
        clearHint()
        controller.updateSession(next)
    }

    fun onCellDoubleTap(row: Int, col: Int) {
        if (controller.isFinalized) return
        val current = session ?: return
        val opened = applyAnimalDokuOpen(current, row, col) ?: return
        clearHint()
        lastOpenEvent = AnimalDokuOpenEvent(row, col, opened.wasCorrect)
        val stillPlaying = opened.session.status == AnimalDokuStatus.PLAYING
        controller.updateSession(opened.session, persist = stillPlaying)
        when (opened.session.status) {
            AnimalDokuStatus.WON -> controller.finishAsWin()
            AnimalDokuStatus.LOST -> controller.finishAsLoss("hearts_exhausted")
            AnimalDokuStatus.PLAYING -> Unit
        }
    }

    fun endPuzzle() {
        clearHint()
        controller.endPuzzle()
    }

    fun restartPuzzle() {
        clearHint()
        controller.restartPuzzle()
    }
}
