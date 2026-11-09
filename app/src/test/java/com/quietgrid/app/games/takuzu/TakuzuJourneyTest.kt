package com.quietgrid.app.games.takuzu

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
import com.quietgrid.engine.takuzu.TakuzuPuzzleEntry
import com.quietgrid.engine.takuzu.transformed
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

private val JOURNEY_PUZZLE = TakuzuPuzzleEntry("journey-test", 6, "medium", "68dd2a4e5", "140c29020")

class TakuzuJourneyTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val context = mockk<Context>(relaxed = true)

    @Before
    fun setUp() {
        mockkObject(TakuzuPuzzleBank)
        coEvery { TakuzuPuzzleBank.randomPuzzle(any(), any(), any()) } returns JOURNEY_PUZZLE.copy(id = "random")
        coEvery { TakuzuPuzzleBank.puzzleById(any(), Difficulty.MEDIUM, "journey-test") } returns JOURNEY_PUZZLE
    }

    @After
    fun tearDown() {
        unmockkObject(TakuzuPuzzleBank)
    }

    private fun newViewModel(journey: JourneyRequest? = null): TakuzuPlayViewModel = TakuzuPlayViewModel(
        context, FakeSessionStore(), FakeStatsStore(), FakeHistoryStore(), FakeJourneyStore(),
        Difficulty.MEDIUM, resume = false, dailyDate = null, journey = journey,
    ).also { it.hintDispatcher = mainDispatcherRule.dispatcher }

    @Test
    fun `journey start loads the stop puzzle by id with its transform`() {
        val viewModel = newViewModel(JourneyRequest("journey-test", "journey-test", GridTransform.ROTATE_90.id))
        val expected = JOURNEY_PUZZLE.transformed(GridTransform.ROTATE_90)
        assertEquals("journey-test", viewModel.session?.puzzle?.id)
        assertEquals(expected.mask, viewModel.session?.puzzle?.mask)
        assertEquals(expected.solution, viewModel.session?.puzzle?.solution)
    }

    @Test
    fun `takuzu hint counts once per shown hint and survives encode and restore`() {
        val viewModel = newViewModel()
        viewModel.toggleNextMoveHint()
        assertNotNull(viewModel.nextMoveHint)
        assertEquals(1, viewModel.session?.hintsUsed)

        viewModel.toggleNextMoveHint()
        viewModel.toggleNextMoveHint()
        assertEquals(2, viewModel.session?.hintsUsed)

        val adapter = TakuzuPuzzleAdapter(context, FakeHistoryStore())
        assertEquals(2, kotlinx.coroutines.runBlocking { adapter.restoreSession(adapter.encode(viewModel.session!!), 0.0) }?.hintsUsed)
    }

    @Test
    fun `takuzu signals count hints and accuracy drops`() {
        val adapter = TakuzuPuzzleAdapter(context, FakeHistoryStore())
        val session = createTakuzuSession(JOURNEY_PUZZLE).copy(hintsUsed = 3, accuracyDrops = 1)
        assertEquals(JourneySignals(hintsUsed = 3, mistakes = 1), adapter.journeySignals(session))
    }
}
