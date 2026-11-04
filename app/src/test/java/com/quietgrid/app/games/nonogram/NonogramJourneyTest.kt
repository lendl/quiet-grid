package com.quietgrid.app.games.nonogram

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
import com.quietgrid.engine.nonogram.NonogramPuzzleEntry
import com.quietgrid.engine.nonogram.transformed
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

private val JOURNEY_PUZZLE = NonogramPuzzleEntry(
    id = "journey-test",
    difficulty = "hard",
    rows = 10,
    cols = 5,
    solution = listOf("00000", "01010", "01011", "11111", "11111", "01111", "00100", "00111", "00110", "00100")
        .map { row -> row.map { it == '1' } },
)

class NonogramJourneyTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val context = mockk<Context>(relaxed = true)

    @Before
    fun setUp() {
        mockkObject(NonogramPuzzleBank)
        coEvery { NonogramPuzzleBank.randomPuzzle(any(), any(), any()) } returns JOURNEY_PUZZLE.copy(id = "random")
        coEvery { NonogramPuzzleBank.puzzleById(any(), Difficulty.HARD, "journey-test") } returns JOURNEY_PUZZLE
    }

    @After
    fun tearDown() {
        unmockkObject(NonogramPuzzleBank)
    }

    private fun newViewModel(journey: JourneyRequest? = null): NonogramPlayViewModel = NonogramPlayViewModel(
        context, FakeSessionStore(), FakeStatsStore(), FakeHistoryStore(), FakeJourneyStore(),
        Difficulty.HARD, resume = false, journey = journey,
    ).also { it.hintDispatcher = mainDispatcherRule.dispatcher }

    @Test
    fun `journey start loads the stop puzzle by id with its transform`() {
        val viewModel = newViewModel(JourneyRequest("journey-test", "journey-test", GridTransform.ROTATE_180.id))
        assertEquals("journey-test", viewModel.session?.puzzle?.id)
        assertEquals(JOURNEY_PUZZLE.transformed(GridTransform.ROTATE_180).solution, viewModel.session?.solution)
    }

    @Test
    fun `nonogram hint counts once per shown hint and survives encode and restore`() {
        val viewModel = newViewModel()
        viewModel.toggleNextMoveHint()
        assertNotNull(viewModel.nextMoveHint)
        assertEquals(1, viewModel.session?.hintsUsed)

        viewModel.toggleNextMoveHint()
        viewModel.toggleNextMoveHint()
        assertEquals(2, viewModel.session?.hintsUsed)

        val adapter = NonogramPuzzleAdapter(context, FakeHistoryStore())
        assertEquals(2, adapter.restoreSession(adapter.encode(viewModel.session!!), 0.0)?.hintsUsed)
    }

    @Test
    fun `nonogram signals count hints and never mistakes`() {
        val adapter = NonogramPuzzleAdapter(context, FakeHistoryStore())
        val session = createNonogramSession(JOURNEY_PUZZLE).copy(hintsUsed = 2)
        assertEquals(JourneySignals(hintsUsed = 2, mistakes = 0), adapter.journeySignals(session))
    }
}
