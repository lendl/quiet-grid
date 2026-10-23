package com.quietgrid.app.games.links

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
import com.quietgrid.engine.links.LinksStroke
import com.quietgrid.engine.links.extendLinksStroke
import com.quietgrid.engine.links.linksStrokePaths
import com.quietgrid.engine.links.startLinksStroke
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

data class LinksResult(
    val difficulty: Difficulty,
    val solved: Boolean,
    val score: Int,
    val accuracyPct: Int,
    val elapsedSeconds: Int,
    val lossReason: String?,
    val isFirstSolve: Boolean = false,
    val isNewHighScore: Boolean = false,
)

private class LinksPuzzleAdapter(
    private val appContext: android.content.Context,
    private val historyStore: PlayHistoryStore,
) : PuzzleAdapter<LinksSession, LinksResult> {
    override val gameId: GameId = GameId.LINKS

    override suspend fun freshSession(difficulty: Difficulty): LinksSession? {
        val recentIds = historyStore.recentlyPlayedPuzzleIds(gameId, difficulty)
        val puzzle = LinksPuzzleBank.randomPuzzle(appContext, difficulty, recentIds) ?: return null
        return createLinksSession(puzzle)
    }

    override fun restoreSession(payload: String, elapsedSeconds: Double): LinksSession? = runCatching {
        val persisted = json.decodeFromString<LinksPersistedSession>(payload)
        if (persisted.status != LinksStatus.PLAYING.name) return@runCatching null
        LinksSession(
            puzzle = persisted.puzzle,
            paths = persisted.paths,
            moves = persisted.moves,
            lastColor = persisted.lastColor,
            hintsUsed = persisted.hintsUsed,
            status = LinksStatus.PLAYING,
        ).takeIf { isRestorableLinksSession(it) }
    }.getOrNull()

    override fun difficultyOf(session: LinksSession): Difficulty = Difficulty.fromKey(session.puzzle.difficulty)

    override fun hasMeaningfulProgress(session: LinksSession): Boolean = session.paths.any { it.isNotEmpty() }

    override fun encode(session: LinksSession): String = json.encodeToString(
        LinksPersistedSession(
            puzzle = session.puzzle,
            paths = session.paths,
            moves = session.moves,
            lastColor = session.lastColor,
            hintsUsed = session.hintsUsed,
            status = session.status.name,
        ),
    )

    override fun puzzleIdOf(session: LinksSession): String? = session.puzzle.id

    override fun scoreOnWin(session: LinksSession, difficulty: Difficulty, elapsedSeconds: Int): Int =
        linksScore(session, elapsedSeconds)

    override fun buildResult(session: LinksSession?, outcome: PuzzleOutcome): LinksResult = LinksResult(
        difficulty = outcome.difficulty,
        solved = outcome.solved,
        score = outcome.score,
        accuracyPct = session?.let { linksAccuracy(it) } ?: 0,
        elapsedSeconds = outcome.elapsedSeconds,
        lossReason = outcome.lossReason,
        isFirstSolve = outcome.isFirstSolve,
        isNewHighScore = outcome.isNewHighScore,
    )
}

@HiltViewModel(assistedFactory = LinksPlayViewModel.Factory::class)
class LinksPlayViewModel @AssistedInject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext appContext: android.content.Context,
    sessionRepository: SessionStore,
    statsRepository: StatsStore,
    historyRepository: PlayHistoryStore,
    @Assisted requestedDifficulty: Difficulty,
    @Assisted resume: Boolean,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(requestedDifficulty: Difficulty, resume: Boolean): LinksPlayViewModel
    }

    private val controller = PuzzleSessionController(
        scope = viewModelScope,
        sessionStore = sessionRepository,
        statsStore = statsRepository,
        historyStore = historyRepository,
        adapter = LinksPuzzleAdapter(appContext, historyRepository),
    )

    val session get() = controller.session
    val elapsedSeconds get() = controller.elapsedSeconds
    val result = controller.result

    var stroke by mutableStateOf<LinksStroke?>(null)
        private set

    val shownPaths: List<List<Int>>?
        get() = stroke?.let { linksStrokePaths(it) } ?: session?.paths

    init {
        controller.start(requestedDifficulty, resume)
    }

    fun onDragStart(cell: Int) {
        if (controller.isFinalized) return
        val current = session ?: return
        stroke = startLinksStroke(current.puzzle, current.paths, cell)
    }

    fun onDragMove(cell: Int) {
        val current = session ?: return
        val active = stroke ?: return
        stroke = extendLinksStroke(current.puzzle, active, cell)
    }

    fun onDragEnd() {
        val active = stroke ?: return
        stroke = null
        if (controller.isFinalized) return
        val current = session ?: return
        val next = commitLinksStroke(current, active)
        if (next === current) return
        val won = next.status == LinksStatus.WON
        controller.updateSession(next, persist = !won)
        if (won) controller.finishAsWin()
    }

    fun onHint() {
        if (controller.isFinalized) return
        stroke = null
        val current = session ?: return
        val next = applyLinksHint(current) ?: return
        val won = next.status == LinksStatus.WON
        controller.updateSession(next, persist = !won)
        if (won) controller.finishAsWin()
    }

    fun endPuzzle() = controller.endPuzzle()
}
