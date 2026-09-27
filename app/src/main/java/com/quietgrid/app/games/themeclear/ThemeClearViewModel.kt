package com.quietgrid.app.games.themeclear

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
import com.quietgrid.app.data.SettingsRepository
import com.quietgrid.app.data.StatsStore
import com.quietgrid.app.data.ThemePreferencesRepository
import com.quietgrid.app.data.recentlyPlayedPuzzleIds
import com.quietgrid.app.session.PuzzleAdapter
import com.quietgrid.app.session.PuzzleOutcome
import com.quietgrid.app.session.PuzzleSessionController
import com.quietgrid.engine.themeclear.ThemeClearSelectionState
import com.quietgrid.engine.themeclear.ThemeClearSolver
import com.quietgrid.engine.themeclear.evaluateThemeClearSelection
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.random.Random

private val json = Json { ignoreUnknownKeys = true }

data class ThemeClearResult(
    val difficulty: Difficulty,
    val solved: Boolean,
    val score: Int,
    val elapsedSeconds: Int,
    val lossReason: String?,
    val isFirstSolve: Boolean = false,
    val isNewHighScore: Boolean = false,
    val intendedWords: List<String> = emptyList(),
)

private class ThemeClearPuzzleAdapter(
    private val appContext: Context,
    private val settingsRepository: SettingsRepository,
    private val themePreferencesRepository: ThemePreferencesRepository,
    private val historyStore: PlayHistoryStore,
) : PuzzleAdapter<ThemeClearSession, ThemeClearResult> {
    override val gameId: GameId = GameId.THEMECLEAR

    override suspend fun freshSession(difficulty: Difficulty): ThemeClearSession? {
        val locale = currentThemeClearLocale(settingsRepository.settings.first().puzzleLanguage)
        val recentIds = historyStore.recentlyPlayedPuzzleIds(gameId, difficulty)
        val excludedThemes = themePreferencesRepository.excludedThemes(gameId, locale).first()
        val entry = ThemeClearPuzzleBank.randomPuzzle(appContext, locale, difficulty, recentIds, excludedThemes) ?: return null
        return themeClearNewSession(entry)
    }

    override fun restoreSession(payload: String, elapsedSeconds: Double): ThemeClearSession? {
        val persisted = runCatching { json.decodeFromString(ThemeClearPersistedSession.serializer(), payload) }.getOrNull() ?: return null
        return ThemeClearSession(
            puzzle = persisted.puzzle,
            columns = persisted.columns,
            foundWords = persisted.foundWords,
            selection = emptyList(),
            hintsUsed = persisted.hintsUsed,
            discoveredWords = persisted.discoveredWords,
        )
    }

    override fun difficultyOf(session: ThemeClearSession): Difficulty = Difficulty.fromKey(session.puzzle.difficulty)

    override fun hasMeaningfulProgress(session: ThemeClearSession): Boolean = themeClearHasMeaningfulProgress(session)

    override fun encode(session: ThemeClearSession): String = json.encodeToString(
        ThemeClearPersistedSession.serializer(),
        ThemeClearPersistedSession(
            puzzle = session.puzzle,
            columns = session.columns,
            foundWords = session.foundWords,
            hintsUsed = session.hintsUsed,
            discoveredWords = session.discoveredWords,
        ),
    )

    override fun puzzleIdOf(session: ThemeClearSession): String? = session.puzzle.id

    override fun scoreOnWin(session: ThemeClearSession, difficulty: Difficulty, elapsedSeconds: Int): Int =
        themeClearScore(session.hintsUsed, elapsedSeconds, session.discoveredWords)

    override fun buildResult(session: ThemeClearSession?, outcome: PuzzleOutcome): ThemeClearResult = ThemeClearResult(
        difficulty = outcome.difficulty,
        solved = outcome.solved,
        score = outcome.score,
        elapsedSeconds = outcome.elapsedSeconds,
        lossReason = outcome.lossReason,
        isFirstSolve = outcome.isFirstSolve,
        isNewHighScore = outcome.isNewHighScore,
        intendedWords = session?.puzzle?.words ?: emptyList(),
    )
}

@HiltViewModel(assistedFactory = ThemeClearPlayViewModel.Factory::class)
class ThemeClearPlayViewModel @AssistedInject constructor(
    @ApplicationContext private val appContext: Context,
    sessionRepository: SessionStore,
    statsRepository: StatsStore,
    historyRepository: PlayHistoryStore,
    settingsRepository: SettingsRepository,
    themePreferencesRepository: ThemePreferencesRepository,
    @Assisted requestedDifficulty: Difficulty,
    @Assisted resume: Boolean,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(requestedDifficulty: Difficulty, resume: Boolean): ThemeClearPlayViewModel
    }

    private val controller = PuzzleSessionController(
        scope = viewModelScope,
        sessionStore = sessionRepository,
        statsStore = statsRepository,
        historyStore = historyRepository,
        adapter = ThemeClearPuzzleAdapter(appContext, settingsRepository, themePreferencesRepository, historyRepository),
    )

    val session get() = controller.session
    val elapsedSeconds get() = controller.elapsedSeconds
    val result = controller.result

    var selectionState by mutableStateOf(ThemeClearSelectionState.EMPTY)
        private set
    var isDeadEnd by mutableStateOf(false)
        private set
    var hintTileIds by mutableStateOf<List<Int>>(emptyList())
        private set
    var acceptEvent by mutableStateOf(0)
        private set

    internal var solverDispatcher: CoroutineDispatcher = Dispatchers.Default
    internal var random: Random = Random.Default
    internal var pendingAcceptDelayMs = 2_000L

    private var solver: ThemeClearSolver? = null
    private var pendingJob: Job? = null
    private var deadEndJob: Job? = null
    private var hintJob: Job? = null
    private var evaluationJob: Job? = null

    init {
        controller.start(requestedDifficulty, resume)
    }

    private suspend fun solverFor(current: ThemeClearSession): ThemeClearSolver? {
        solver?.let { return it }
        val dictionary = ThemeClearPuzzleBank.dictionary(appContext, current.puzzle.locale, current.puzzle.themeId) ?: return null
        return ThemeClearSolver(dictionary).also { solver = it }
    }

    private fun cancelPending() {
        pendingJob?.cancel()
        pendingJob = null
    }

    private fun cancelHint() {
        hintJob?.cancel()
        hintJob = null
        hintTileIds = emptyList()
    }

    private fun cancelEvaluation() {
        evaluationJob?.cancel()
        evaluationJob = null
    }

    private fun resetTransientState() {
        cancelPending()
        cancelHint()
        cancelEvaluation()
        selectionState = ThemeClearSelectionState.EMPTY
    }

    fun onTileTap(tileId: Int) {
        val current = session ?: return
        if (controller.isFinalized) return
        cancelPending()
        cancelHint()
        cancelEvaluation()
        controller.updateSession(tcToggleTile(current, tileId), persist = false)
        evaluateSelection()
    }

    private fun evaluateSelection() {
        evaluationJob = viewModelScope.launch {
            val loaded = session ?: return@launch
            val activeSolver = solverFor(loaded) ?: return@launch
            val current = session ?: return@launch
            val state = evaluateThemeClearSelection(tcSelectedWord(current), tcUnselectedLetters(current), activeSolver.dictionary)
            selectionState = state
            when (state) {
                ThemeClearSelectionState.ACCEPT -> accept()
                ThemeClearSelectionState.PENDING -> {
                    val pendingSelection = current.selection
                    pendingJob = viewModelScope.launch {
                        delay(pendingAcceptDelayMs)
                        if (session?.selection == pendingSelection) accept()
                    }
                }
                else -> Unit
            }
        }
    }

    fun onSelectionTextTap() {
        val current = session ?: return
        if (controller.isFinalized) return
        cancelHint()
        cancelEvaluation()
        if (selectionState == ThemeClearSelectionState.PENDING) {
            cancelPending()
            accept()
        } else {
            resetTransientState()
            controller.updateSession(tcClearSelection(current), persist = false)
        }
    }

    private fun accept() {
        val current = session ?: return
        if (controller.isFinalized || current.selection.isEmpty()) return
        pendingJob = null
        cancelHint()
        val next = tcAcceptSelection(current)
        selectionState = ThemeClearSelectionState.EMPTY
        acceptEvent++
        controller.updateSession(next)
        if (tcIsCleared(next)) {
            isDeadEnd = false
            controller.finishAsWin()
            return
        }
        refreshDeadEnd()
    }

    fun onRemoveFoundWord(index: Int) {
        val current = session ?: return
        if (controller.isFinalized) return
        resetTransientState()
        controller.updateSession(tcRemoveFoundWord(current, index, random))
        refreshDeadEnd()
    }

    fun onShuffle() {
        val current = session ?: return
        if (controller.isFinalized) return
        resetTransientState()
        controller.updateSession(tcShuffle(current, random))
        refreshDeadEnd()
    }

    fun onHint() {
        val current = session ?: return
        if (controller.isFinalized || isDeadEnd) return
        if (hintTileIds.isNotEmpty()) {
            cancelHint()
            return
        }
        hintJob = viewModelScope.launch {
            val activeSolver = solverFor(current) ?: return@launch
            val word = withContext(solverDispatcher) { activeSolver.hintWord(tcBoardLetters(current)) } ?: return@launch
            if (session != current) return@launch
            val latest = session ?: return@launch
            hintTileIds = tcTilesForWord(latest, word)
            controller.updateSession(latest.copy(hintsUsed = latest.hintsUsed + 1))
        }
    }

    fun refreshDeadEnd() {
        deadEndJob?.cancel()
        val current = session ?: return
        deadEndJob = viewModelScope.launch {
            val activeSolver = solverFor(current) ?: return@launch
            val stuck = withContext(solverDispatcher) {
                tcBoardLetters(current).isNotEmpty() && activeSolver.dictionary.buildableWords(tcBoardLetters(current)).isEmpty()
            }
            if (session?.columns == current.columns) isDeadEnd = stuck
        }
    }

    fun endPuzzle() = controller.endPuzzle()
}
