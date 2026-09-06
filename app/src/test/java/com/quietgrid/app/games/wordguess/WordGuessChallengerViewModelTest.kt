package com.quietgrid.app.games.wordguess

import android.content.Context
import com.quietgrid.app.MainDispatcherRule
import com.quietgrid.app.core.ChallengerPuzzleSolve
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.AppSettings
import com.quietgrid.app.data.SettingsRepository
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class WordGuessChallengerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val puzzleEntry = WordGuessPuzzleEntry(id = "test-wordguess", locale = "en", difficulty = Difficulty.EASY.key, word = "cat")

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

    private fun newViewModel(statsStore: FakeStatsStore = FakeStatsStore()): WordGuessChallengerViewModel {
        val settingsRepository = mockk<SettingsRepository>(relaxed = true)
        every { settingsRepository.settings } returns MutableStateFlow(AppSettings())
        return WordGuessChallengerViewModel(mockk<Context>(relaxed = true), settingsRepository, statsStore)
    }

    @Test
    fun `finalizeRun on abandoned includes the previous best score and no fastest solve`() {
        val statsStore = FakeStatsStore()
        statsStore.seedChallenger(GameId.WORDGUESS, solved = 2, bestScore = 500)
        val viewModel = newViewModel(statsStore)
        val results = mutableListOf<WordGuessChallengerResult>()
        val collectJob = CoroutineScope(mainDispatcherRule.dispatcher).launch { viewModel.result.collect { results.add(it) } }

        viewModel.endRun()

        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(500)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        val result = results.single()
        assertEquals(500, result.previousBest)
        assertNull(result.fastestSolveSeconds)
        assertEquals(emptyList<ChallengerPuzzleSolve>(), result.puzzleHistory)

        collectJob.cancel()
    }
}
