package com.quietgrid.app.games.wordswap

import android.content.Context
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.journey.JourneySignals
import com.quietgrid.app.testutil.FakeHistoryStore
import com.quietgrid.engine.wordswap.WordSwapPuzzleEntry
import com.quietgrid.engine.wordswap.wordSwapLineSlots
import com.quietgrid.engine.wordswap.wordSwapMinSwaps
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

private const val SOLUTION = "bat" + "a.o" + "dog"
private const val START = "tab" + "o.a" + "dgo"

private val PUZZLE = WordSwapPuzzleEntry(
    id = "stop",
    difficulty = "medium",
    rows = 3,
    cols = 3,
    solution = SOLUTION,
    start = START,
    slots = wordSwapLineSlots(SOLUTION, 3, 3),
    minSwaps = wordSwapMinSwaps(START, SOLUTION),
)

class WordSwapJourneyTest {

    private val adapter = WordSwapPuzzleAdapter(mockk<Context>(relaxed = true), FakeHistoryStore()) { "nl" }

    @Before
    fun setUp() {
        mockkObject(WordSwapPuzzleBank)
        coEvery { WordSwapPuzzleBank.puzzleById(any(), any(), any()) } returns null
        coEvery { WordSwapPuzzleBank.puzzleById(any(), Difficulty.MEDIUM, "stop") } returns PUZZLE
        coEvery { WordSwapPuzzleBank.randomPuzzle(any(), any(), any(), any()) } returns null
        coEvery { WordSwapPuzzleBank.randomPuzzle(any(), "nl", Difficulty.MEDIUM, any()) } returns PUZZLE
    }

    @After
    fun tearDown() {
        unmockkObject(WordSwapPuzzleBank)
    }

    @Test
    fun `journey start loads the stop puzzle by id`() {
        assertEquals(PUZZLE, runBlocking { adapter.journeySession(Difficulty.MEDIUM, "stop", 0) }?.puzzle)
        assertNull(runBlocking { adapter.journeySession(Difficulty.MEDIUM, "missing", 0) })
    }

    @Test
    fun `fresh puzzle comes from the puzzle language bank`() {
        assertEquals(PUZZLE, runBlocking { adapter.freshSession(Difficulty.MEDIUM) }?.puzzle)
    }

    @Test
    fun `journey signals count hints only`() {
        val session = createWordSwapSession(PUZZLE).copy(hintsUsed = 2)
        assertEquals(JourneySignals(hintsUsed = 2, mistakes = 0), adapter.journeySignals(session))
    }
}