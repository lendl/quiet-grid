package com.quietgrid.app.games.battleship

import android.content.Context
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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

data class BattleshipResult(
    val difficulty: Difficulty,
    val solved: Boolean,
    val score: Int,
    val elapsedSeconds: Int,
    val lossReason: String?,
    val isFirstSolve: Boolean,
    val isNewHighScore: Boolean,
)

private class BattleshipPuzzleAdapter(
    private val appContext: Context,
    private val historyStore: PlayHistoryStore,
) : PuzzleAdapter<BattleshipSession, BattleshipResult> {
    override val gameId: GameId = GameId.BATTLESHIP

    override suspend fun freshSession(difficulty: Difficulty): BattleshipSession? {
        val recentIds = historyStore.recentlyPlayedPuzzleIds(gameId, difficulty)
        val puzzle = BattleshipPuzzleBank.randomPuzzle(appContext, difficulty, recentIds) ?: return null
        return createBattleshipSession(puzzle)
    }

    override fun restoreSession(payload: String, elapsedSeconds: Double): BattleshipSession? {
        val persisted = runCatching { json.decodeFromString<BattleshipPersistedSession>(payload) }.getOrNull() ?: return null
        val fresh = createBattleshipSession(persisted.puzzle)
        return fresh.copy(board = decodeBattleshipBoard(persisted.board, persisted.puzzle.size))
    }

    override fun difficultyOf(session: BattleshipSession): Difficulty = Difficulty.fromKey(session.puzzle.difficulty)
    override fun hasMeaningfulProgress(session: BattleshipSession): Boolean = battleshipHasMeaningfulProgress(session)
    override fun encode(session: BattleshipSession): String =
        json.encodeToString(BattleshipPersistedSession(puzzle = session.puzzle, board = encodeBattleshipBoard(session.board)))
    override fun puzzleIdOf(session: BattleshipSession): String? = session.puzzle.id
    override fun scoreOnWin(session: BattleshipSession, difficulty: Difficulty, elapsedSeconds: Int): Int = battleshipScore(difficulty, elapsedSeconds)
    override fun buildResult(session: BattleshipSession?, outcome: PuzzleOutcome): BattleshipResult = BattleshipResult(
        difficulty = outcome.difficulty,
        solved = outcome.solved,
        score = outcome.score,
        elapsedSeconds = outcome.elapsedSeconds,
        lossReason = outcome.lossReason,
        isFirstSolve = outcome.isFirstSolve,
        isNewHighScore = outcome.isNewHighScore,
    )
}

@HiltViewModel(assistedFactory = BattleshipPlayViewModel.Factory::class)
class BattleshipPlayViewModel @AssistedInject constructor(
    @ApplicationContext appContext: Context,
    sessionRepository: SessionStore,
    statsRepository: StatsStore,
    historyRepository: PlayHistoryStore,
    @Assisted requestedDifficulty: Difficulty,
    @Assisted resume: Boolean,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(requestedDifficulty: Difficulty, resume: Boolean): BattleshipPlayViewModel
    }

    private val controller = PuzzleSessionController(
        scope = viewModelScope,
        sessionStore = sessionRepository,
        statsStore = statsRepository,
        historyStore = historyRepository,
        adapter = BattleshipPuzzleAdapter(appContext, historyRepository),
    )

    val session get() = controller.session
    val elapsedSeconds get() = controller.elapsedSeconds
    val result = controller.result

    init {
        controller.start(requestedDifficulty, resume)
    }

    fun pressCell(row: Int, col: Int) {
        val current = controller.session ?: return
        val next = applyBattleshipPressCell(current, row, col)
        controller.updateSession(next)
        if (battleshipIsSolved(next)) controller.finishAsWin()
    }

    fun endPuzzle() = controller.endPuzzle()
}
