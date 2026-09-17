package com.quietgrid.app.games.takuzu

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietgrid.app.core.ChallengerPuzzleSolve
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameCatalog
import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.PlayHistoryStore
import com.quietgrid.app.data.PlayRecord
import com.quietgrid.app.data.StatsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

private const val CHALLENGER_TICK_INTERVAL_MS = 1000L
private const val CHALLENGER_FINISH_DELAY_MS = 450L

@HiltViewModel
class TakuzuChallengerViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val statsStore: StatsStore,
    private val historyStore: PlayHistoryStore,
) : ViewModel() {

    var session by mutableStateOf<TakuzuChallengerSession?>(null)
        private set

    var wrongMoveTrigger by mutableStateOf(0)
        private set

    private var finalized = false

    private val _result = MutableSharedFlow<TakuzuChallengerResult>(extraBufferCapacity = 1)
    val result: SharedFlow<TakuzuChallengerResult> = _result

    init {
        viewModelScope.launch {
            val firstPuzzle = TakuzuPuzzleBank.randomPuzzle(appContext, Difficulty.EASY)
            if (firstPuzzle != null) {
                session = createInitialTakuzuChallengerSession(firstPuzzle)
                runTicker()
            }
        }
    }

    fun onCellPress(row: Int, col: Int) {
        if (finalized) return
        val current = session ?: return
        val updated = applyTakuzuPressCell(current.puzzleSession, row, col) ?: return
        val lineKeys = listOf("r$row", "c$col")
        val result = applyTakuzuFinalizeValidation(updated, updated.board, lineKeys)
        val newPenalties = result.session.accuracyDrops - updated.accuracyDrops
        val livesRemaining = current.livesRemaining - newPenalties
        if (newPenalties > 0) wrongMoveTrigger++

        val withValidation = current.copy(puzzleSession = result.session, livesRemaining = livesRemaining)
        session = withValidation

        when {
            livesRemaining <= 0 -> finalizeRun(withValidation, "lives_exhausted")
            isBoardSolved(result.session.board, result.session.solution) -> onSolved(withValidation)
            else -> Unit
        }
    }

    private fun onSolved(withValidation: TakuzuChallengerSession) {
        val (nextTier, nextSolvesInTier) = takuzuChallengerTierAfterSolve(withValidation.tier, withValidation.solvesInTier)
        viewModelScope.launch {
            val nextPuzzle = TakuzuPuzzleBank.randomPuzzle(appContext, nextTier, withValidation.servedPuzzleIds)
            if (nextPuzzle == null) {
                val creditedScore = withValidation.score + takuzuScore(withValidation.tier, withValidation.secondsOnCurrentPuzzle.toInt(), withValidation.puzzleSession.accuracyDrops)
                val credited = withValidation.copy(
                    puzzlesSolved = withValidation.puzzlesSolved + 1,
                    score = creditedScore,
                    fastestSolveSeconds = takuzuChallengerFastestSolve(withValidation),
                    puzzleHistory = withValidation.puzzleHistory + ChallengerPuzzleSolve(withValidation.tier, withValidation.secondsOnCurrentPuzzle),
                )
                finalizeRun(credited, "bank_exhausted")
            } else {
                session = advanceTakuzuChallengerAfterSolve(withValidation, nextTier, nextSolvesInTier, nextPuzzle)
            }
        }
    }

    fun endRun() {
        val current = session ?: return
        finalizeRun(current, "abandoned")
    }

    private suspend fun runTicker() {
        while (true) {
            delay(CHALLENGER_TICK_INTERVAL_MS)
            if (finalized) continue
            val current = session ?: continue
            val ticked = tickTakuzuChallenger(current)
            session = ticked
            if (ticked.secondsRemaining <= 0) finalizeRun(ticked, "time_up")
        }
    }

    private fun finalizeRun(current: TakuzuChallengerSession, reason: String) {
        if (finalized) return
        finalized = true
        viewModelScope.launch {
            val previousBest = statsStore.challengerStatsFor(GameId.TAKUZU).first()
            val isNewHighScore = current.score > previousBest.bestScore
            statsStore.recordChallengerResult(GameId.TAKUZU, current.puzzlesSolved, current.score)
            if (!GameCatalog.get(GameId.TAKUZU).beta) {
                historyStore.appendRecord(
                    PlayRecord(
                        gameId = GameId.TAKUZU.key,
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
                TakuzuChallengerResult(
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
}
