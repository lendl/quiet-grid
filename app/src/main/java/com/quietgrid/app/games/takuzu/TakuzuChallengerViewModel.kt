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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    var showMistakes = true

    private var pendingLineKeys = mutableSetOf<LineKey>()
    private var validationJob: Job? = null

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
        controller.session = current.copy(puzzleSession = updated)

        pendingLineKeys.add("r$row")
        pendingLineKeys.add("c$col")
        validationJob?.cancel()
        if (isBoardSolved(updated.board, updated.solution)) {
            runFinalizeValidation()
        } else {
            validationJob = viewModelScope.launch {
                delay(TAKUZU_VALIDATION_DELAY_MS)
                runFinalizeValidation()
            }
        }
    }

    private fun runFinalizeValidation() {
        if (controller.isFinalized) return
        val current = session ?: return
        val lineKeys = pendingLineKeys.toList()
        pendingLineKeys = mutableSetOf()

        val puzzleSession = current.puzzleSession
        val result = applyTakuzuFinalizeValidation(puzzleSession, puzzleSession.board, lineKeys, showMistakes)
        if (result.session.accuracyDrops > puzzleSession.accuracyDrops) wrongMoveTrigger++

        val withValidation = current.copy(puzzleSession = result.session)
        controller.session = withValidation

        if (isBoardSolved(result.session.board, result.session.solution)) onSolved(withValidation)
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
