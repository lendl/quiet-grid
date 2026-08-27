package com.quietgrid.app.games.takuzu

import com.quietgrid.app.MainDispatcherRule
import com.quietgrid.engine.takuzu.TakuzuPuzzleEntry
import com.quietgrid.engine.takuzu.gridToHex
import com.quietgrid.engine.takuzu.maskToHex
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TakuzuAnalyzerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val json = Json { ignoreUnknownKeys = true }

    private val solutionGrid = listOf(
        listOf(0, 1, 0, 1),
        listOf(1, 0, 1, 0),
        listOf(0, 1, 1, 0),
        listOf(1, 0, 0, 1),
    )
    private val maskGrid = listOf(
        listOf(false, true, true, true),
        listOf(true, true, true, true),
        listOf(true, true, true, true),
        listOf(true, true, true, true),
    )
    private val puzzleEntry = TakuzuPuzzleEntry(
        id = "test-analyzer",
        size = 4,
        difficulty = "easy",
        solution = gridToHex(solutionGrid),
        mask = maskToHex(maskGrid),
    )

    private fun snapshotWith(board: List<Int?>) = json.encodeToString(
        TakuzuPersistedSession(
            puzzle = puzzleEntry,
            board = board,
            finishedCells = List(16) { false },
            accuracyDrops = 0,
            penalizedLineKeys = emptyList(),
        ),
    )

    private fun newViewModel(snapshot: String?): TakuzuAnalyzerViewModel {
        val viewModel = TakuzuAnalyzerViewModel(snapshot)
        viewModel.replayDispatcher = StandardTestDispatcher(mainDispatcherRule.dispatcher.scheduler)
        viewModel.load()
        return viewModel
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `a valid unsolved snapshot replays to a Ready state ending solved`() {
        val board: List<Int?> = solutionGrid.flatten().toMutableList<Int?>().also { it[0] = null }
        val viewModel = newViewModel(snapshotWith(board))

        assertEquals(TakuzuAnalyzerState.Loading, viewModel.state)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        val ready = viewModel.state as TakuzuAnalyzerState.Ready
        assertEquals(1, ready.steps.size)
        assertEquals(0, ready.currentIndex)
        assertEquals(4, ready.puzzleSize)
        assertEquals(solutionGrid, ready.finalBoard)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `a board with an incorrect filled cell is sanitized and still reaches Ready`() {
        val board: List<Int?> = solutionGrid.flatten().toMutableList<Int?>().also { it[0] = 1 }
        val viewModel = newViewModel(snapshotWith(board))

        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        val ready = viewModel.state as TakuzuAnalyzerState.Ready
        assertEquals(solutionGrid, ready.finalBoard)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `an already-solved snapshot is AlreadySolved, not Ready`() {
        val viewModel = newViewModel(snapshotWith(solutionGrid.flatten()))

        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        assertEquals(TakuzuAnalyzerState.AlreadySolved, viewModel.state)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `a null snapshot is LoadFailed`() {
        val viewModel = newViewModel(null)

        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        assertEquals(TakuzuAnalyzerState.LoadFailed, viewModel.state)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `an undecodable snapshot is LoadFailed`() {
        val viewModel = newViewModel("not valid json")

        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        assertEquals(TakuzuAnalyzerState.LoadFailed, viewModel.state)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `next and back move the index and stay within bounds`() {
        val board: List<Int?> = solutionGrid.flatten().toMutableList<Int?>().also { it[0] = null; it[3] = null }
        val viewModel = newViewModel(snapshotWith(board))
        mainDispatcherRule.dispatcher.scheduler.runCurrent()
        val readyBefore = viewModel.state as TakuzuAnalyzerState.Ready
        val lastIndex = readyBefore.steps.size

        viewModel.back()
        assertEquals(0, (viewModel.state as TakuzuAnalyzerState.Ready).currentIndex)

        repeat(lastIndex + 5) { viewModel.next() }
        assertEquals(lastIndex, (viewModel.state as TakuzuAnalyzerState.Ready).currentIndex)

        repeat(lastIndex + 5) { viewModel.back() }
        assertEquals(0, (viewModel.state as TakuzuAnalyzerState.Ready).currentIndex)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `togglePlay auto-advances and stops automatically at the last step`() {
        val board: List<Int?> = solutionGrid.flatten().toMutableList<Int?>().also { it[0] = null; it[3] = null }
        val viewModel = newViewModel(snapshotWith(board))
        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        viewModel.togglePlay()
        assertTrue((viewModel.state as TakuzuAnalyzerState.Ready).isPlaying)

        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(10_000)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        val ready = viewModel.state as TakuzuAnalyzerState.Ready
        assertEquals(ready.steps.size, ready.currentIndex)
        assertEquals(false, ready.isPlaying)
    }
}
