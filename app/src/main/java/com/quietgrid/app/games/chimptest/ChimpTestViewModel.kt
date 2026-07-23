package com.quietgrid.app.games.chimptest

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.ActiveSessionEnvelope
import com.quietgrid.app.data.SessionRepository
import com.quietgrid.app.data.StatsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
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

class ChimpTestPlayViewModel(
    private val sessionRepository: SessionRepository,
    private val statsRepository: StatsRepository,
    private val requestedDifficulty: Difficulty,
    private val resume: Boolean,
) : ViewModel() {

    var session by mutableStateOf<ChimpTestSession?>(null)
        private set
    var elapsedSeconds by mutableStateOf(0.0)
        private set

    // The session being played may differ from requestedDifficulty when resuming
    // (the restored session carries its own puzzle.difficulty); stats must record
    // against that actual difficulty, not the route argument used to launch this screen.
    private var difficulty: Difficulty = requestedDifficulty

    private var finalized = false
    private val _result = MutableSharedFlow<ChimpTestResult>(extraBufferCapacity = 1)
    val result: SharedFlow<ChimpTestResult> = _result

    init {
        viewModelScope.launch {
            session = if (resume) restoreOrCreate() else createChimpTestSession(requestedDifficulty)
            runTicker()
        }
    }

    private suspend fun restoreOrCreate(): ChimpTestSession {
        val envelope = sessionRepository.activeSession.first()
        if (envelope != null && envelope.gameId == GameId.CHIMPTEST.key) {
            val restored = runCatching { json.decodeFromString<ChimpTestSession>(envelope.payload) }.getOrNull()
            if (restored != null) {
                elapsedSeconds = envelope.elapsedSeconds
                difficulty = Difficulty.fromKey(restored.puzzle.difficulty)
                return restored.copy(
                    cells = generateChimpTestCells(restored.currentCount, restored.puzzle.gridSize),
                    nextExpected = 1,
                    revealAll = false,
                    wrongTapCell = null,
                    roundStartElapsed = envelope.elapsedSeconds,
                )
            }
        }
        return createChimpTestSession(requestedDifficulty)
    }

    private suspend fun runTicker() {
        while (true) {
            delay(1000)
            val current = session ?: continue
            if (finalized || current.status != ChimpTestStatus.PLAYING) continue
            elapsedSeconds += 1.0
            persistIfMeaningful()
        }
    }

    fun onCellTap(row: Int, col: Int) {
        val current = session ?: return
        if (finalized || current.status != ChimpTestStatus.PLAYING || current.revealAll) return

        val outcome = runChimpTestAction(current, row, col, elapsedSeconds)
        if (!outcome.changed) return
        session = outcome.session

        if (outcome.effects.any { it is ChimpTestEffect.WrongTap }) {
            viewModelScope.launch {
                delay(WRONG_TAP_REVEAL_MS)
                if (!finalized) finishAsLoss("rule-failure")
            }
            return
        }

        if (outcome.session.status == ChimpTestStatus.WON) {
            finishAsWin()
            return
        }

        persistIfMeaningful()
    }

    fun endPuzzle() {
        if (finalized) return
        finishAsLoss("abandoned")
    }

    private fun finishAsWin() {
        if (finalized) return
        finalized = true
        val current = session ?: return
        val score = chimpTestScore(current)
        viewModelScope.launch {
            val previous = statsRepository.statsFor(GameId.CHIMPTEST).first().forDifficulty(difficulty)
            statsRepository.recordResult(GameId.CHIMPTEST, difficulty, solved = true, score = score)
            sessionRepository.clear()
            _result.emit(
                ChimpTestResult(
                    difficulty = difficulty,
                    solved = true,
                    score = score,
                    elapsedSeconds = elapsedSeconds.toInt(),
                    lossReason = null,
                    isFirstSolve = previous.solved == 0,
                    isNewHighScore = previous.solved > 0 && score > previous.bestScore,
                ),
            )
        }
    }

    private fun finishAsLoss(reason: String) {
        if (finalized) return
        finalized = true
        viewModelScope.launch {
            statsRepository.recordResult(GameId.CHIMPTEST, difficulty, solved = false, score = 0)
            sessionRepository.clear()
            _result.emit(
                ChimpTestResult(
                    difficulty = difficulty,
                    solved = false,
                    score = 0,
                    elapsedSeconds = elapsedSeconds.toInt(),
                    lossReason = reason,
                ),
            )
        }
    }

    private fun hasMeaningfulProgress(current: ChimpTestSession): Boolean =
        current.roundTimes.isNotEmpty() || current.nextExpected > 1

    private fun persistIfMeaningful() {
        val current = session ?: return
        if (finalized || current.status != ChimpTestStatus.PLAYING) return
        if (!hasMeaningfulProgress(current)) return
        val payload = json.encodeToString(
            current.copy(revealAll = false, wrongTapCell = null),
        )
        viewModelScope.launch {
            sessionRepository.save(
                ActiveSessionEnvelope(
                    gameId = GameId.CHIMPTEST.key,
                    elapsedSeconds = elapsedSeconds,
                    payload = payload,
                ),
            )
        }
    }
}
