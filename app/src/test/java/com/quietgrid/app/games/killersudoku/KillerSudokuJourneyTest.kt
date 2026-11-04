package com.quietgrid.app.games.killersudoku

import android.content.Context
import com.quietgrid.app.MainDispatcherRule
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.journey.JourneyRequest
import com.quietgrid.app.core.journey.JourneySignals
import com.quietgrid.app.testutil.FakeHistoryStore
import com.quietgrid.app.testutil.FakeJourneyStore
import com.quietgrid.app.testutil.FakeSessionStore
import com.quietgrid.app.testutil.FakeStatsStore
import com.quietgrid.engine.journey.GridTransform
import com.quietgrid.engine.killersudoku.KillerSudokuCage
import com.quietgrid.engine.killersudoku.KillerSudokuPuzzleEntry
import com.quietgrid.engine.killersudoku.transformed
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

private val JOURNEY_PUZZLE = KillerSudokuPuzzleEntry(
    id = "journey-test",
    difficulty = "easy",
    cages = listOf(
        KillerSudokuCage(13, listOf(0, 9, 18)),
        KillerSudokuCage(11, listOf(1, 10)),
        KillerSudokuCage(15, listOf(2, 3, 11, 12)),
        KillerSudokuCage(15, listOf(4, 13, 14)),
        KillerSudokuCage(17, listOf(5, 6)),
        KillerSudokuCage(12, listOf(7, 8, 17)),
        KillerSudokuCage(9, listOf(15, 24)),
        KillerSudokuCage(15, listOf(16, 25)),
        KillerSudokuCage(17, listOf(19, 20, 21)),
        KillerSudokuCage(10, listOf(22, 23)),
        KillerSudokuCage(7, listOf(26, 35)),
        KillerSudokuCage(16, listOf(27, 28, 29)),
        KillerSudokuCage(10, listOf(30, 31)),
        KillerSudokuCage(13, listOf(32, 33, 34)),
        KillerSudokuCage(15, listOf(36, 45, 54)),
        KillerSudokuCage(14, listOf(37, 38, 39)),
        KillerSudokuCage(9, listOf(40, 41)),
        KillerSudokuCage(5, listOf(42, 43)),
        KillerSudokuCage(29, listOf(44, 53, 62, 71)),
        KillerSudokuCage(10, listOf(46, 47)),
        KillerSudokuCage(12, listOf(48, 49)),
        KillerSudokuCage(13, listOf(50, 51)),
        KillerSudokuCage(6, listOf(52, 61, 70)),
        KillerSudokuCage(17, listOf(55, 64)),
        KillerSudokuCage(10, listOf(56, 57)),
        KillerSudokuCage(10, listOf(58, 59)),
        KillerSudokuCage(14, listOf(60, 68, 69)),
        KillerSudokuCage(16, listOf(63, 72, 73)),
        KillerSudokuCage(3, listOf(65, 74)),
        KillerSudokuCage(7, listOf(66, 67)),
        KillerSudokuCage(9, listOf(75, 76)),
        KillerSudokuCage(17, listOf(77, 78)),
        KillerSudokuCage(9, listOf(79, 80)),
    ),
    solution = listOf("146359872", "975182463", "328764591", "457913286", "863527149", "219846735", "584691327", "792435618", "631278954")
        .map { row -> row.map { it.digitToInt() } },
)

class KillerSudokuJourneyTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val context = mockk<Context>(relaxed = true)

    @Before
    fun setUp() {
        mockkObject(KillerSudokuPuzzleBank)
        coEvery { KillerSudokuPuzzleBank.randomPuzzle(any(), any(), any()) } returns JOURNEY_PUZZLE.copy(id = "random")
        coEvery { KillerSudokuPuzzleBank.puzzleById(any(), Difficulty.EASY, "journey-test") } returns JOURNEY_PUZZLE
    }

    @After
    fun tearDown() {
        unmockkObject(KillerSudokuPuzzleBank)
    }

    private fun newViewModel(journey: JourneyRequest? = null): KillerSudokuPlayViewModel = KillerSudokuPlayViewModel(
        context, FakeSessionStore(), FakeStatsStore(), FakeHistoryStore(), FakeJourneyStore(),
        Difficulty.EASY, resume = false, journey = journey,
    ).also { it.hintDispatcher = mainDispatcherRule.dispatcher }

    @Test
    fun `journey start loads the stop puzzle by id with its transform`() {
        val viewModel = newViewModel(JourneyRequest("journey-test", "journey-test", GridTransform.ROTATE_90.id))
        val expected = JOURNEY_PUZZLE.transformed(GridTransform.ROTATE_90)
        assertEquals("journey-test", viewModel.session?.puzzle?.id)
        assertEquals(expected.cages, viewModel.session?.puzzle?.cages)
        assertEquals(expected.solution, viewModel.session?.grid?.puzzle?.solution)
    }

    @Test
    fun `killer hint counts once per shown hint and survives encode and restore`() {
        val viewModel = newViewModel()
        viewModel.toggleHint()
        assertNotNull(viewModel.hint)
        assertEquals(1, viewModel.session?.grid?.hintsUsed)

        viewModel.toggleHint()
        viewModel.toggleHint()
        assertEquals(2, viewModel.session?.grid?.hintsUsed)

        val adapter = KillerSudokuPuzzleAdapter(context, FakeHistoryStore())
        assertEquals(2, adapter.restoreSession(adapter.encode(viewModel.session!!), 0.0)?.grid?.hintsUsed)
    }

    @Test
    fun `killer signals count hints and accuracy drops`() {
        val adapter = KillerSudokuPuzzleAdapter(context, FakeHistoryStore())
        val base = createKillerSudokuSession(JOURNEY_PUZZLE)
        val session = base.copy(grid = base.grid.copy(hintsUsed = 1, accuracyDrops = 2))
        assertEquals(JourneySignals(hintsUsed = 1, mistakes = 2), adapter.journeySignals(session))
    }
}
