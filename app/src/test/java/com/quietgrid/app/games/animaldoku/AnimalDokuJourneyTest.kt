package com.quietgrid.app.games.animaldoku

import android.content.Context
import com.quietgrid.app.MainDispatcherRule
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.journey.JourneyRequest
import com.quietgrid.app.core.journey.JourneySignals
import com.quietgrid.app.testutil.FakeHistoryStore
import com.quietgrid.app.testutil.FakeJourneyStore
import com.quietgrid.app.testutil.FakeSessionStore
import com.quietgrid.app.testutil.FakeStatsStore
import com.quietgrid.engine.animaldoku.AnimalDokuPuzzleEntry
import com.quietgrid.engine.animaldoku.transformed
import com.quietgrid.engine.journey.GridTransform
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

private val JOURNEY_PUZZLE = AnimalDokuPuzzleEntry(
    id = "journey-test",
    size = 5,
    difficulty = "medium",
    regions = listOf(
        listOf(4, 4, 4, 2, 2),
        listOf(4, 4, 2, 2, 2),
        listOf(4, 4, 2, 2, 0),
        listOf(1, 1, 3, 3, 0),
        listOf(1, 1, 1, 0, 0),
    ),
    solution = listOf(3, 1, 4, 2, 0),
)

class AnimalDokuJourneyTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val context = mockk<Context>(relaxed = true)

    @Before
    fun setUp() {
        mockkObject(AnimalDokuPuzzleBank)
        coEvery { AnimalDokuPuzzleBank.randomPuzzle(any(), any(), any()) } returns JOURNEY_PUZZLE.copy(id = "random")
        coEvery { AnimalDokuPuzzleBank.puzzleById(any(), Difficulty.MEDIUM, "journey-test") } returns JOURNEY_PUZZLE
    }

    @After
    fun tearDown() {
        unmockkObject(AnimalDokuPuzzleBank)
    }

    private fun newViewModel(journey: JourneyRequest? = null): AnimalDokuPlayViewModel = AnimalDokuPlayViewModel(
        context, FakeSessionStore(), FakeStatsStore(), FakeHistoryStore(), FakeJourneyStore(),
        Difficulty.MEDIUM, resume = false, dailyDate = null, journey = journey,
    ).also { it.hintDispatcher = mainDispatcherRule.dispatcher }

    @Test
    fun `journey start loads the stop puzzle by id`() {
        val viewModel = newViewModel(JourneyRequest("journey-test", "journey-test", 0))
        assertEquals("journey-test", viewModel.session?.puzzle?.id)
    }

    @Test
    fun `animaldoku hint counts once per shown hint and survives encode and restore`() {
        val viewModel = newViewModel()
        viewModel.toggleHint()
        assertNotNull(viewModel.hint)
        assertEquals(1, viewModel.session?.hintsUsed)

        viewModel.toggleHint()
        assertEquals(1, viewModel.session?.hintsUsed)

        viewModel.toggleHint()
        assertEquals(2, viewModel.session?.hintsUsed)

        val adapter = AnimalDokuPuzzleAdapter(context, FakeHistoryStore())
        val session = viewModel.session!!
        assertEquals(2, adapter.restoreSession(adapter.encode(session), 0.0)?.hintsUsed)
    }

    @Test
    fun `saved sessions from before hint counting restore with zero hints`() {
        val adapter = AnimalDokuPuzzleAdapter(context, FakeHistoryStore())
        val encoded = adapter.encode(createAnimalDokuSession(JOURNEY_PUZZLE)).replace(",\"hintsUsed\":0", "")
        assertEquals(0, adapter.restoreSession(encoded, 0.0)?.hintsUsed)
    }

    @Test
    fun `journey session with a transform rotates regions and solution`() {
        val viewModel = newViewModel(JourneyRequest("journey-test", "journey-test", GridTransform.ROTATE_90.id))
        val expected = JOURNEY_PUZZLE.transformed(GridTransform.ROTATE_90)
        assertEquals(expected.regions, viewModel.session?.puzzle?.regions)
        assertEquals(expected.solution, viewModel.session?.puzzle?.solution)
        assertEquals("journey-test", viewModel.session?.puzzle?.id)
    }

    @Test
    fun `hint works on a transformed session`() {
        val viewModel = newViewModel(JourneyRequest("journey-test", "journey-test", GridTransform.MIRROR_V.id))
        viewModel.toggleHint()
        assertNotNull(viewModel.hint)
    }

    @Test
    fun `animaldoku signals count hints and lost lives`() {
        val adapter = AnimalDokuPuzzleAdapter(context, FakeHistoryStore())
        val session = createAnimalDokuSession(JOURNEY_PUZZLE).copy(hintsUsed = 1, lives = ANIMALDOKU_STARTING_LIVES - 2)
        assertEquals(JourneySignals(hintsUsed = 1, mistakes = 2), adapter.journeySignals(session))
    }
}
