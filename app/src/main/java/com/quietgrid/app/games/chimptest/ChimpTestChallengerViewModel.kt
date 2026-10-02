package com.quietgrid.app.games.chimptest

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.PlayHistoryStore
import com.quietgrid.app.data.StatsStore
import com.quietgrid.app.session.AppForeground
import com.quietgrid.app.session.ChallengerRunController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val CHALLENGER_SOLVE_ADVANCE_DELAY_MS = 500L
private const val CHALLENGER_WRONG_TAP_REVEAL_MS = 700L

@HiltViewModel
class ChimpTestChallengerViewModel @Inject constructor(
    statsStore: StatsStore,
    historyStore: PlayHistoryStore,
    appForeground: AppForeground,
) : ViewModel() {

    private val controller = ChallengerRunController(
        scope = viewModelScope,
        gameId = GameId.CHIMPTEST,
        statsStore = statsStore,
        historyStore = historyStore,
        appForeground = appForeground,
        tick = ::tickChimpTestChallenger,
    )

    val session get() = controller.session
    val result = controller.result

    var correctTapTrigger by mutableStateOf(0)
        private set

    var wrongTapTrigger by mutableStateOf(0)
        private set

    init {
        controller.start(createInitialChimpTestChallengerSession())
    }

    fun onCellTap(row: Int, col: Int) {
        if (controller.isFinalized) return
        val current = session ?: return
        if (current.puzzleSession.status != ChimpTestStatus.PLAYING || current.puzzleSession.revealAll) return

        val outcome = runChimpTestAction(current.puzzleSession, row, col, current.secondsOnCurrentPuzzle)
        if (!outcome.changed) return

        if (outcome.effects.any { it is ChimpTestEffect.WrongTap }) {
            val withTap = current.copy(puzzleSession = outcome.session)
            controller.session = withTap
            wrongTapTrigger++
            val remainingLives = withTap.livesRemaining - 1
            viewModelScope.launch {
                delay(CHALLENGER_WRONG_TAP_REVEAL_MS)
                if (remainingLives <= 0) {
                    controller.finalizeRun(withTap.copy(livesRemaining = 0), "lives_exhausted")
                } else {
                    controller.session = advanceChimpTestChallengerAfterLoss(withTap.copy(livesRemaining = remainingLives))
                }
            }
            return
        }

        if (outcome.session.status == ChimpTestStatus.WON) {
            val withWin = current.copy(puzzleSession = outcome.session)
            controller.session = withWin
            correctTapTrigger++
            val (nextTier, nextSolvesInTier) = chimpTestChallengerTierAfterSolve(withWin.tier, withWin.solvesInTier)
            viewModelScope.launch {
                delay(CHALLENGER_SOLVE_ADVANCE_DELAY_MS)
                controller.session = advanceChimpTestChallengerAfterSolve(withWin, nextTier, nextSolvesInTier)
            }
            return
        }

        correctTapTrigger++
        controller.session = current.copy(puzzleSession = outcome.session)
    }

    fun endRun() = controller.endRun()
}
