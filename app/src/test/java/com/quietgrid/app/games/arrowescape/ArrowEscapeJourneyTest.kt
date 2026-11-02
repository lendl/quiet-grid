package com.quietgrid.app.games.arrowescape

import android.content.Context
import com.quietgrid.app.MainDispatcherRule
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.journey.JourneyRequest
import com.quietgrid.app.core.journey.JourneySignals
import com.quietgrid.app.data.AppSettings
import com.quietgrid.app.data.SettingsRepository
import com.quietgrid.app.testutil.FakeHistoryStore
import com.quietgrid.app.testutil.FakeJourneyStore
import com.quietgrid.app.testutil.FakeSessionStore
import com.quietgrid.app.testutil.FakeStatsStore
import com.quietgrid.engine.arrowescape.ArrowEscapePieceData
import com.quietgrid.engine.arrowescape.ArrowEscapePuzzleEntry
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

private fun puzzle(id: String) = ArrowEscapePuzzleEntry(
    id = id,
    difficulty = Difficulty.HARD.key,
    rows = 2,
    cols = 1,
    pieces = listOf(
        ArrowEscapePieceData(cells = listOf(listOf(0, 0)), headDirection = "down"),
        ArrowEscapePieceData(cells = listOf(listOf(1, 0)), headDirection = "down"),
    ),
)

private val ASYMMETRIC = ArrowEscapePuzzleEntry(
    id = "ae-mirror",
    difficulty = Difficulty.HARD.key,
    rows = 1,
    cols = 3,
    pieces = listOf(ArrowEscapePieceData(cells = listOf(listOf(0, 0), listOf(0, 1)), headDirection = "right")),
    shape = "test",
    mask = listOf("##."),
)

class ArrowEscapeJourneyTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val context = mockk<Context>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true).also {
        every { it.settings } returns MutableStateFlow(AppSettings())
    }

    @Before
    fun setUp() {
        mockkObject(ArrowEscapePuzzleBank)
        coEvery { ArrowEscapePuzzleBank.randomPuzzle(any(), any(), any()) } returns puzzle("random")
        coEvery { ArrowEscapePuzzleBank.halloweenPuzzle(any(), any(), any()) } returns null
        coEvery { ArrowEscapePuzzleBank.puzzleById(any(), Difficulty.HARD, "ae-stop") } returns puzzle("ae-stop")
        coEvery { ArrowEscapePuzzleBank.puzzleById(any(), Difficulty.HARD, "ae-mirror") } returns ASYMMETRIC
    }

    @After
    fun tearDown() {
        unmockkObject(ArrowEscapePuzzleBank)
    }

    @Test
    fun `journey start loads the stop puzzle by id`() {
        val viewModel = ArrowEscapePlayViewModel(
            context, FakeSessionStore(), FakeStatsStore(), FakeHistoryStore(), settingsRepository, FakeJourneyStore(),
            Difficulty.HARD, resume = false, journey = JourneyRequest("ae-stop", "ae-stop", 0),
        )
        assertEquals("ae-stop", viewModel.session?.puzzle?.id)
    }

    @Test
    fun `journey session with transform 1 is mirrored`() = kotlinx.coroutines.runBlocking {
        val adapter = ArrowEscapePuzzleAdapter(context, FakeHistoryStore(), settingsRepository)
        val session = adapter.journeySession(Difficulty.HARD, "ae-mirror", 1)!!
        assertEquals("ae-mirror", session.puzzle.id)
        assertEquals(listOf(listOf(0, 2), listOf(0, 1)), session.puzzle.pieces.single().cells)
        assertEquals("left", session.puzzle.pieces.single().headDirection)
        assertEquals(listOf(".##"), session.puzzle.mask)
    }

    @Test
    fun `the same request builds the same board twice`() = kotlinx.coroutines.runBlocking {
        val adapter = ArrowEscapePuzzleAdapter(context, FakeHistoryStore(), settingsRepository)
        assertEquals(adapter.journeySession(Difficulty.HARD, "ae-mirror", 1)!!.puzzle, adapter.journeySession(Difficulty.HARD, "ae-mirror", 1)!!.puzzle)
        assertEquals(ASYMMETRIC, adapter.journeySession(Difficulty.HARD, "ae-mirror", 0)!!.puzzle)
    }

    @Test
    fun `arrowescape signals count hints and lost lives`() {
        val adapter = ArrowEscapePuzzleAdapter(context, FakeHistoryStore(), settingsRepository)
        val session = createArrowEscapeSession(puzzle("ae-stop")).copy(hintsUsed = 2, lives = ARROW_ESCAPE_STARTING_LIVES - 1)
        assertEquals(JourneySignals(hintsUsed = 2, mistakes = 1), adapter.journeySignals(session))
    }
}
