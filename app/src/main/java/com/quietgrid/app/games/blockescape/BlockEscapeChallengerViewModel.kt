package com.quietgrid.app.games.blockescape

import android.content.Context
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
import com.quietgrid.engine.blockescape.BlockEscapeMove
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BlockEscapeChallengerViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    statsStore: StatsStore,
    historyStore: PlayHistoryStore,
    appForeground: AppForeground,
) : ViewModel() {

    private val controller = ChallengerRunController(
        scope = viewModelScope,
        gameId = GameId.BLOCKESCAPE,
        statsStore = statsStore,
        historyStore = historyStore,
        appForeground = appForeground,
        tick = ::tickBlockEscapeChallenger,
    )

    val session get() = controller.session
    val result = controller.result

    init {
        viewModelScope.launch {
            val firstPuzzle = BlockEscapePuzzleBank.randomPuzzle(appContext, Difficulty.EASY)
            if (firstPuzzle != null) {
                controller.start(createInitialBlockEscapeChallengerSession(firstPuzzle))
            }
        }
    }

    var canUndo by mutableStateOf(false)
        private set

    private val undoHistory = mutableListOf<BlockEscapeSession>()

    fun onMove(move: BlockEscapeMove) {
        if (controller.isFinalized) return
        val current = session ?: return
        val next = applyBlockEscapeSlide(current.puzzleSession, move) ?: return
        undoHistory += current.puzzleSession
        canUndo = true
        val updated = current.copy(puzzleSession = next)
        controller.session = updated
        if (next.status == BlockEscapeStatus.WON) onSolved(updated)
    }

    fun onUndo() {
        if (controller.isFinalized) return
        val current = session ?: return
        if (current.puzzleSession.status != BlockEscapeStatus.PLAYING) return
        val previous = undoHistory.removeLastOrNull() ?: return
        canUndo = undoHistory.isNotEmpty()
        controller.session = current.copy(puzzleSession = previous)
    }

    private fun onSolved(solved: BlockEscapeChallengerSession) {
        undoHistory.clear()
        canUndo = false
        val (nextTier, nextSolvesInTier) = blockEscapeChallengerTierAfterSolve(solved.tier, solved.solvesInTier)
        viewModelScope.launch {
            val nextPuzzle = BlockEscapePuzzleBank.randomPuzzle(appContext, nextTier, solved.servedPuzzleIds)
            if (nextPuzzle == null || nextPuzzle.id in solved.servedPuzzleIds) {
                controller.finalizeRun(creditBlockEscapeChallengerSolve(solved), "bank_exhausted")
            } else {
                controller.session = advanceBlockEscapeChallengerAfterSolve(solved, nextTier, nextSolvesInTier, nextPuzzle)
            }
        }
    }

    fun endRun() = controller.endRun()
}
