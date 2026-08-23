package com.quietgrid.app.games.wordguess

import android.content.Context
import com.quietgrid.app.MainDispatcherRule
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.data.AppSettings
import com.quietgrid.app.data.SettingsRepository
import com.quietgrid.app.testutil.FakeHistoryStore
import com.quietgrid.app.testutil.FakeSessionStore
import com.quietgrid.app.testutil.FakeStatsStore
import com.quietgrid.engine.wordguess.WordGuessPuzzleEntry
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class WordGuessPlayViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val puzzleEntry = WordGuessPuzzleEntry(
        id = "test-wordguess",
        locale = "en",
        difficulty = Difficulty.EASY.key,
        word = "cat",
    )

    @Before
    fun setUp() {
        mockkStatic(::currentWordGuessLocale)
        every { currentWordGuessLocale(any()) } returns "en"
        mockkObject(WordGuessPuzzleBank)
        coEvery { WordGuessPuzzleBank.randomPuzzle(any(), any(), any(), any()) } returns puzzleEntry
        coEvery { WordGuessPuzzleBank.loadDictionary(any(), any()) } returns setOf("cat")
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun newViewModel(sessionStore: FakeSessionStore = FakeSessionStore(), statsStore: FakeStatsStore = FakeStatsStore()): WordGuessPlayViewModel {
        val settingsRepository = mockk<SettingsRepository>(relaxed = true)
        every { settingsRepository.settings } returns MutableStateFlow(AppSettings())
        return WordGuessPlayViewModel(
            mockk<Context>(relaxed = true), sessionStore, statsStore, FakeHistoryStore(), settingsRepository, Difficulty.EASY, resume = false,
        )
    }

    @Test
    fun `starting fresh loads the mocked puzzle as the target word`() {
        val viewModel = newViewModel()

        assertEquals("cat", viewModel.session?.targetWord)
        assertEquals(0, viewModel.session?.guesses?.size)
    }

    @Test
    fun `submitting the target word wins the puzzle`() {
        val sessionStore = FakeSessionStore()
        val viewModel = newViewModel(sessionStore)
        val results = mutableListOf<WordGuessResult>()
        val collectJob = CoroutineScope(mainDispatcherRule.dispatcher).launch { viewModel.result.collect { results.add(it) } }

        viewModel.onSubmitGuess("cat") { org.junit.Assert.fail("guess should be valid") }
        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(500)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        assertTrue(sessionStore.cleared)
        assertEquals(1, results.size)
        assertEquals(true, results.single().solved)
        collectJob.cancel()
    }

    @Test
    fun `submitting a guess of the wrong length is reported invalid`() {
        val viewModel = newViewModel()
        var invalidCount = 0

        viewModel.onSubmitGuess("toolongguess") { invalidCount++ }
        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        assertEquals(1, invalidCount)
        assertEquals(0, viewModel.session?.guesses?.size)
    }

    @Test
    fun `endPuzzle finalizes the session as an abandoned loss`() {
        val sessionStore = FakeSessionStore()
        val viewModel = newViewModel(sessionStore)
        val results = mutableListOf<WordGuessResult>()
        val collectJob = CoroutineScope(mainDispatcherRule.dispatcher).launch { viewModel.result.collect { results.add(it) } }

        viewModel.endPuzzle()
        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(500)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        assertTrue(sessionStore.cleared)
        assertEquals("abandoned", results.single().lossReason)
        collectJob.cancel()
    }
}
