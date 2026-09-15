package com.quietgrid.app.games.starbattle

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
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }
private const val STARBATTLE_BASE_SCORE = 1000
private const val STARBATTLE_LIFE_PENALTY = 200
private const val STARBATTLE_TIME_PENALTY_PER_SECOND = 2

data class StarBattleResult(
    val difficulty: Difficulty,
    val solved: Boolean,
    val score: Int,
    val elapsedSeconds: Int,
    val lossReason: String?,
    val isFirstSolve: Boolean = false,
    val isNewHighScore: Boolean = false,
)

data class StarBattleOpenEvent(val row: Int, val col: Int, val wasCorrect: Boolean)

internal fun starBattleScore(livesLeft: Int, elapsedSeconds: Int): Int = maxOf(
    0,
    STARBATTLE_BASE_SCORE - (STARBATTLE_STARTING_LIVES - livesLeft) * STARBATTLE_LIFE_PENALTY - elapsedSeconds * STARBATTLE_TIME_PENALTY_PER_SECOND,
)

private fun encodeCells(cells: List<List<StarBattleCellState>>): List<Int> = cells.flatten().map { it.ordinal }

private fun decodeCells(flat: List<Int>, size: Int): List<List<StarBattleCellState>> {
    val states = StarBattleCellState.entries
    return List(size) { row -> List(size) { col -> states[flat[row * size + col]] } }
}

private class StarBattlePuzzleAdapter(
    private val appContext: android.content.Context,
    private val historyStore: PlayHistoryStore,
) : PuzzleAdapter<StarBattleSession, StarBattleResult> {
    override val gameId: GameId = GameId.STARBATTLE

    override suspend fun freshSession(difficulty: Difficulty): StarBattleSession? {
        val recentIds = historyStore.recentlyPlayedPuzzleIds(gameId, difficulty)
        val puzzle = StarBattlePuzzleBank.randomPuzzle(appContext, difficulty, recentIds) ?: return null
        return createStarBattleSession(puzzle)
    }

    override fun restoreSession(payload: String, elapsedSeconds: Double): StarBattleSession? = runCatching {
        val persisted = json.decodeFromString<StarBattlePersistedSession>(payload)
        if (persisted.status != StarBattleStatus.PLAYING.name) return@runCatching null
        StarBattleSession(
            puzzle = persisted.puzzle,
            cells = decodeCells(persisted.cells, persisted.puzzle.size),
            lives = persisted.lives,
            status = StarBattleStatus.valueOf(persisted.status),
        )
    }.getOrNull()

    override fun difficultyOf(session: StarBattleSession): Difficulty = Difficulty.fromKey(session.puzzle.difficulty)

    override fun hasMeaningfulProgress(session: StarBattleSession): Boolean =
        session.cells.any { row -> row.any { it != StarBattleCellState.EMPTY } }

    override fun encode(session: StarBattleSession): String = json.encodeToString(
        StarBattlePersistedSession(
            puzzle = session.puzzle,
            cells = encodeCells(session.cells),
            lives = session.lives,
            status = session.status.name,
        ),
    )

    override fun puzzleIdOf(session: StarBattleSession): String? = session.puzzle.id

    override fun scoreOnWin(session: StarBattleSession, difficulty: Difficulty, elapsedSeconds: Int): Int =
        starBattleScore(session.lives, elapsedSeconds)

    override fun buildResult(session: StarBattleSession?, outcome: PuzzleOutcome): StarBattleResult = StarBattleResult(
        difficulty = outcome.difficulty,
        solved = outcome.solved,
        score = outcome.score,
        elapsedSeconds = outcome.elapsedSeconds,
        lossReason = outcome.lossReason,
        isFirstSolve = outcome.isFirstSolve,
        isNewHighScore = outcome.isNewHighScore,
    )
}

@HiltViewModel(assistedFactory = StarBattlePlayViewModel.Factory::class)
class StarBattlePlayViewModel @AssistedInject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext appContext: android.content.Context,
    sessionRepository: SessionStore,
    statsRepository: StatsStore,
    historyRepository: PlayHistoryStore,
    @Assisted requestedDifficulty: Difficulty,
    @Assisted resume: Boolean,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(requestedDifficulty: Difficulty, resume: Boolean): StarBattlePlayViewModel
    }

    private val controller = PuzzleSessionController(
        scope = viewModelScope,
        sessionStore = sessionRepository,
        statsStore = statsRepository,
        historyStore = historyRepository,
        adapter = StarBattlePuzzleAdapter(appContext, historyRepository),
    )

    val session get() = controller.session
    val elapsedSeconds get() = controller.elapsedSeconds
    val result = controller.result

    var lastOpenEvent by mutableStateOf<StarBattleOpenEvent?>(null)
        private set

    init {
        controller.start(requestedDifficulty, resume)
    }

    fun onCellTap(row: Int, col: Int) {
        if (controller.isFinalized) return
        val current = session ?: return
        val next = applyStarBattleTap(current, row, col) ?: return
        controller.updateSession(next)
    }

    fun onCellDrag(markAll: Boolean, visited: List<Pair<Int, Int>>) {
        if (controller.isFinalized) return
        val current = session ?: return
        val next = applyStarBattleDrag(current, markAll, visited) ?: return
        controller.updateSession(next)
    }

    fun onCellDoubleTap(row: Int, col: Int) {
        if (controller.isFinalized) return
        val current = session ?: return
        val opened = applyStarBattleOpen(current, row, col) ?: return
        lastOpenEvent = StarBattleOpenEvent(row, col, opened.wasCorrect)
        val stillPlaying = opened.session.status == StarBattleStatus.PLAYING
        controller.updateSession(opened.session, persist = stillPlaying)
        when (opened.session.status) {
            StarBattleStatus.WON -> controller.finishAsWin()
            StarBattleStatus.LOST -> controller.finishAsLoss("hearts_exhausted")
            StarBattleStatus.PLAYING -> Unit
        }
    }

    fun endPuzzle() = controller.endPuzzle()
}
