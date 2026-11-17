package com.quietgrid.app.games.blockescape

import android.content.Context
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.journey.JourneySignals
import com.quietgrid.app.testutil.FakeHistoryStore
import com.quietgrid.engine.blockescape.BlockEscapeBlock
import com.quietgrid.engine.blockescape.BlockEscapePuzzleEntry
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

private val PUZZLE = BlockEscapePuzzleEntry(
    id = "stop",
    difficulty = "medium",
    blocks = listOf(
        BlockEscapeBlock(2, 0, 2, true),
        BlockEscapeBlock(1, 2, 2, false),
        BlockEscapeBlock(2, 4, 3, false),
        BlockEscapeBlock(5, 3, 2, true),
        BlockEscapeBlock(0, 1, 3, true),
        BlockEscapeBlock(3, 2, 2, false),
        BlockEscapeBlock(4, 0, 2, true),
    ),
    minMoves = 5,
)

class BlockEscapeJourneyTest {

    private val adapter = BlockEscapePuzzleAdapter(mockk<Context>(relaxed = true), FakeHistoryStore())

    @Before
    fun setUp() {
        mockkObject(BlockEscapePuzzleBank)
        coEvery { BlockEscapePuzzleBank.puzzleById(any(), any(), any()) } returns null
        coEvery { BlockEscapePuzzleBank.puzzleById(any(), Difficulty.MEDIUM, "stop") } returns PUZZLE
    }

    @After
    fun tearDown() {
        unmockkObject(BlockEscapePuzzleBank)
    }

    @Test
    fun `journey start loads the stop puzzle by id`() {
        assertEquals(PUZZLE, runBlocking { adapter.journeySession(Difficulty.MEDIUM, "stop", 0) }?.puzzle)
        assertNull(runBlocking { adapter.journeySession(Difficulty.MEDIUM, "missing", 0) })
    }

    @Test
    fun `journey signals count hints only`() {
        val session = createBlockEscapeSession(PUZZLE).copy(hintsUsed = 2)
        assertEquals(JourneySignals(hintsUsed = 2, mistakes = 0), adapter.journeySignals(session))
    }
}