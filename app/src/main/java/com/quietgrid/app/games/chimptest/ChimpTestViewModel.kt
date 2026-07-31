package com.quietgrid.app.games.chimptest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.SessionStore
import com.quietgrid.app.data.StatsStore
import com.quietgrid.app.session.PuzzleAdapter
import com.quietgrid.app.session.PuzzleOutcome
import com.quietgrid.app.session.PuzzleSessionController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }
private const val WRONG_TAP_REVEAL_MS = 700L

data class ChimpTestResult(
    val difficulty: Difficulty,
    val solved: Boolean,
    val score: Int,
    val elapsedSeconds: Int,
    val lossReason: String?,
    val isFirstSolve: Boolean = false,
    val isNewHighScore: Boolean = false,
)

private class ChimpTestPuzzleAdapter : PuzzleAdapter<ChimpTestSession, ChimpTestResult> {
    override val gameId: GameId = GameId.CHIMPTEST

    override suspend fun freshSession(difficulty: Difficulty): ChimpTestSession =
        createChimpTestSession(difficulty)

    override fun restoreSession(payload: String, elapsedSeconds: Double): ChimpTestSession? {
        val restored = runCatching { json.decodeFromString<ChimpTestSession>(payload) }.getOrNull() ?: return null
        return restored.copy(
            cells = generateChimpTestCells(restored.currentCount, restored.puzzle.gridSize),
            nextExpected = 1,
            revealAll = false,
            wrongTapCell = null,
            roundStartElapsed = elapsedSeconds,
        )
    }

    override fun difficultyOf(session: ChimpTestSession): Difficulty = Difficulty.fromKey(session.puzzle.difficulty)

    override fun hasMeaningfulProgress(session: ChimpTestSession): Boolean =
        session.roundTimes.isNotEmpty() || session.nextExpected > 1

    override fun encode(session: ChimpTestSession): String =
        json.encodeToString(session.copy(revealAll = false, wrongTapCell = null))

    override fun scoreOnWin(session: ChimpTestSession, difficulty: Difficulty, elapsedSeconds: Int): Int =
        chimpTestScore(session)

    override fun buildResult(session: ChimpTestSession?, outcome: PuzzleOutcome): ChimpTestResult = ChimpTestResult(
        difficulty = outcome.difficulty,
        solved = outcome.solved,
        score = outcome.score,
        elapsedSeconds = outcome.elapsedSeconds,
        lossReason = outcome.lossReason,
        isFirstSolve = outcome.isFirstSolve,
        isNewHighScore = outcome.isNewHighScore,
    )
}

class ChimpTestPlayViewModel(
    sessionRepository: SessionStore,
    statsRepository: StatsStore,
    requestedDifficulty: Difficulty,
    resume: Boolean,
) : ViewModel() {

    private val controller = PuzzleSessionController(
        scope = viewModelScope,
        sessionStore = sessionRepository,
        statsStore = statsRepository,
        adapter = ChimpTestPuzzleAdapter(),
    )

    val session get() = controller.session
    val elapsedSeconds get() = controller.elapsedSeconds
    val result = controller.result

    init {
        controller.start(requestedDifficulty, resume)
    }

    fun onCellTap(row: Int, col: Int) {
        val current = session ?: return
        if (current.status != ChimpTestStatus.PLAYING || current.revealAll) return

        val outcome = runChimpTestAction(current, row, col, elapsedSeconds)
        if (!outcome.changed) return

        if (outcome.effects.any { it is ChimpTestEffect.WrongTap }) {
            controller.updateSession(outcome.session, persist = false)
            viewModelScope.launch {
                delay(WRONG_TAP_REVEAL_MS)
                controller.finishAsLoss("rule-failure")
            }
            return
        }

        if (outcome.session.status == ChimpTestStatus.WON) {
            controller.updateSession(outcome.session, persist = false)
            controller.finishAsWin()
            return
        }

        controller.updateSession(outcome.session)
    }

    fun endPuzzle() = controller.endPuzzle()
}
