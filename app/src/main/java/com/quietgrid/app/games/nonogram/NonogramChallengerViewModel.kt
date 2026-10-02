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
import com.quietgrid.app.data.PlayHistoryStore
import com.quietgrid.app.data.StatsStore
import com.quietgrid.app.session.AppForeground
import com.quietgrid.app.session.ChallengerRunController
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NonogramChallengerViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    statsStore: StatsStore,
    historyStore: PlayHistoryStore,
    appForeground: AppForeground,
) : ViewModel() {

    private val controller = ChallengerRunController(
        scope = viewModelScope,
        gameId = GameId.NONOGRAM,
        statsStore = statsStore,
        historyStore = historyStore,
        appForeground = appForeground,
        tick = ::tickNonogramChallenger,
    )

    val session get() = controller.session
    val result = controller.result

    var inputMode by mutableStateOf(NonogramInputMode.FILL)

    var wrongTapTrigger by mutableStateOf(0)
        private set

    init {
        viewModelScope.launch {
            val firstPuzzle = NonogramPuzzleBank.randomPuzzle(appContext, Difficulty.EASY)
            if (firstPuzzle != null) {
                controller.start(createInitialNonogramChallengerSession(firstPuzzle))
            }
        }
    }

    fun onCellTap(row: Int, col: Int) {
        if (controller.isFinalized) return
        val current = session ?: return
        val tapResult = applyNonogramChallengerTap(current, row, col, inputMode)

        if (tapResult.wasWrong) {
            wrongTapTrigger++
            val livesRemaining = current.livesRemaining - 1
            val withLoss = current.copy(livesRemaining = livesRemaining)
            controller.session = withLoss
            if (livesRemaining <= 0) controller.finalizeRun(withLoss, "lives_exhausted")
            return
        }

        controller.session = tapResult.session
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
                controller.finalizeRun(credited, "bank_exhausted")
            } else {
                controller.session = advanceNonogramChallengerAfterSolve(withTap, nextTier, nextSolvesInTier, nextPuzzle)
            }
        }
    }

    fun endRun() = controller.endRun()
}
