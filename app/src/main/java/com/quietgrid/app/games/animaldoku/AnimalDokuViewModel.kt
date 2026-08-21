// app/src/main/java/com/quietgrid/app/games/animaldoku/AnimalDokuViewModel.kt
package com.quietgrid.app.games.animaldoku

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
import com.quietgrid.app.session.PuzzleAdapter
import com.quietgrid.app.session.PuzzleOutcome
import com.quietgrid.app.session.PuzzleSessionController
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
)

data class AnimalDokuOpenEvent(val row: Int, val col: Int, val wasCorrect: Boolean)

private fun animalDokuScore(livesLeft: Int, elapsedSeconds: Int): Int = maxOf(
    0,
    ANIMALDOKU_BASE_SCORE - (ANIMALDOKU_STARTING_LIVES - livesLeft) * ANIMALDOKU_LIFE_PENALTY - elapsedSeconds * ANIMALDOKU_TIME_PENALTY_PER_SECOND,
)

private fun encodeCells(cells: List<List<AnimalDokuCellState>>): List<Int> = cells.flatten().map { it.ordinal }

private fun decodeCells(flat: List<Int>, size: Int): List<List<AnimalDokuCellState>> {
    val states = AnimalDokuCellState.entries
    return List(size) { row -> List(size) { col -> states[flat[row * size + col]] } }
}

private class AnimalDokuPuzzleAdapter(private val appContext: android.content.Context) : PuzzleAdapter<AnimalDokuSession, AnimalDokuResult> {
    override val gameId: GameId = GameId.ANIMALDOKU

    override suspend fun freshSession(difficulty: Difficulty): AnimalDokuSession? {
        val puzzle = AnimalDokuPuzzleBank.randomPuzzle(appContext, difficulty) ?: return null
        return createAnimalDokuSession(puzzle)
    }

    override fun restoreSession(payload: String, elapsedSeconds: Double): AnimalDokuSession? = runCatching {
        val persisted = json.decodeFromString<AnimalDokuPersistedSession>(payload)
        if (persisted.status != AnimalDokuStatus.PLAYING.name) return@runCatching null
        AnimalDokuSession(
            puzzle = persisted.puzzle,
            cells = decodeCells(persisted.cells, persisted.puzzle.size),
            lives = persisted.lives,
            status = AnimalDokuStatus.valueOf(persisted.status),
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
    )
}

@HiltViewModel(assistedFactory = AnimalDokuPlayViewModel.Factory::class)
class AnimalDokuPlayViewModel @AssistedInject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext appContext: android.content.Context,
    sessionRepository: SessionStore,
    statsRepository: StatsStore,
    historyRepository: PlayHistoryStore,
    @Assisted requestedDifficulty: Difficulty,
    @Assisted resume: Boolean,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(requestedDifficulty: Difficulty, resume: Boolean): AnimalDokuPlayViewModel
    }

    private val controller = PuzzleSessionController(
        scope = viewModelScope,
        sessionStore = sessionRepository,
        statsStore = statsRepository,
        historyStore = historyRepository,
        adapter = AnimalDokuPuzzleAdapter(appContext),
    )

    val session get() = controller.session
    val elapsedSeconds get() = controller.elapsedSeconds
    val result = controller.result

    var lastOpenEvent by mutableStateOf<AnimalDokuOpenEvent?>(null)
        private set

    init {
        controller.start(requestedDifficulty, resume)
    }

    fun onCellTap(row: Int, col: Int) {
        if (controller.isFinalized) return
        val current = session ?: return
        val next = applyAnimalDokuTap(current, row, col) ?: return
        controller.updateSession(next)
    }

    fun onCellDrag(markAll: Boolean, visited: List<Pair<Int, Int>>) {
        if (controller.isFinalized) return
        val current = session ?: return
        val next = applyAnimalDokuDrag(current, markAll, visited) ?: return
        controller.updateSession(next)
    }

    fun onCellDoubleTap(row: Int, col: Int) {
        if (controller.isFinalized) return
        val current = session ?: return
        val opened = applyAnimalDokuOpen(current, row, col) ?: return
        lastOpenEvent = AnimalDokuOpenEvent(row, col, opened.wasCorrect)
        val stillPlaying = opened.session.status == AnimalDokuStatus.PLAYING
        controller.updateSession(opened.session, persist = stillPlaying)
        when (opened.session.status) {
            AnimalDokuStatus.WON -> controller.finishAsWin()
            AnimalDokuStatus.LOST -> controller.finishAsLoss("hearts_exhausted")
            AnimalDokuStatus.PLAYING -> Unit
        }
    }

    fun endPuzzle() = controller.endPuzzle()
}
