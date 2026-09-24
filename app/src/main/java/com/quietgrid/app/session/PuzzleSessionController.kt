package com.quietgrid.app.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameCatalog
import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.ActiveSessionEnvelope
import com.quietgrid.app.data.PlayHistoryStore
import com.quietgrid.app.data.PlayRecord
import com.quietgrid.app.data.SessionStore
import com.quietgrid.app.data.StatsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val FINISH_TRANSITION_DELAY_MS = 450L
private const val TICK_INTERVAL_MS = 1000L

class PuzzleSessionController<TSession, TResult>(
    private val scope: CoroutineScope,
    private val sessionStore: SessionStore,
    private val statsStore: StatsStore,
    private val historyStore: PlayHistoryStore,
    private val adapter: PuzzleAdapter<TSession, TResult>,
    private val isAppForeground: () -> Boolean = {
        ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    },
) {
    var session by mutableStateOf<TSession?>(null)
        private set
    var elapsedSeconds by mutableStateOf(0.0)
        private set
    var dailyDate by mutableStateOf<LocalDate?>(null)
        private set

    val isFinalized: Boolean
        get() = finalized

    private var difficulty: Difficulty = Difficulty.EASY
    private var finalized = false

    private val _result = MutableSharedFlow<TResult>(extraBufferCapacity = 1)
    val result: SharedFlow<TResult> = _result

    fun start(requestedDifficulty: Difficulty, resume: Boolean, requestedDailyDate: LocalDate? = null) {
        difficulty = requestedDifficulty
        scope.launch {
            session = if (resume) {
                restoreOrCreate(requestedDifficulty, requestedDailyDate)
            } else {
                forfeitStaleDaily(requestedDifficulty, requestedDailyDate)
                createFresh(requestedDifficulty, requestedDailyDate)
            }
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
            if (!GameCatalog.get(adapter.gameId).beta) {
                historyStore.appendRecord(
                    PlayRecord(
                        gameId = adapter.gameId.key,
                        difficulty = difficulty.key,
                        puzzleId = adapter.puzzleIdOf(current),
                        solved = true,
                        score = score,
                        elapsedSeconds = elapsedSeconds.toInt(),
                        timestampMillis = System.currentTimeMillis(),
                        lossReason = null,
                        dailyDate = dailyDate?.toString(),
                        shareDetail = if (dailyDate != null) adapter.dailyShareDetail(current) else null,
                    ),
                )
            }
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
            val score = session?.let { adapter.scoreOnLoss(it, difficulty, elapsedSeconds.toInt()) } ?: 0
            statsStore.recordResult(adapter.gameId, difficulty, solved = false, score = score)
            if (!GameCatalog.get(adapter.gameId).beta) {
                historyStore.appendRecord(
                    PlayRecord(
                        gameId = adapter.gameId.key,
                        difficulty = difficulty.key,
                        puzzleId = session?.let { adapter.puzzleIdOf(it) },
                        solved = false,
                        score = score,
                        elapsedSeconds = elapsedSeconds.toInt(),
                        timestampMillis = System.currentTimeMillis(),
                        lossReason = reason,
                        dailyDate = dailyDate?.toString(),
                        shareDetail = if (dailyDate != null) session?.let { adapter.dailyShareDetail(it) } else null,
                    ),
                )
            }
            sessionStore.clear()
            delay(FINISH_TRANSITION_DELAY_MS)
            _result.emit(
                adapter.buildResult(
                    session,
                    PuzzleOutcome(
                        difficulty = difficulty,
                        solved = false,
                        score = score,
                        elapsedSeconds = elapsedSeconds.toInt(),
                        lossReason = reason,
                        isFirstSolve = false,
                        isNewHighScore = false,
                    ),
                ),
            )
        }
    }

    private suspend fun restoreOrCreate(requestedDifficulty: Difficulty, requestedDailyDate: LocalDate?): TSession? {
        val envelope = sessionStore.activeSession.first()
        if (envelope != null && envelope.gameId == adapter.gameId.key) {
            val restored = adapter.restoreSession(envelope.payload, envelope.elapsedSeconds)
            if (restored != null) {
                elapsedSeconds = envelope.elapsedSeconds
                difficulty = adapter.difficultyOf(restored)
                dailyDate = envelope.dailyDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                return restored
            }
        }
        return createFresh(requestedDifficulty, requestedDailyDate)
    }

    private suspend fun createFresh(requestedDifficulty: Difficulty, requestedDailyDate: LocalDate?): TSession? {
        dailyDate = requestedDailyDate
        return if (requestedDailyDate != null) {
            adapter.dailySession(requestedDifficulty, requestedDailyDate)
        } else {
            adapter.freshSession(requestedDifficulty)
        }
    }

    private suspend fun forfeitStaleDaily(requestedDifficulty: Difficulty, requestedDailyDate: LocalDate?) {
        val envelope = sessionStore.activeSession.first() ?: return
        val staleDate = envelope.dailyDate ?: return
        val staleTier = envelope.dailyTier ?: return
        val sameRun = envelope.gameId == adapter.gameId.key &&
            staleDate == requestedDailyDate?.toString() &&
            staleTier == requestedDifficulty.key
        if (sameRun) return
        val staleGameId = GameId.entries.firstOrNull { it.key == envelope.gameId } ?: return
        val staleDifficulty = Difficulty.entries.firstOrNull { it.key == staleTier } ?: return
        statsStore.recordResult(staleGameId, staleDifficulty, solved = false, score = 0)
        historyStore.appendRecord(
            PlayRecord(
                gameId = envelope.gameId,
                difficulty = staleTier,
                puzzleId = null,
                solved = false,
                score = 0,
                elapsedSeconds = envelope.elapsedSeconds.toInt(),
                timestampMillis = System.currentTimeMillis(),
                lossReason = "abandoned",
                dailyDate = staleDate,
            ),
        )
        sessionStore.clear()
    }

    private suspend fun runTicker() {
        while (true) {
            delay(TICK_INTERVAL_MS)
            if (finalized || session == null) continue
            if (!isAppForeground()) continue
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
                    dailyDate = dailyDate?.toString(),
                    dailyTier = dailyDate?.let { difficulty.key },
                ),
            )
        }
    }
}
