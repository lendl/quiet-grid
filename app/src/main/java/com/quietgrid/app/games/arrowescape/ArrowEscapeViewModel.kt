// app/src/main/java/com/quietgrid/app/games/arrowescape/ArrowEscapeViewModel.kt
package com.quietgrid.app.games.arrowescape

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
import com.quietgrid.app.data.SettingsRepository
import com.quietgrid.app.data.StatsStore
import com.quietgrid.app.data.isHalloweenActive
import com.quietgrid.app.data.puzzleIdsPlayedOn
import com.quietgrid.app.data.recentlyPlayedPuzzleIds
import com.quietgrid.app.session.PuzzleAdapter
import com.quietgrid.app.session.PuzzleOutcome
import com.quietgrid.app.session.PuzzleSessionController
import com.quietgrid.engine.arrowescape.ArrowEscapePiece
import com.quietgrid.engine.arrowescape.ArrowEscapePuzzleEntry
import com.quietgrid.engine.arrowescape.buildCellOwnerMap
import com.quietgrid.engine.arrowescape.toPiece
import com.quietgrid.engine.arrowescape.transformed
import com.quietgrid.engine.journey.GridTransform
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

data class ArrowEscapeResult(
    val difficulty: Difficulty,
    val solved: Boolean,
    val score: Int,
    val elapsedSeconds: Int,
    val lossReason: String?,
    val isFirstSolve: Boolean = false,
    val isNewHighScore: Boolean = false,
    val journeySignals: JourneySignals = JourneySignals(0, 0),
)

data class ArrowEscapeTapEvent(val id: Int, val pieceIndex: Int, val removed: Boolean)

private class ParsedArrowEscapePuzzle(val puzzle: ArrowEscapePuzzleEntry) {
    val pieces: List<ArrowEscapePiece> = puzzle.pieces.map { it.toPiece() }
    val ownerLookup: Map<Pair<Int, Int>, Int> = buildCellOwnerMap(pieces)
}

internal class ArrowEscapePuzzleAdapter(
    private val appContext: android.content.Context,
    private val historyStore: PlayHistoryStore,
    private val settingsRepository: SettingsRepository,
) : PuzzleAdapter<ArrowEscapeSession, ArrowEscapeResult> {
    override val gameId: GameId = GameId.ARROWESCAPE

    override suspend fun freshSession(difficulty: Difficulty): ArrowEscapeSession? {
        val puzzle = halloweenPuzzle(difficulty)
            ?: ArrowEscapePuzzleBank.randomPuzzle(appContext, difficulty, historyStore.recentlyPlayedPuzzleIds(gameId, difficulty))
            ?: return null
        return createArrowEscapeSession(puzzle)
    }

    private suspend fun halloweenPuzzle(difficulty: Difficulty): ArrowEscapePuzzleEntry? {
        val today = LocalDate.now()
        if (!settingsRepository.isHalloweenActive(today)) return null
        return ArrowEscapePuzzleBank.halloweenPuzzle(appContext, difficulty, historyStore.puzzleIdsPlayedOn(gameId, today))
    }

    override suspend fun journeySession(difficulty: Difficulty, puzzleId: String, transform: Int): ArrowEscapeSession? =
        bankPuzzle(difficulty, puzzleId, transform)?.let { createArrowEscapeSession(it, transform) }

    private suspend fun bankPuzzle(difficulty: Difficulty, puzzleId: String, transform: Int): ArrowEscapePuzzleEntry? =
        ArrowEscapePuzzleBank.puzzleById(appContext, difficulty, puzzleId)?.transformed(gridTransformById(transform))

    override fun journeySignals(session: ArrowEscapeSession): JourneySignals =
        JourneySignals(session.hintsUsed, ARROW_ESCAPE_STARTING_LIVES - session.lives)

    override suspend fun restoreSession(payload: String, elapsedSeconds: Double): ArrowEscapeSession? {
        val persisted = runCatching { json.decodeFromString<ArrowEscapePersistedSession>(payload) }.getOrNull() ?: return null
        if (persisted.status != ArrowEscapeStatus.PLAYING.name) return null
        val puzzle = persisted.puzzle ?: savedBankPuzzle(persisted) ?: return null
        val transform = if (persisted.puzzle != null) inlinePuzzleTransform(persisted.puzzle) else persisted.transform
        val pieceRange = puzzle.pieces.indices
        val indices = persisted.removedIndices + persisted.wrongIndices + listOfNotNull(persisted.selectedIndex)
        if (indices.any { it !in pieceRange }) return null
        return ArrowEscapeSession(
            puzzle = puzzle,
            removedIndices = persisted.removedIndices.toSet(),
            lives = persisted.lives,
            selectedIndex = persisted.selectedIndex,
            status = ArrowEscapeStatus.PLAYING,
            hintsUsed = persisted.hintsUsed,
            wrongIndices = persisted.wrongIndices.toSet(),
            transform = transform,
        )
    }

    private suspend fun savedBankPuzzle(persisted: ArrowEscapePersistedSession): ArrowEscapePuzzleEntry? {
        val puzzleId = persisted.puzzleId ?: return null
        val difficulty = Difficulty.entries.firstOrNull { it.key == persisted.difficulty } ?: return null
        return bankPuzzle(difficulty, puzzleId, persisted.transform)
    }

    private suspend fun inlinePuzzleTransform(puzzle: ArrowEscapePuzzleEntry): Int {
        val difficulty = Difficulty.entries.firstOrNull { it.key == puzzle.difficulty } ?: return 0
        val original = ArrowEscapePuzzleBank.puzzleById(appContext, difficulty, puzzle.id) ?: return 0
        return GridTransform.entries.firstOrNull { original.transformed(it) == puzzle }?.id ?: 0
    }

    override fun difficultyOf(session: ArrowEscapeSession): Difficulty = Difficulty.fromKey(session.puzzle.difficulty)

    override fun hasMeaningfulProgress(session: ArrowEscapeSession): Boolean = session.removedIndices.isNotEmpty()

    override fun encode(session: ArrowEscapeSession): String = json.encodeToString(
        ArrowEscapePersistedSession(
            puzzleId = session.puzzle.id,
            difficulty = session.puzzle.difficulty,
            transform = session.transform,
            removedIndices = session.removedIndices.toList(),
            lives = session.lives,
            selectedIndex = session.selectedIndex,
            status = session.status.name,
            hintsUsed = session.hintsUsed,
            wrongIndices = session.wrongIndices.toList(),
        ),
    )

    override fun puzzleIdOf(session: ArrowEscapeSession): String? = session.puzzle.id

    override fun scoreOnWin(session: ArrowEscapeSession, difficulty: Difficulty, elapsedSeconds: Int): Int = arrowEscapeScore(
        difficulty = difficulty,
        arrowCount = session.puzzle.pieces.size,
        elapsedSeconds = elapsedSeconds,
        mistakes = ARROW_ESCAPE_STARTING_LIVES - session.lives,
        hintsUsed = session.hintsUsed,
    )

    override fun buildResult(session: ArrowEscapeSession?, outcome: PuzzleOutcome): ArrowEscapeResult = ArrowEscapeResult(
        difficulty = outcome.difficulty,
        solved = outcome.solved,
        score = outcome.score,
        elapsedSeconds = outcome.elapsedSeconds,
        lossReason = outcome.lossReason,
        isFirstSolve = outcome.isFirstSolve,
        isNewHighScore = outcome.isNewHighScore,
        journeySignals = outcome.journeySignals,
    )
}

@HiltViewModel(assistedFactory = ArrowEscapePlayViewModel.Factory::class)
class ArrowEscapePlayViewModel @AssistedInject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext appContext: android.content.Context,
    sessionRepository: SessionStore,
    statsRepository: StatsStore,
    historyRepository: PlayHistoryStore,
    settingsRepository: SettingsRepository,
    journeyStore: JourneyProgressStore,
    @Assisted requestedDifficulty: Difficulty,
    @Assisted resume: Boolean,
    @Assisted journey: JourneyRequest?,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(requestedDifficulty: Difficulty, resume: Boolean, journey: JourneyRequest?): ArrowEscapePlayViewModel
    }

    private val controller = PuzzleSessionController(
        scope = viewModelScope,
        sessionStore = sessionRepository,
        statsStore = statsRepository,
        historyStore = historyRepository,
        adapter = ArrowEscapePuzzleAdapter(appContext, historyRepository, settingsRepository),
        journeyStore = journeyStore,
    )

    val session get() = controller.session
    val elapsedSeconds get() = controller.elapsedSeconds
    val result = controller.result
    val finishing = controller.finishing
    val restarts = controller.restarts

    var lastTapEvent by mutableStateOf<ArrowEscapeTapEvent?>(null)
        private set
    var isComputingHint by mutableStateOf(false)
        private set

    internal var hintDispatcher: CoroutineDispatcher = Dispatchers.Default
    private var hintJob: Job? = null
    private var tapEventSeq = 0
    private var parsedPuzzle: ParsedArrowEscapePuzzle? = null

    private fun parsedPuzzleFor(puzzle: ArrowEscapePuzzleEntry): ParsedArrowEscapePuzzle =
        parsedPuzzle?.takeIf { it.puzzle === puzzle } ?: ParsedArrowEscapePuzzle(puzzle).also { parsedPuzzle = it }

    init {
        controller.start(requestedDifficulty, resume, journey = journey)
    }

    fun onPieceTap(pieceIndex: Int) {
        if (controller.isFinalized) return
        val current = session ?: return
        val parsed = parsedPuzzleFor(current.puzzle)
        val attempt = applyArrowEscapeAttempt(current, pieceIndex, parsed.pieces, parsed.ownerLookup) ?: return
        hintJob?.cancel()
        hintJob = null
        isComputingHint = false
        lastTapEvent = ArrowEscapeTapEvent(++tapEventSeq, pieceIndex, attempt.removed)
        val stillPlaying = attempt.session.status == ArrowEscapeStatus.PLAYING
        controller.updateSession(attempt.session, persist = stillPlaying)
        when (attempt.session.status) {
            ArrowEscapeStatus.WON -> controller.finishAsWin()
            ArrowEscapeStatus.LOST -> controller.finishAsLoss("out_of_lives")
            ArrowEscapeStatus.PLAYING -> Unit
        }
    }

    fun onHint() {
        if (controller.isFinalized || isComputingHint) return
        val current = session ?: return
        isComputingHint = true
        hintJob = viewModelScope.launch {
            val next = withContext(hintDispatcher) { applyArrowEscapeHint(current) }
            isComputingHint = false
            if (next != null && !controller.isFinalized && session == current) controller.updateSession(next, persist = next.hintsUsed != current.hintsUsed)
        }
    }

    fun endPuzzle() = controller.endPuzzle()

    fun restartPuzzle() {
        hintJob?.cancel()
        hintJob = null
        isComputingHint = false
        controller.restartPuzzle()
    }
}
