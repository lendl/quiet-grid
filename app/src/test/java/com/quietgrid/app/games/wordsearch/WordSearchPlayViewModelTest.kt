package com.quietgrid.app.games.wordsearch

import android.content.Context
import com.quietgrid.app.MainDispatcherRule
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.testutil.FakeSessionStore
import com.quietgrid.app.testutil.FakeStatsStore
import com.quietgrid.engine.wordsearch.WSCellRef
import com.quietgrid.engine.wordsearch.WSHiddenWord
import com.quietgrid.engine.wordsearch.WSWordEntry
import com.quietgrid.engine.wordsearch.WordSearchPuzzleEntry
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class WordSearchPlayViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /** A single 1x3 horizontal word: "cat". */
    private val puzzleEntry = WordSearchPuzzleEntry(
        id = "test-wordsearch",
        difficulty = Difficulty.EASY.key,
        rows = 1,
        cols = 3,
        themeId = "animals",
        grid = listOf(listOf("c", "a", "t")),
        words = listOf(WSWordEntry(id = "cat", word = "cat", positions = listOf(WSCellRef(0, 0), WSCellRef(0, 1), WSCellRef(0, 2)))),
        hiddenWord = WSHiddenWord(word = "cat", clue = "a pet", positions = listOf(WSCellRef(0, 0), WSCellRef(0, 1), WSCellRef(0, 2))),
    )

    @Before
    fun setUp() {
        mockkObject(WordSearchPuzzleBank)
        coEvery { WordSearchPuzzleBank.randomPuzzle(any(), any()) } returns puzzleEntry
    }

    @After
    fun tearDown() {
        unmockkObject(WordSearchPuzzleBank)
    }

    private fun newViewModel(sessionStore: FakeSessionStore = FakeSessionStore(), statsStore: FakeStatsStore = FakeStatsStore()) =
        WordSearchPlayViewModel(mockk<Context>(relaxed = true), sessionStore, statsStore, Difficulty.EASY, resume = false)

    @Test
    fun `starting fresh loads the mocked puzzle with no words found yet`() {
        val viewModel = newViewModel()

        assertEquals("test-wordsearch", viewModel.session?.puzzle?.id)
        assertEquals(emptyList<String>(), viewModel.session?.foundWordIds)
    }

    @Test
    fun `tapping a cell begins a single-cell selection there`() {
        val viewModel = newViewModel()

        viewModel.onCellTap(0, 0)

        assertEquals(listOf(WSCellRef(0, 0)), viewModel.session?.tempSelection?.path)
    }

    @Test
    fun `endPuzzle finalizes the session as an abandoned loss`() {
        val sessionStore = FakeSessionStore()
        val viewModel = newViewModel(sessionStore)
        val results = mutableListOf<WordSearchResult>()
        val collectJob = CoroutineScope(mainDispatcherRule.dispatcher).launch { viewModel.result.collect { results.add(it) } }

        viewModel.endPuzzle()
        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(500)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        assertTrue(sessionStore.cleared)
        assertEquals("abandoned", results.single().lossReason)
        collectJob.cancel()
    }
}
