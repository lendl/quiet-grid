package com.quietgrid.app.games.starbattle

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
import com.quietgrid.engine.starbattle.StarBattlePuzzleEntry
import com.quietgrid.engine.starbattle.transformed
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

private val JOURNEY_PUZZLE = StarBattlePuzzleEntry(
    id = "journey-test",
    size = 8,
    difficulty = "medium",
    k = 2,
    regions = "1111000013100024333322243733444477355444755556667555566677776666".chunked(8).map { row -> row.map { it.digitToInt() } },
    solution = listOf(listOf(4, 6), listOf(0, 2), listOf(4, 6), listOf(0, 2), listOf(5, 7), listOf(1, 3), listOf(5, 7), listOf(1, 3)),
)

class StarBattleJourneyTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val context = mockk<Context>(relaxed = true)

    @Before
    fun setUp() {
        mockkObject(StarBattlePuzzleBank)
        coEvery { StarBattlePuzzleBank.randomPuzzle(any(), any(), any()) } returns JOURNEY_PUZZLE.copy(id = "random")
        coEvery { StarBattlePuzzleBank.puzzleById(any(), Difficulty.MEDIUM, "journey-test") } returns JOURNEY_PUZZLE
    }

    @After
    fun tearDown() {
        unmockkObject(StarBattlePuzzleBank)
    }

    private fun newViewModel(journey: JourneyRequest? = null): StarBattlePlayViewModel = StarBattlePlayViewModel(
        context, FakeSessionStore(), FakeStatsStore(), FakeHistoryStore(), FakeJourneyStore(),
        Difficulty.MEDIUM, resume = false, journey = journey,
    )

    @Test
    fun `journey start loads the stop puzzle by id`() {
        assertEquals("journey-test", newViewModel(JourneyRequest("journey-test", "journey-test", 0)).session?.puzzle?.id)
        assertEquals("random", newViewModel().session?.puzzle?.id)
    }

    @Test
    fun `journey session with a transform rotates regions and solution`() {
        val viewModel = newViewModel(JourneyRequest("journey-test", "journey-test", GridTransform.ROTATE_90.id))
        val expected = JOURNEY_PUZZLE.transformed(GridTransform.ROTATE_90)
        assertEquals(expected.regions, viewModel.session?.puzzle?.regions)
        assertEquals(expected.solution, viewModel.session?.puzzle?.solution)
    }

    @Test
    fun `star battle signals count lost lives as mistakes`() {
        val adapter = StarBattlePuzzleAdapter(context, FakeHistoryStore())
        val session = createStarBattleSession(JOURNEY_PUZZLE).copy(lives = STARBATTLE_STARTING_LIVES - 1)
        assertEquals(JourneySignals(hintsUsed = 0, mistakes = 1), adapter.journeySignals(session))
    }
}
