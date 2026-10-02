package com.quietgrid.app.games.takuzu

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
class TakuzuChallengerViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    statsStore: StatsStore,
    historyStore: PlayHistoryStore,
    appForeground: AppForeground,
) : ViewModel() {

    private val controller = ChallengerRunController(
        scope = viewModelScope,
        gameId = GameId.TAKUZU,
        statsStore = statsStore,
        historyStore = historyStore,
        appForeground = appForeground,
        tick = ::tickTakuzuChallenger,
    )

    val session get() = controller.session
    val result = controller.result

    var wrongMoveTrigger by mutableStateOf(0)
        private set

    init {
        viewModelScope.launch {
            val firstPuzzle = TakuzuPuzzleBank.randomPuzzle(appContext, Difficulty.EASY)
            if (firstPuzzle != null) {
                controller.start(createInitialTakuzuChallengerSession(firstPuzzle))
            }
        }
    }

    fun onCellPress(row: Int, col: Int) {
        if (controller.isFinalized) return
        val current = session ?: return
        val updated = applyTakuzuPressCell(current.puzzleSession, row, col) ?: return
        val lineKeys = listOf("r$row", "c$col")
        val result = applyTakuzuFinalizeValidation(updated, updated.board, lineKeys)
        val newPenalties = result.session.accuracyDrops - updated.accuracyDrops
        val livesRemaining = current.livesRemaining - newPenalties
        if (newPenalties > 0) wrongMoveTrigger++

        val withValidation = current.copy(puzzleSession = result.session, livesRemaining = livesRemaining)
        controller.session = withValidation

        when {
            livesRemaining <= 0 -> controller.finalizeRun(withValidation, "lives_exhausted")
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
                controller.finalizeRun(credited, "bank_exhausted")
            } else {
                controller.session = advanceTakuzuChallengerAfterSolve(withValidation, nextTier, nextSolvesInTier, nextPuzzle)
            }
        }
    }

    fun endRun() = controller.endRun()
}
