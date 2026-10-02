// app/src/main/java/com/quietgrid/app/games/animaldoku/AnimalDokuChallengerViewModel.kt
package com.quietgrid.app.games.animaldoku

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
class AnimalDokuChallengerViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    statsStore: StatsStore,
    historyStore: PlayHistoryStore,
    appForeground: AppForeground,
) : ViewModel() {

    private val controller = ChallengerRunController(
        scope = viewModelScope,
        gameId = GameId.ANIMALDOKU,
        statsStore = statsStore,
        historyStore = historyStore,
        appForeground = appForeground,
        tick = ::tickChallenger,
    )

    val session get() = controller.session
    val result = controller.result

    var lastOpenEvent by mutableStateOf<AnimalDokuOpenEvent?>(null)
        private set

    init {
        viewModelScope.launch {
            val firstPuzzle = AnimalDokuPuzzleBank.randomPuzzle(appContext, Difficulty.EASY)
            if (firstPuzzle != null) {
                controller.start(createInitialChallengerSession(firstPuzzle))
            }
        }
    }

    fun onCellTap(row: Int, col: Int) {
        if (controller.isFinalized) return
        val current = session ?: return
        val next = applyAnimalDokuTap(current.puzzleSession, row, col) ?: return
        controller.session = current.copy(puzzleSession = next)
    }

    fun onCellDrag(markAll: Boolean, visited: List<Pair<Int, Int>>) {
        if (controller.isFinalized) return
        val current = session ?: return
        val next = applyAnimalDokuDrag(current.puzzleSession, markAll, visited) ?: return
        controller.session = current.copy(puzzleSession = next)
    }

    fun onCellDoubleTap(row: Int, col: Int) {
        if (controller.isFinalized) return
        val current = session ?: return
        val opened = applyAnimalDokuOpen(current.puzzleSession, row, col) ?: return
        lastOpenEvent = AnimalDokuOpenEvent(row, col, opened.wasCorrect)

        when (opened.session.status) {
            AnimalDokuStatus.WON -> {
                val withOpen = current.copy(puzzleSession = opened.session)
                controller.session = withOpen
                val (nextTier, nextSolvesInTier) = tierAfterSolve(current.tier, current.solvesInTier)
                viewModelScope.launch {
                    val nextPuzzle = AnimalDokuPuzzleBank.randomPuzzle(appContext, nextTier, withOpen.servedPuzzleIds)
                    if (nextPuzzle == null) {
                        val creditedScore = withOpen.score + animalDokuScore(withOpen.puzzleSession.lives, withOpen.secondsOnCurrentPuzzle.toInt())
                        val credited = withOpen.copy(
                            puzzlesSolved = withOpen.puzzlesSolved + 1,
                            score = creditedScore,
                            fastestSolveSeconds = animalDokuChallengerFastestSolve(withOpen),
                            puzzleHistory = withOpen.puzzleHistory + ChallengerPuzzleSolve(withOpen.tier, withOpen.secondsOnCurrentPuzzle),
                        )
                        controller.finalizeRun(credited, "bank_exhausted")
                    } else {
                        controller.session = advanceChallengerAfterSolve(withOpen, nextTier, nextSolvesInTier, nextPuzzle)
                    }
                }
            }
            AnimalDokuStatus.LOST -> {
                val withOpen = current.copy(puzzleSession = opened.session)
                controller.session = withOpen
                controller.finalizeRun(withOpen, "lives_exhausted")
            }
            AnimalDokuStatus.PLAYING -> {
                controller.session = current.copy(puzzleSession = opened.session)
            }
        }
    }

    fun endRun() = controller.endRun()
}
