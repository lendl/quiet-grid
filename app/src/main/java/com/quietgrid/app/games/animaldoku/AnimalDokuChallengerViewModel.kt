// app/src/main/java/com/quietgrid/app/games/animaldoku/AnimalDokuChallengerViewModel.kt
package com.quietgrid.app.games.animaldoku

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
class AnimalDokuChallengerViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val statsStore: StatsStore,
) : ViewModel() {

    var session by mutableStateOf<AnimalDokuChallengerSession?>(null)
        private set

    var lastOpenEvent by mutableStateOf<AnimalDokuOpenEvent?>(null)
        private set

    private var finalized = false

    private val _result = MutableSharedFlow<AnimalDokuChallengerResult>(extraBufferCapacity = 1)
    val result: SharedFlow<AnimalDokuChallengerResult> = _result

    init {
        viewModelScope.launch {
            val firstPuzzle = AnimalDokuPuzzleBank.randomPuzzle(appContext, Difficulty.EASY)
            if (firstPuzzle != null) {
                session = createInitialChallengerSession(firstPuzzle)
                runTicker()
            }
        }
    }

    fun onCellTap(row: Int, col: Int) {
        if (finalized) return
        val current = session ?: return
        val next = applyAnimalDokuTap(current.puzzleSession, row, col) ?: return
        session = current.copy(puzzleSession = next)
    }

    fun onCellDrag(markAll: Boolean, visited: List<Pair<Int, Int>>) {
        if (finalized) return
        val current = session ?: return
        val next = applyAnimalDokuDrag(current.puzzleSession, markAll, visited) ?: return
        session = current.copy(puzzleSession = next)
    }

    fun onCellDoubleTap(row: Int, col: Int) {
        if (finalized) return
        val current = session ?: return
        val opened = applyAnimalDokuOpen(current.puzzleSession, row, col) ?: return
        lastOpenEvent = AnimalDokuOpenEvent(row, col, opened.wasCorrect)

        when (opened.session.status) {
            AnimalDokuStatus.WON -> {
                val withOpen = current.copy(puzzleSession = opened.session)
                session = withOpen
                val (nextTier, nextSolvesInTier) = tierAfterSolve(current.tier, current.solvesInTier)
                viewModelScope.launch {
                    val nextPuzzle = AnimalDokuPuzzleBank.randomPuzzle(appContext, nextTier, withOpen.servedPuzzleIds)
                    if (nextPuzzle == null) {
                        val creditedScore = withOpen.score + animalDokuScore(withOpen.puzzleSession.lives, withOpen.secondsOnCurrentPuzzle.toInt())
                        val credited = withOpen.copy(puzzlesSolved = withOpen.puzzlesSolved + 1, score = creditedScore)
                        finalizeRun(credited, "bank_exhausted")
                    } else {
                        session = advanceChallengerAfterSolve(withOpen, nextTier, nextSolvesInTier, nextPuzzle)
                    }
                }
            }
            AnimalDokuStatus.LOST -> {
                val withOpen = current.copy(puzzleSession = opened.session)
                session = withOpen
                finalizeRun(withOpen, "lives_exhausted")
            }
            AnimalDokuStatus.PLAYING -> {
                session = current.copy(puzzleSession = opened.session)
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
            val ticked = tickChallenger(current)
            session = ticked
            if (ticked.secondsRemaining <= 0) finalizeRun(ticked, "time_up")
        }
    }

    private fun finalizeRun(current: AnimalDokuChallengerSession, reason: String) {
        if (finalized) return
        finalized = true
        viewModelScope.launch {
            val previousBest = statsStore.challengerStatsFor(GameId.ANIMALDOKU).first()
            val isNewHighScore = current.score > previousBest.bestScore
            statsStore.recordChallengerResult(GameId.ANIMALDOKU, current.puzzlesSolved, current.score)
            delay(CHALLENGER_FINISH_DELAY_MS)
            _result.emit(
                AnimalDokuChallengerResult(
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
