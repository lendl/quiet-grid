package com.quietgrid.app.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.quietgrid.app.core.ChallengerPuzzleSolve
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameCatalog
import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.PlayHistoryStore
import com.quietgrid.app.data.PlayRecord
import com.quietgrid.app.data.StatsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val CHALLENGER_TICK_INTERVAL_MS = 1000L
private const val CHALLENGER_FINISH_DELAY_MS = 450L

interface ChallengerRunState {
    val tier: Difficulty
    val solvesInTier: Int
    val puzzlesSolved: Int
    val score: Int
    val secondsRemaining: Double
    val fastestSolveSeconds: Double?
    val puzzleHistory: List<ChallengerPuzzleSolve>
}

data class ChallengerResult(
    val puzzlesSolved: Int,
    val tierReached: Difficulty,
    val score: Int,
    val isNewHighScore: Boolean,
    val reason: String,
    val previousBest: Int,
    val fastestSolveSeconds: Double?,
    val puzzleHistory: List<ChallengerPuzzleSolve>,
    val solvesInTier: Int,
)

class ChallengerRunController<TSession : ChallengerRunState>(
    private val scope: CoroutineScope,
    private val gameId: GameId,
    private val statsStore: StatsStore,
    private val historyStore: PlayHistoryStore,
    private val appForeground: AppForeground,
    private val tick: (TSession) -> TSession,
) {
    var session by mutableStateOf<TSession?>(null)

    val isFinalized: Boolean
        get() = finalized

    private var finalized = false

    private val _result = MutableSharedFlow<ChallengerResult>(extraBufferCapacity = 1)
    val result: SharedFlow<ChallengerResult> = _result

    fun start(initial: TSession) {
        session = initial
        scope.launch { runTicker() }
    }

    fun endRun() {
        val current = session ?: return
        finalizeRun(current, "abandoned")
    }

    fun finalizeRun(current: TSession, reason: String) {
        if (finalized) return
        finalized = true
        scope.launch {
            val previousBest = statsStore.challengerStatsFor(gameId).first()
            val isNewHighScore = current.score > previousBest.bestScore
            statsStore.recordChallengerResult(gameId, current.puzzlesSolved, current.score)
            if (!GameCatalog.get(gameId).beta) {
                historyStore.appendRecord(
                    PlayRecord(
                        gameId = gameId.key,
                        difficulty = current.tier.key,
                        puzzleId = null,
                        solved = true,
                        score = current.score,
                        elapsedSeconds = current.puzzleHistory.sumOf { it.elapsedSeconds }.roundToInt(),
                        timestampMillis = System.currentTimeMillis(),
                        lossReason = reason,
                        isChallenger = true,
                        puzzlesSolved = current.puzzlesSolved,
                    ),
                )
            }
            delay(CHALLENGER_FINISH_DELAY_MS)
            _result.emit(
                ChallengerResult(
                    puzzlesSolved = current.puzzlesSolved,
                    tierReached = current.tier,
                    score = current.score,
                    isNewHighScore = isNewHighScore,
                    reason = reason,
                    previousBest = previousBest.bestScore,
                    fastestSolveSeconds = current.fastestSolveSeconds,
                    puzzleHistory = current.puzzleHistory,
                    solvesInTier = current.solvesInTier,
                ),
            )
        }
    }

    private suspend fun runTicker() {
        while (true) {
            delay(CHALLENGER_TICK_INTERVAL_MS)
            if (finalized || !appForeground.isForeground()) continue
            val current = session ?: continue
            val ticked = tick(current)
            session = ticked
            if (ticked.secondsRemaining <= 0) finalizeRun(ticked, "time_up")
        }
    }
}
