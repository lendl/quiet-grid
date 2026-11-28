package com.quietgrid.app.games.nonogram

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

class NonogramChallengerViewModel(
    private val files: BundledFiles,
    statsStore: StatsStore,
    historyStore: PlayHistoryStore,
    appForeground: AppForeground,
) : ViewModel() {

    private val controller = ChallengerRunController<NonogramChallengerSession>(
        scope = viewModelScope,
        gameId = GameId.NONOGRAM,
        statsStore = statsStore,
        historyStore = historyStore,
        appForeground = appForeground,
        tick = { it.tick() },
    )

    val session get() = controller.session
    val result = controller.result

    var inputMode by mutableStateOf(NonogramInputMode.FILL)

    init {
        viewModelScope.launch {
            val firstPuzzle = NonogramPuzzleBank.randomPuzzle(files, Difficulty.EASY)
            if (firstPuzzle != null) {
                controller.start(createInitialNonogramChallengerSession(firstPuzzle))
            }
        }
    }

    private var advancing = false

    fun onCellTap(row: Int, col: Int) {
        if (controller.isFinalized || advancing) return
        val current = session ?: return
        val updated = applyNonogramTap(current.puzzleSession, row, col, inputMode) ?: return
        applyPuzzleSession(current, updated)
    }

    fun onDragPaint(cells: List<Pair<Int, Int>>) {
        if (controller.isFinalized || advancing || cells.isEmpty()) return
        val current = session ?: return
        val value = if (inputMode == NonogramInputMode.FILL) 1 else 0
        val updated = applyNonogramPaint(current.puzzleSession, cells, value) ?: return
        applyPuzzleSession(current, updated)
    }

    private fun applyPuzzleSession(current: NonogramChallengerSession, puzzleSession: NonogramSession) {
        val next = current.copy(puzzleSession = puzzleSession)
        controller.session = next
        if (isNonogramSolved(puzzleSession.board, puzzleSession.solution)) onSolved(next)
    }

    private fun onSolved(withTap: NonogramChallengerSession) {
        val (nextTier, nextSolvesInTier) = challengerTierAfterSolve(NONOGRAM_CHALLENGER_CONFIG, withTap.tier, withTap.solvesInTier)
        advancing = true
        viewModelScope.launch {
            val nextPuzzle = NonogramPuzzleBank.randomPuzzle(files, nextTier, withTap.servedPuzzleIds)
            if (nextPuzzle == null) {
                val credited = withTap.creditSolve(nonogramChallengerPuzzleScore(withTap))
                controller.finalizeRun(credited, "bank_exhausted")
            } else {
                controller.session = advanceNonogramChallengerAfterSolve(withTap, nextTier, nextSolvesInTier, nextPuzzle)
                advancing = false
            }
        }
    }

    fun endRun() = controller.endRun()
}
