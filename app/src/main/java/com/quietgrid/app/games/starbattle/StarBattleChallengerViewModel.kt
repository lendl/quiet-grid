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
class StarBattleChallengerViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val statsStore: StatsStore,
) : ViewModel() {

    var session by mutableStateOf<StarBattleChallengerSession?>(null)
        private set

    var lastOpenEvent by mutableStateOf<StarBattleOpenEvent?>(null)
        private set

    private var finalized = false

    private val _result = MutableSharedFlow<StarBattleChallengerResult>(extraBufferCapacity = 1)
    val result: SharedFlow<StarBattleChallengerResult> = _result

    init {
        viewModelScope.launch {
            val firstPuzzle = StarBattlePuzzleBank.randomPuzzle(appContext, Difficulty.EASY)
            if (firstPuzzle != null) {
                session = createInitialStarBattleChallengerSession(firstPuzzle)
                runTicker()
            }
        }
    }

    fun onCellTap(row: Int, col: Int) {
        if (finalized) return
        val current = session ?: return
        val next = applyStarBattleTap(current.puzzleSession, row, col) ?: return
        session = current.copy(puzzleSession = next)
    }

    fun onCellDrag(markAll: Boolean, visited: List<Pair<Int, Int>>) {
        if (finalized) return
        val current = session ?: return
        val next = applyStarBattleDrag(current.puzzleSession, markAll, visited) ?: return
        session = current.copy(puzzleSession = next)
    }

    fun onCellDoubleTap(row: Int, col: Int) {
        if (finalized) return
        val current = session ?: return
        val opened = applyStarBattleOpen(current.puzzleSession, row, col) ?: return
        lastOpenEvent = StarBattleOpenEvent(row, col, opened.wasCorrect)

        when (opened.session.status) {
            StarBattleStatus.WON -> {
                val withOpen = current.copy(puzzleSession = opened.session)
                session = withOpen
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
                        finalizeRun(credited, "bank_exhausted")
                    } else {
                        session = advanceStarBattleChallengerAfterSolve(withOpen, nextTier, nextSolvesInTier, nextPuzzle)
                    }
                }
            }
            StarBattleStatus.LOST -> {
                val withOpen = current.copy(puzzleSession = opened.session)
                session = withOpen
                finalizeRun(withOpen, "hearts_exhausted")
            }
            StarBattleStatus.PLAYING -> {
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
            val ticked = tickStarBattleChallenger(current)
            session = ticked
            if (ticked.secondsRemaining <= 0) finalizeRun(ticked, "time_up")
        }
    }

    private fun finalizeRun(current: StarBattleChallengerSession, reason: String) {
        if (finalized) return
        finalized = true
        viewModelScope.launch {
            val previousBest = statsStore.challengerStatsFor(GameId.STARBATTLE).first()
            val isNewHighScore = current.score > previousBest.bestScore
            statsStore.recordChallengerResult(GameId.STARBATTLE, current.puzzlesSolved, current.score)
            delay(CHALLENGER_FINISH_DELAY_MS)
            _result.emit(
                StarBattleChallengerResult(
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
