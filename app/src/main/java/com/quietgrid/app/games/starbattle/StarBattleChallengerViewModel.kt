package com.quietgrid.app.games.starbattle

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
class StarBattleChallengerViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    statsStore: StatsStore,
    historyStore: PlayHistoryStore,
    appForeground: AppForeground,
) : ViewModel() {

    private val controller = ChallengerRunController(
        scope = viewModelScope,
        gameId = GameId.STARBATTLE,
        statsStore = statsStore,
        historyStore = historyStore,
        appForeground = appForeground,
        tick = ::tickStarBattleChallenger,
    )

    val session get() = controller.session
    val result = controller.result

    var lastOpenEvent by mutableStateOf<StarBattleOpenEvent?>(null)
        private set

    init {
        viewModelScope.launch {
            val firstPuzzle = StarBattlePuzzleBank.randomPuzzle(appContext, Difficulty.EASY)
            if (firstPuzzle != null) {
                controller.start(createInitialStarBattleChallengerSession(firstPuzzle))
            }
        }
    }

    fun onCellTap(row: Int, col: Int) {
        if (controller.isFinalized) return
        val current = session ?: return
        val next = applyStarBattleTap(current.puzzleSession, row, col) ?: return
        controller.session = current.copy(puzzleSession = next)
    }

    fun onCellDrag(markAll: Boolean, visited: List<Pair<Int, Int>>) {
        if (controller.isFinalized) return
        val current = session ?: return
        val next = applyStarBattleDrag(current.puzzleSession, markAll, visited) ?: return
        controller.session = current.copy(puzzleSession = next)
    }

    fun onCellDoubleTap(row: Int, col: Int) {
        if (controller.isFinalized) return
        val current = session ?: return
        val opened = applyStarBattleOpen(current.puzzleSession, row, col) ?: return
        lastOpenEvent = StarBattleOpenEvent(row, col, opened.wasCorrect)

        when (opened.session.status) {
            StarBattleStatus.WON -> {
                val withOpen = current.copy(puzzleSession = opened.session)
                controller.session = withOpen
                val (nextTier, nextSolvesInTier) = starBattleChallengerTierAfterSolve(current.tier, current.solvesInTier)
                viewModelScope.launch {
                    val nextPuzzle = StarBattlePuzzleBank.randomPuzzle(appContext, nextTier, withOpen.servedPuzzleIds)
                    if (nextPuzzle == null) {
                        val creditedScore = withOpen.score + starBattleScore(withOpen.puzzleSession.lives, withOpen.secondsOnCurrentPuzzle.toInt())
                        val credited = withOpen.copy(
                            puzzlesSolved = withOpen.puzzlesSolved + 1,
                            score = creditedScore,
                            fastestSolveSeconds = starBattleChallengerFastestSolve(withOpen),
                            puzzleHistory = withOpen.puzzleHistory + ChallengerPuzzleSolve(withOpen.tier, withOpen.secondsOnCurrentPuzzle),
                        )
                        controller.finalizeRun(credited, "bank_exhausted")
                    } else {
                        controller.session = advanceStarBattleChallengerAfterSolve(withOpen, nextTier, nextSolvesInTier, nextPuzzle)
                    }
                }
            }
            StarBattleStatus.LOST -> {
                val withOpen = current.copy(puzzleSession = opened.session)
                controller.session = withOpen
                controller.finalizeRun(withOpen, "hearts_exhausted")
            }
            StarBattleStatus.PLAYING -> {
                controller.session = current.copy(puzzleSession = opened.session)
            }
        }
    }

    fun endRun() = controller.endRun()
}
