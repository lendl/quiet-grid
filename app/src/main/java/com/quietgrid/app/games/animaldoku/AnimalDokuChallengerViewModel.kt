// app/src/main/java/com/quietgrid/app/games/animaldoku/AnimalDokuChallengerViewModel.kt
package com.quietgrid.app.games.animaldoku

import com.quietgrid.app.platform.BundledFiles
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.PlayHistoryStore
import com.quietgrid.app.data.StatsStore
import com.quietgrid.app.session.AppForeground
import com.quietgrid.app.session.ChallengerRunController
import com.quietgrid.app.session.challengerTierAfterSolve
import com.quietgrid.app.session.creditSolve
import com.quietgrid.app.session.tick
import kotlinx.coroutines.launch

class AnimalDokuChallengerViewModel(
    private val files: BundledFiles,
    statsStore: StatsStore,
    historyStore: PlayHistoryStore,
    appForeground: AppForeground,
) : ViewModel() {

    private val controller = ChallengerRunController<AnimalDokuChallengerSession>(
        scope = viewModelScope,
        gameId = GameId.ANIMALDOKU,
        statsStore = statsStore,
        historyStore = historyStore,
        appForeground = appForeground,
        tick = { it.tick() },
    )

    val session get() = controller.session
    val result = controller.result

    var lastOpenEvent by mutableStateOf<AnimalDokuOpenEvent?>(null)
        private set

    init {
        viewModelScope.launch {
            val firstPuzzle = AnimalDokuPuzzleBank.randomPuzzle(files, Difficulty.EASY)
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
                val (nextTier, nextSolvesInTier) = challengerTierAfterSolve(ANIMALDOKU_CHALLENGER_CONFIG, current.tier, current.solvesInTier)
                viewModelScope.launch {
                    val nextPuzzle = AnimalDokuPuzzleBank.randomPuzzle(files, nextTier, withOpen.servedPuzzleIds)
                    if (nextPuzzle == null) {
                        val credited = withOpen.creditSolve(animalDokuChallengerPuzzleScore(withOpen))
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
