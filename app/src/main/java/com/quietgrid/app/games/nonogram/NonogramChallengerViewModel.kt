package com.quietgrid.app.games.nonogram

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietgrid.app.core.ChallengerPuzzleSolve
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.StatsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val CHALLENGER_TICK_INTERVAL_MS = 1000L
private const val CHALLENGER_FINISH_DELAY_MS = 450L

@HiltViewModel
class NonogramChallengerViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val statsStore: StatsStore,
) : ViewModel() {

    var session by mutableStateOf<NonogramChallengerSession?>(null)
        private set

    var inputMode by mutableStateOf(NonogramInputMode.FILL)

    var wrongTapTrigger by mutableStateOf(0)
        private set

    private var finalized = false

    private val _result = MutableSharedFlow<NonogramChallengerResult>(extraBufferCapacity = 1)
    val result: SharedFlow<NonogramChallengerResult> = _result

    init {
        viewModelScope.launch {
            val firstPuzzle = NonogramPuzzleBank.randomPuzzle(appContext, Difficulty.EASY)
            if (firstPuzzle != null) {
                session = createInitialNonogramChallengerSession(firstPuzzle)
                runTicker()
            }
        }
    }

    fun onCellTap(row: Int, col: Int) {
        if (finalized) return
        val current = session ?: return
        val tapResult = applyNonogramChallengerTap(current, row, col, inputMode)

        if (tapResult.wasWrong) {
            wrongTapTrigger++
            val livesRemaining = current.livesRemaining - 1
            val withLoss = current.copy(livesRemaining = livesRemaining)
            session = withLoss
            if (livesRemaining <= 0) finalizeRun(withLoss, "lives_exhausted")
            return
        }

        session = tapResult.session
        if (isNonogramSolved(tapResult.session.puzzleSession.board, tapResult.session.puzzleSession.solution)) {
            onSolved(tapResult.session)
        }
    }

    private fun onSolved(withTap: NonogramChallengerSession) {
        val (nextTier, nextSolvesInTier) = nonogramChallengerTierAfterSolve(withTap.tier, withTap.solvesInTier)
        viewModelScope.launch {
            val nextPuzzle = NonogramPuzzleBank.randomPuzzle(appContext, nextTier, withTap.servedPuzzleIds)
            if (nextPuzzle == null) {
                val creditedScore = withTap.score + nonogramScore(withTap.secondsOnCurrentPuzzle.toInt())
                val credited = withTap.copy(
                    puzzlesSolved = withTap.puzzlesSolved + 1,
                    score = creditedScore,
                    fastestSolveSeconds = nonogramChallengerFastestSolve(withTap),
                    puzzleHistory = withTap.puzzleHistory + ChallengerPuzzleSolve(withTap.tier, withTap.secondsOnCurrentPuzzle),
                )
                finalizeRun(credited, "bank_exhausted")
            } else {
                session = advanceNonogramChallengerAfterSolve(withTap, nextTier, nextSolvesInTier, nextPuzzle)
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
            val ticked = tickNonogramChallenger(current)
            session = ticked
            if (ticked.secondsRemaining <= 0) finalizeRun(ticked, "time_up")
        }
    }

    private fun finalizeRun(current: NonogramChallengerSession, reason: String) {
        if (finalized) return
        finalized = true
        viewModelScope.launch {
            val previousBest = statsStore.challengerStatsFor(GameId.NONOGRAM).first()
            val isNewHighScore = current.score > previousBest.bestScore
            statsStore.recordChallengerResult(GameId.NONOGRAM, current.puzzlesSolved, current.score)
            delay(CHALLENGER_FINISH_DELAY_MS)
            _result.emit(
                NonogramChallengerResult(
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
