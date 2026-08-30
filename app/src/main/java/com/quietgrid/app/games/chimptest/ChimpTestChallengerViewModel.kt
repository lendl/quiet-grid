package com.quietgrid.app.games.chimptest

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.StatsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val CHALLENGER_TICK_INTERVAL_MS = 1000L
private const val CHALLENGER_FINISH_DELAY_MS = 450L
private const val CHALLENGER_SOLVE_ADVANCE_DELAY_MS = 500L
private const val CHALLENGER_WRONG_TAP_REVEAL_MS = 700L

@HiltViewModel
class ChimpTestChallengerViewModel @Inject constructor(
    private val statsStore: StatsStore,
) : ViewModel() {

    var session by mutableStateOf<ChimpTestChallengerSession?>(null)
        private set

    var correctTapTrigger by mutableStateOf(0)
        private set

    var wrongTapTrigger by mutableStateOf(0)
        private set

    private var finalized = false

    private val _result = MutableSharedFlow<ChimpTestChallengerResult>(extraBufferCapacity = 1)
    val result: SharedFlow<ChimpTestChallengerResult> = _result

    init {
        session = createInitialChimpTestChallengerSession()
        viewModelScope.launch { runTicker() }
    }

    fun onCellTap(row: Int, col: Int) {
        if (finalized) return
        val current = session ?: return
        if (current.puzzleSession.status != ChimpTestStatus.PLAYING || current.puzzleSession.revealAll) return

        val outcome = runChimpTestAction(current.puzzleSession, row, col, current.secondsOnCurrentPuzzle)
        if (!outcome.changed) return

        if (outcome.effects.any { it is ChimpTestEffect.WrongTap }) {
            val withTap = current.copy(puzzleSession = outcome.session)
            session = withTap
            wrongTapTrigger++
            val remainingLives = withTap.livesRemaining - 1
            viewModelScope.launch {
                delay(CHALLENGER_WRONG_TAP_REVEAL_MS)
                if (remainingLives <= 0) {
                    finalizeRun(withTap.copy(livesRemaining = 0), "lives_exhausted")
                } else {
                    session = advanceChimpTestChallengerAfterLoss(withTap.copy(livesRemaining = remainingLives))
                }
            }
            return
        }

        if (outcome.session.status == ChimpTestStatus.WON) {
            val withWin = current.copy(puzzleSession = outcome.session)
            session = withWin
            correctTapTrigger++
            val (nextTier, nextSolvesInTier) = chimpTestChallengerTierAfterSolve(withWin.tier, withWin.solvesInTier)
            viewModelScope.launch {
                delay(CHALLENGER_SOLVE_ADVANCE_DELAY_MS)
                session = advanceChimpTestChallengerAfterSolve(withWin, nextTier, nextSolvesInTier)
            }
            return
        }

        correctTapTrigger++
        session = current.copy(puzzleSession = outcome.session)
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
            val ticked = tickChimpTestChallenger(current)
            session = ticked
            if (ticked.secondsRemaining <= 0) finalizeRun(ticked, "time_up")
        }
    }

    private fun finalizeRun(current: ChimpTestChallengerSession, reason: String) {
        if (finalized) return
        finalized = true
        viewModelScope.launch {
            val previousBest = statsStore.challengerStatsFor(GameId.CHIMPTEST).first()
            val isNewHighScore = current.score > previousBest.bestScore
            statsStore.recordChallengerResult(GameId.CHIMPTEST, current.puzzlesSolved, current.score)
            delay(CHALLENGER_FINISH_DELAY_MS)
            _result.emit(
                ChimpTestChallengerResult(
                    puzzlesSolved = current.puzzlesSolved,
                    tierReached = current.tier,
                    score = current.score,
                    isNewHighScore = isNewHighScore,
                    reason = reason,
                ),
            )
        }
    }
}
