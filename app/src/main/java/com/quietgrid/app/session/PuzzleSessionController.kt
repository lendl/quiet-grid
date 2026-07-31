package com.quietgrid.app.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.data.ActiveSessionEnvelope
import com.quietgrid.app.data.SessionStore
import com.quietgrid.app.data.StatsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Lets the last move's feedback finish playing before the win/loss screen cuts in. */
private const val FINISH_TRANSITION_DELAY_MS = 450L
private const val TICK_INTERVAL_MS = 1000L

class PuzzleSessionController<TSession, TResult>(
    private val scope: CoroutineScope,
    private val sessionStore: SessionStore,
    private val statsStore: StatsStore,
    private val adapter: PuzzleAdapter<TSession, TResult>,
) {
    var session by mutableStateOf<TSession?>(null)
        private set
    var elapsedSeconds by mutableStateOf(0.0)
        private set

    val isFinalized: Boolean
        get() = finalized

    private var difficulty: Difficulty = Difficulty.EASY
    private var finalized = false

    private val _result = MutableSharedFlow<TResult>(extraBufferCapacity = 1)
    val result: SharedFlow<TResult> = _result

    fun start(requestedDifficulty: Difficulty, resume: Boolean) {
        difficulty = requestedDifficulty
        scope.launch {
            session = if (resume) restoreOrCreate(requestedDifficulty) else adapter.freshSession(requestedDifficulty)
            runTicker()
        }
    }

    fun updateSession(next: TSession, persist: Boolean = true) {
        if (finalized) return
        session = next
        if (persist) persistIfMeaningful()
    }

    fun endPuzzle() {
        if (finalized) return
        finishAsLoss("abandoned")
    }

    fun finishAsWin() {
        if (finalized) return
        finalized = true
        val current = session ?: return
        val score = adapter.scoreOnWin(current, difficulty, elapsedSeconds.toInt())
        scope.launch {
            val previous = statsStore.statsFor(adapter.gameId).first().forDifficulty(difficulty)
            statsStore.recordResult(adapter.gameId, difficulty, solved = true, score = score)
            sessionStore.clear()
            delay(FINISH_TRANSITION_DELAY_MS)
            _result.emit(
                adapter.buildResult(
                    current,
                    PuzzleOutcome(
                        difficulty = difficulty,
                        solved = true,
                        score = score,
                        elapsedSeconds = elapsedSeconds.toInt(),
                        lossReason = null,
                        isFirstSolve = previous.solved == 0,
                        isNewHighScore = previous.solved > 0 && score > previous.bestScore,
                    ),
                ),
            )
        }
    }

    fun finishAsLoss(reason: String) {
        if (finalized) return
        finalized = true
        scope.launch {
            statsStore.recordResult(adapter.gameId, difficulty, solved = false, score = 0)
            sessionStore.clear()
            delay(FINISH_TRANSITION_DELAY_MS)
            _result.emit(
                adapter.buildResult(
                    session,
                    PuzzleOutcome(
                        difficulty = difficulty,
                        solved = false,
                        score = 0,
                        elapsedSeconds = elapsedSeconds.toInt(),
                        lossReason = reason,
                        isFirstSolve = false,
                        isNewHighScore = false,
                    ),
                ),
            )
        }
    }

    private suspend fun restoreOrCreate(requestedDifficulty: Difficulty): TSession? {
        val envelope = sessionStore.activeSession.first()
        if (envelope != null && envelope.gameId == adapter.gameId.key) {
            val restored = adapter.restoreSession(envelope.payload, envelope.elapsedSeconds)
            if (restored != null) {
                elapsedSeconds = envelope.elapsedSeconds
                difficulty = adapter.difficultyOf(restored)
                return restored
            }
        }
        return adapter.freshSession(requestedDifficulty)
    }

    private suspend fun runTicker() {
        while (true) {
            delay(TICK_INTERVAL_MS)
            if (finalized || session == null) continue
            elapsedSeconds += 1.0
            persistIfMeaningful()
        }
    }

    private fun persistIfMeaningful() {
        val current = session ?: return
        if (finalized) return
        if (!adapter.hasMeaningfulProgress(current)) return
        val payload = adapter.encode(current)
        scope.launch {
            sessionStore.save(
                ActiveSessionEnvelope(
                    gameId = adapter.gameId.key,
                    elapsedSeconds = elapsedSeconds,
                    payload = payload,
                ),
            )
        }
    }
}
