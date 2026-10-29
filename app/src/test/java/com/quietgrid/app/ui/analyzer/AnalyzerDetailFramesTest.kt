package com.quietgrid.app.ui.analyzer

import com.quietgrid.app.MainDispatcherRule
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

private class FramedAnalyzerViewModel : AnalyzerViewModel<Unit, Int, Int>() {
    override fun computeInitialState(): AnalyzerState<Unit, Int, Int> =
        AnalyzerState.Ready(AnalyzerReplay(Unit, listOf(AnalyzerStep(0, 3), AnalyzerStep(1, 0)), finalBoard = 2))

    override fun detailFrameCount(hint: Int): Int = hint
}

private val FramedAnalyzerViewModel.ready: AnalyzerState.Ready<Unit, Int, Int>
    get() = state as AnalyzerState.Ready<Unit, Int, Int>

class AnalyzerDetailFramesTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun loadedViewModel(): FramedAnalyzerViewModel {
        val viewModel = FramedAnalyzerViewModel()
        viewModel.replayDispatcher = StandardTestDispatcher(mainDispatcherRule.dispatcher.scheduler)
        viewModel.load()
        mainDispatcherRule.dispatcher.scheduler.runCurrent()
        return viewModel
    }

    @Test
    fun `steps start with their frames closed`() {
        assertNull(loadedViewModel().ready.detailIndex)
    }

    @Test
    fun `showing the frames opens the first one`() {
        val viewModel = loadedViewModel()
        viewModel.showDetail()
        assertEquals(0, viewModel.ready.detailIndex)
        assertEquals(0, viewModel.ready.currentIndex)
    }

    @Test
    fun `a step without frames cannot open them`() {
        val viewModel = loadedViewModel()
        viewModel.next()
        viewModel.showDetail()
        assertNull(viewModel.ready.detailIndex)
    }

    @Test
    fun `next walks the frames and then moves on with the frames closed`() {
        val viewModel = loadedViewModel()
        viewModel.showDetail()
        viewModel.next()
        viewModel.next()
        assertEquals(2, viewModel.ready.detailIndex)
        assertEquals(0, viewModel.ready.currentIndex)
        viewModel.next()
        assertNull(viewModel.ready.detailIndex)
        assertEquals(1, viewModel.ready.currentIndex)
    }

    @Test
    fun `back from the first frame closes the frames without changing the step`() {
        val viewModel = loadedViewModel()
        viewModel.showDetail()
        viewModel.next()
        viewModel.back()
        assertEquals(0, viewModel.ready.detailIndex)
        viewModel.back()
        assertNull(viewModel.ready.detailIndex)
        assertEquals(0, viewModel.ready.currentIndex)
    }

    @Test
    fun `hiding the frames returns to the step summary`() {
        val viewModel = loadedViewModel()
        viewModel.showDetail()
        viewModel.next()
        viewModel.hideDetail()
        assertNull(viewModel.ready.detailIndex)
        assertEquals(0, viewModel.ready.currentIndex)
    }

    @Test
    fun `autoplay skips closed frames`() {
        val viewModel = loadedViewModel()
        viewModel.togglePlay()
        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(2_001)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()
        assertEquals(1, viewModel.ready.currentIndex)
        assertNull(viewModel.ready.detailIndex)
        viewModel.togglePlay()
    }

    @Test
    fun `autoplay plays through open frames before moving on`() {
        val viewModel = loadedViewModel()
        viewModel.showDetail()
        viewModel.togglePlay()
        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(4_001)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()
        assertEquals(2, viewModel.ready.detailIndex)
        assertEquals(0, viewModel.ready.currentIndex)
        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(2_000)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()
        assertNull(viewModel.ready.detailIndex)
        assertEquals(1, viewModel.ready.currentIndex)
        viewModel.togglePlay()
        assertFalse(viewModel.ready.isPlaying)
    }
}
