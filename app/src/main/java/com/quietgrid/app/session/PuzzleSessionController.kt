package com.quietgrid.app.session

import android.animation.ValueAnimator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameCatalog
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.journey.JourneyRequest
import com.quietgrid.app.core.journey.JourneySignals
import com.quietgrid.app.data.ActiveSessionEnvelope
import com.quietgrid.app.data.JourneyProgressStore
import com.quietgrid.app.data.PlayHistoryStore
import com.quietgrid.app.data.PlayRecord
import com.quietgrid.app.data.SessionStore
import com.quietgrid.app.data.StatsStore
import com.quietgrid.app.data.journey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

private const val FOLD_HANDOFF_MS = 1500L
private const val TICK_INTERVAL_MS = 1000L
private const val CHECKPOINT_INTERVAL_TICKS = 15
private const val ABANDONED_REASON = "abandoned"

enum class FinishKind { Win, Loss, Abandoned }

object FinishMotion {
    var reduced: Boolean = false
}

private fun systemAnimatorsEnabled(): Boolean =
    runCatching { ValueAnimator.areAnimatorsEnabled() }.getOrDefault(false)

class PuzzleSessionController<TSession, TResult>(
    private val scope: CoroutineScope,
    private val sessionStore: SessionStore,
    private val statsStore: StatsStore,
    private val historyStore: PlayHistoryStore,
    private val adapter: PuzzleAdapter<TSession, TResult>,
    private val isAppForeground: () -> Boolean = ProcessAppForeground()::isForeground,
    private val animationsEnabled: () -> Boolean = ::systemAnimatorsEnabled,
    private val journeyStore: JourneyProgressStore? = null,
) {
    var session by mutableStateOf<TSession?>(null)
        private set
    var elapsedSeconds by mutableStateOf(0.0)
        private set
    var dailyDate by mutableStateOf<LocalDate?>(null)
        private set
    var journey by mutableStateOf<JourneyRequest?>(null)
        private set

    val isFinalized: Boolean
        get() = finalized

    private var difficulty: Difficulty = Difficulty.EASY
    private var finalized = false

    private val _result = MutableSharedFlow<TResult>(extraBufferCapacity = 1)
    val result: SharedFlow<TResult> = _result

    private val _finishing = MutableStateFlow<FinishKind?>(null)
    val finishing: StateFlow<FinishKind?> = _finishing

    private val _restarts = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val restarts: SharedFlow<Unit> = _restarts

    fun start(requestedDifficulty: Difficulty, resume: Boolean, requestedDailyDate: LocalDate? = null, journey: JourneyRequest? = null) {
        difficulty = requestedDifficulty
        scope.launch {
            session = if (resume) {
                restoreOrCreate(requestedDifficulty, requestedDailyDate, journey)
            } else {
                forfeitStaleDaily(requestedDifficulty, requestedDailyDate)
                createFresh(requestedDifficulty, requestedDailyDate, journey)
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
        finishAsLoss(ABANDONED_REASON)
    }

    fun restartPuzzle() {
        if (finalized || session == null) return
        finalized = true
        scope.launch {
            recordLoss(ABANDONED_REASON)
            sessionStore.clear()
            _restarts.emit(Unit)
        }
    }

    fun finishAsWin() {
        if (finalized) return
        finalized = true
        val current = session ?: return
        _finishing.value = FinishKind.Win
        val score = adapter.scoreOnWin(current, difficulty, elapsedSeconds.toInt())
        scope.launch {
            val previous = statsStore.statsFor(adapter.gameId).first().forDifficulty(difficulty)
            statsStore.recordResult(adapter.gameId, difficulty, solved = true, score = score)
            val journeyStars = journey?.let { journeyStore?.recordWin(adapter.gameId, it.stopId, adapter.journeySignals(current), elapsedSeconds.toInt(), score) }
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
                        journeyStop = journey?.stopId,
                        journeyStars = journeyStars,
                    ),
                )
            }
            sessionStore.clear()
            delay(foldHandoffMs())
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
                        journeySignals = adapter.journeySignals(current),
                    ),
                ),
            )
        }
    }

    fun finishAsLoss(reason: String) {
        if (finalized) return
        finalized = true
        _finishing.value = if (reason == ABANDONED_REASON) FinishKind.Abandoned else FinishKind.Loss
        scope.launch {
            val score = recordLoss(reason)
            sessionStore.clear()
            delay(foldHandoffMs())
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
                        journeySignals = session?.let { adapter.journeySignals(it) } ?: JourneySignals(0, 0),
                    ),
                ),
            )
        }
    }

    private suspend fun recordLoss(reason: String): Int {
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
                    journeyStop = journey?.stopId,
                ),
            )
        }
        journey?.let {
            if (reason == ABANDONED_REASON) journeyStore?.recordAbandon(adapter.gameId, it.stopId) else journeyStore?.recordLoss(adapter.gameId, it.stopId)
        }
        return score
    }

    private fun foldHandoffMs(): Long = if (animationsEnabled() && !FinishMotion.reduced) FOLD_HANDOFF_MS else 0L

    private suspend fun restoreOrCreate(requestedDifficulty: Difficulty, requestedDailyDate: LocalDate?, requestedJourney: JourneyRequest?): TSession? {
        val envelope = sessionStore.activeSession.first()
        if (envelope != null && envelope.gameId == adapter.gameId.key && envelope.mode == null) {
            val restored = adapter.restoreSession(envelope.payload, envelope.elapsedSeconds)
            if (restored != null) {
                elapsedSeconds = envelope.elapsedSeconds
                difficulty = adapter.difficultyOf(restored)
                dailyDate = envelope.dailyDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                journey = envelope.journey
                return restored
            }
        }
        return createFresh(requestedDifficulty, requestedDailyDate, requestedJourney)
    }

    private suspend fun createFresh(requestedDifficulty: Difficulty, requestedDailyDate: LocalDate?, requestedJourney: JourneyRequest?): TSession? {
        dailyDate = requestedDailyDate
        journey = requestedJourney
        return when {
            requestedJourney != null -> adapter.journeySession(requestedDifficulty, requestedJourney.puzzleId, requestedJourney.transform)
            requestedDailyDate != null -> adapter.dailySession(requestedDifficulty, requestedDailyDate)
            else -> adapter.freshSession(requestedDifficulty)
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
        var ticksSinceCheckpoint = 0
        var wasForeground = isAppForeground()
        try {
            while (true) {
                delay(TICK_INTERVAL_MS)
                if (finalized || session == null) continue
                if (!isAppForeground()) {
                    if (wasForeground) persistIfMeaningful()
                    wasForeground = false
                    continue
                }
                wasForeground = true
                elapsedSeconds += 1.0
                ticksSinceCheckpoint++
                if (ticksSinceCheckpoint >= CHECKPOINT_INTERVAL_TICKS) {
                    ticksSinceCheckpoint = 0
                    persistIfMeaningful()
                }
            }
        } finally {
            withContext(NonCancellable) {
                meaningfulEnvelope()?.let { sessionStore.save(it) }
            }
        }
    }

    private fun meaningfulEnvelope(): ActiveSessionEnvelope? {
        val current = session ?: return null
        if (finalized) return null
        if (!adapter.hasMeaningfulProgress(current)) return null
        return ActiveSessionEnvelope(
            gameId = adapter.gameId.key,
            elapsedSeconds = elapsedSeconds,
            payload = adapter.encode(current),
            dailyDate = dailyDate?.toString(),
            dailyTier = dailyDate?.let { difficulty.key },
            difficulty = difficulty.key,
            journeyStopId = journey?.stopId,
            journeyPuzzleId = journey?.puzzleId,
            journeyTransform = journey?.transform ?: 0,
        )
    }

    private fun persistIfMeaningful() {
        val envelope = meaningfulEnvelope() ?: return
        scope.launch { sessionStore.save(envelope) }
    }
}
