package com.quietgrid.app.games.sudoku

import android.content.Context
import com.quietgrid.app.MainDispatcherRule
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.journey.JourneyRequest
import com.quietgrid.app.core.journey.JourneySignals
import com.quietgrid.app.games.minisudoku.MiniSudokuPuzzleBank
import com.quietgrid.app.testutil.FakeHistoryStore
import com.quietgrid.app.testutil.FakeJourneyStore
import com.quietgrid.app.testutil.FakeSessionStore
import com.quietgrid.app.testutil.FakeStatsStore
import com.quietgrid.engine.journey.GridTransform
import com.quietgrid.engine.sudoku.SudokuPuzzleEntry
import com.quietgrid.engine.sudoku.transformed
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

private val SOLUTION = listOf(
    "534678912", "672195348", "198342567", "859761423", "426853791", "713924856", "961537284", "287419635", "345286179",
).map { row -> row.map { it.digitToInt() } }

private val JOURNEY_PUZZLE = SudokuPuzzleEntry(
    id = "journey-test",
    difficulty = "medium",
    givens = SOLUTION.mapIndexed { r, row -> row.mapIndexed { c, v -> v.takeIf { (r + c) % 3 != 0 } } },
    solution = SOLUTION,
)

private val MINI_SOLUTION = listOf(
    listOf(1, 2, 3, 4, 5, 6),
    listOf(4, 5, 6, 1, 2, 3),
    listOf(2, 3, 1, 5, 6, 4),
    listOf(5, 6, 4, 2, 3, 1),
    listOf(3, 1, 2, 6, 4, 5),
    listOf(6, 4, 5, 3, 1, 2),
)

private val MINI_PUZZLE = SudokuPuzzleEntry(
    id = "mini-journey-test",
    difficulty = "medium",
    givens = MINI_SOLUTION.mapIndexed { r, row -> row.mapIndexed { c, v -> v.takeIf { (r + c) % 2 == 0 } } },
    solution = MINI_SOLUTION,
)

class SudokuJourneyTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val context = mockk<Context>(relaxed = true)

    @Before
    fun setUp() {
        mockkObject(SudokuPuzzleBank)
        mockkObject(MiniSudokuPuzzleBank)
        coEvery { SudokuPuzzleBank.randomPuzzle(any(), any(), any()) } returns JOURNEY_PUZZLE.copy(id = "random")
        coEvery { SudokuPuzzleBank.puzzleById(any(), Difficulty.MEDIUM, "journey-test") } returns JOURNEY_PUZZLE
        coEvery { MiniSudokuPuzzleBank.puzzleById(any(), Difficulty.MEDIUM, "mini-journey-test") } returns MINI_PUZZLE
    }

    @After
    fun tearDown() {
        unmockkObject(SudokuPuzzleBank)
        unmockkObject(MiniSudokuPuzzleBank)
    }

    private fun newViewModel(journey: JourneyRequest? = null, variant: SudokuVariant = SudokuVariant.CLASSIC): SudokuPlayViewModel = SudokuPlayViewModel(
        context, FakeSessionStore(), FakeStatsStore(), FakeHistoryStore(), FakeJourneyStore(),
        Difficulty.MEDIUM, resume = false, dailyDate = null, variant = variant, journey = journey,
    ).also { it.hintDispatcher = mainDispatcherRule.dispatcher }

    @Test
    fun `journey start loads the stop puzzle by id with its transform`() {
        val viewModel = newViewModel(JourneyRequest("journey-test", "journey-test", GridTransform.ROTATE_90.id))
        val expected = JOURNEY_PUZZLE.transformed(GridTransform.ROTATE_90)
        assertEquals("journey-test", viewModel.session?.puzzle?.id)
        assertEquals(expected.givens, viewModel.session?.puzzle?.givens)
        assertEquals(expected.solution, viewModel.session?.puzzle?.solution)
    }

    @Test
    fun `mini journey start reads the mini bank`() {
        val viewModel = newViewModel(JourneyRequest("mini-journey-test", "mini-journey-test", GridTransform.MIRROR_H.id), SudokuVariant.MINI)
        assertEquals(MINI_PUZZLE.transformed(GridTransform.MIRROR_H).givens, viewModel.session?.puzzle?.givens)
    }

    @Test
    fun `sudoku hint counts once per shown hint and survives encode and restore`() {
        val viewModel = newViewModel()
        viewModel.toggleNextMoveHint()
        assertNotNull(viewModel.nextMoveHint)
        assertEquals(1, viewModel.session?.hintsUsed)

        viewModel.toggleNextMoveHint()
        assertEquals(1, viewModel.session?.hintsUsed)

        viewModel.toggleNextMoveHint()
        assertEquals(2, viewModel.session?.hintsUsed)

        val adapter = SudokuPuzzleAdapter(context, FakeHistoryStore(), SudokuVariant.CLASSIC)
        assertEquals(2, kotlinx.coroutines.runBlocking { adapter.restoreSession(adapter.encode(viewModel.session!!), 0.0) }?.hintsUsed)
    }

    @Test
    fun `saved sessions from before hint counting restore with zero hints`() {
        val adapter = SudokuPuzzleAdapter(context, FakeHistoryStore(), SudokuVariant.CLASSIC)
        val encoded = adapter.encode(createSudokuSession(JOURNEY_PUZZLE)).replace(",\"hintsUsed\":0", "")
        assertEquals(0, kotlinx.coroutines.runBlocking { adapter.restoreSession(encoded, 0.0) }?.hintsUsed)
    }

    @Test
    fun `sudoku signals count hints and accuracy drops`() {
        val adapter = SudokuPuzzleAdapter(context, FakeHistoryStore(), SudokuVariant.CLASSIC)
        val session = createSudokuSession(JOURNEY_PUZZLE).copy(hintsUsed = 2, accuracyDrops = 3)
        assertEquals(JourneySignals(hintsUsed = 2, mistakes = 3), adapter.journeySignals(session))
    }
}
