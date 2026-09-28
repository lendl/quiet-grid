package com.quietgrid.app.games.themeclear

import android.content.Context
import com.quietgrid.app.MainDispatcherRule
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.data.AppSettings
import com.quietgrid.app.data.SettingsRepository
import com.quietgrid.app.data.ThemePreferencesRepository
import com.quietgrid.app.testutil.FakeHistoryStore
import com.quietgrid.app.testutil.FakeSessionStore
import com.quietgrid.app.testutil.FakeStatsStore
import com.quietgrid.engine.themeclear.ThemeClearDictionary
import com.quietgrid.engine.themeclear.ThemeClearPuzzleEntry
import com.quietgrid.engine.themeclear.ThemeClearSelectionState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeClearPlayViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val puzzle = ThemeClearPuzzleEntry(
        id = "tc-test",
        difficulty = "easy",
        themeId = "animals",
        rows = 2,
        cols = 3,
        grid = listOf("CAT", "DOG"),
        words = listOf("CAT", "DOG"),
    )

    private var dictionary = ThemeClearDictionary(listOf("CAT", "DOG", "COD", "CODA"))

    @Before
    fun setUp() {
        mockkObject(ThemeClearPuzzleBank)
        coEvery { ThemeClearPuzzleBank.randomPuzzle(any(), any(), any(), any(), any()) } returns puzzle
        coEvery { ThemeClearPuzzleBank.dictionary(any(), any(), any()) } answers { dictionary }
    }

    @After
    fun tearDown() {
        unmockkObject(ThemeClearPuzzleBank)
    }

    private fun newViewModel(excludedThemes: Set<String> = emptySet()): ThemeClearPlayViewModel {
        val settingsRepository = mockk<SettingsRepository>(relaxed = true)
        every { settingsRepository.settings } returns MutableStateFlow(AppSettings())
        val themePreferencesRepository = mockk<ThemePreferencesRepository>()
        every { themePreferencesRepository.excludedThemes(any(), any()) } returns flowOf(excludedThemes)
        return ThemeClearPlayViewModel(
            mockk<Context>(relaxed = true), FakeSessionStore(), FakeStatsStore(), FakeHistoryStore(), settingsRepository, themePreferencesRepository, Difficulty.EASY, resume = false,
        ).also {
            it.solverDispatcher = Dispatchers.Unconfined
            it.random = Random(1)
        }
    }

    private fun ThemeClearPlayViewModel.tap(vararg ids: Int) = ids.forEach { onTileTap(it) }

    @Test
    fun `a word with no possible extension is accepted instantly`() {
        val viewModel = newViewModel()
        viewModel.tap(0, 1, 2)
        assertEquals(listOf("CAT"), viewModel.session?.foundWords?.map { it.word })
        assertEquals(ThemeClearSelectionState.EMPTY, viewModel.selectionState)
    }

    @Test
    fun `a letter sequence that starts no theme word is NO_MATCH`() {
        val viewModel = newViewModel()
        viewModel.tap(2, 5)
        assertEquals(ThemeClearSelectionState.NO_MATCH, viewModel.selectionState)
    }

    @Test
    fun `a word that could still grow waits and is accepted after the delay`() {
        val viewModel = newViewModel()
        viewModel.pendingAcceptDelayMs = 10L
        viewModel.tap(0, 4, 3)
        assertEquals(ThemeClearSelectionState.PENDING, viewModel.selectionState)
        assertTrue(viewModel.session!!.foundWords.isEmpty())
        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(11)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()
        assertEquals(listOf("COD"), viewModel.session?.foundWords?.map { it.word })
    }

    @Test
    fun `tapping the pending word accepts it immediately`() {
        val viewModel = newViewModel()
        viewModel.tap(0, 4, 3)
        viewModel.onSelectionTextTap()
        assertEquals(listOf("COD"), viewModel.session?.foundWords?.map { it.word })
    }

    @Test
    fun `continuing to tap builds the longer word`() {
        val viewModel = newViewModel()
        viewModel.tap(0, 4, 3, 1)
        assertEquals(listOf("CODA"), viewModel.session?.foundWords?.map { it.word })
    }

    @Test
    fun `taking a trap word shows the dead end and removing it clears it`() {
        dictionary = ThemeClearDictionary(listOf("CAT", "DOG", "TAG"))
        val viewModel = newViewModel()
        viewModel.tap(2, 1, 5)
        assertEquals(listOf("TAG"), viewModel.session?.foundWords?.map { it.word })
        assertTrue(viewModel.isDeadEnd)
        viewModel.onRemoveFoundWord(0)
        assertFalse(viewModel.isDeadEnd)
        assertEquals(6, viewModel.session?.columns?.sumOf { it.size })
    }

    @Test
    fun `hint highlights one word of a valid finish and counts as used`() {
        val viewModel = newViewModel()
        viewModel.onHint()
        assertEquals(3, viewModel.hintTileIds.size)
        assertEquals(1, viewModel.session?.hintsUsed)
    }

    @Test
    fun `a tile tap while a hint is loading discards the stale hint`() {
        val gate = CompletableDeferred<Unit>()
        val viewModel = newViewModel()
        coEvery { ThemeClearPuzzleBank.dictionary(any(), any(), any()) } coAnswers { gate.await(); dictionary }
        viewModel.onHint()
        viewModel.tap(2)
        gate.complete(Unit)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()
        assertTrue(viewModel.hintTileIds.isEmpty())
        assertEquals(0, viewModel.session?.hintsUsed)
    }

    @Test
    fun `orphaned pending evaluations do not accept a stale selection`() {
        val gate = CompletableDeferred<Unit>()
        val viewModel = newViewModel()
        coEvery { ThemeClearPuzzleBank.dictionary(any(), any(), any()) } coAnswers { gate.await(); dictionary }
        viewModel.pendingAcceptDelayMs = 10L
        viewModel.tap(0, 4, 3)
        gate.complete(Unit)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()
        assertEquals(ThemeClearSelectionState.PENDING, viewModel.selectionState)
        viewModel.tap(2)
        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(11)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()
        assertTrue(viewModel.session!!.foundWords.isEmpty())
    }

    @Test
    fun `shuffle keeps the same letters and clears the selection`() {
        val viewModel = newViewModel()
        viewModel.tap(2)
        viewModel.onShuffle()
        assertTrue(viewModel.session!!.selection.isEmpty())
        assertEquals("ACDGOT", tcBoardLetters(viewModel.session!!).toList().sorted().joinToString(""))
    }

    @Test
    fun `clearing the whole grid finishes the puzzle`() {
        dictionary = ThemeClearDictionary(listOf("CAT", "DOG"))
        val viewModel = newViewModel()
        viewModel.tap(0, 1, 2, 3, 4, 5)
        assertTrue(tcIsCleared(viewModel.session!!))
    }

    @Test
    fun `taking a word that breaks a full clear still offers a hint when a word remains spellable`() {
        dictionary = ThemeClearDictionary(listOf("CAT", "DOG", "EEL", "TOE", "GEL"))
        val puzzle3x3 = ThemeClearPuzzleEntry(
            id = "tc-test-3x3",
            difficulty = "easy",
            themeId = "animals",
            rows = 3,
            cols = 3,
            grid = listOf("CAT", "DOG", "EEL"),
            words = listOf("CAT", "DOG", "EEL"),
        )
        coEvery { ThemeClearPuzzleBank.randomPuzzle(any(), any(), any(), any(), any()) } returns puzzle3x3
        val viewModel = newViewModel()
        val toeTiles = tcTilesForWord(viewModel.session!!, "TOE")
        viewModel.tap(*toeTiles.toIntArray())
        assertEquals(listOf("TOE"), viewModel.session?.foundWords?.map { it.word })
        assertFalse(viewModel.isDeadEnd)

        viewModel.onHint()
        assertEquals(3, viewModel.hintTileIds.size)
        val hintLetters = viewModel.hintTileIds
            .mapNotNull { id -> viewModel.session!!.columns.flatten().find { it.id == id }?.letter }
            .sorted()
        assertEquals(listOf('E', 'G', 'L'), hintLetters)
        assertEquals(1, viewModel.session?.hintsUsed)

        val gelTiles = tcTilesForWord(viewModel.session!!, "GEL")
        viewModel.tap(*gelTiles.toIntArray())
        assertEquals(listOf("TOE", "GEL"), viewModel.session?.foundWords?.map { it.word })
        assertTrue(viewModel.isDeadEnd)
    }

    @Test
    fun `fresh puzzle is drawn without the excluded themes`() {
        newViewModel(excludedThemes = setOf("food", "space"))

        coVerify { ThemeClearPuzzleBank.randomPuzzle(any(), any(), Difficulty.EASY, any(), setOf("food", "space")) }
    }
}
