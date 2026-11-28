package com.quietgrid.app.games.circuit

import com.quietgrid.app.platform.BundledFiles
import android.content.Context
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.journey.JourneySignals
import com.quietgrid.app.testutil.FakeHistoryStore
import com.quietgrid.engine.circuit.CIRCUIT_DOWN
import com.quietgrid.engine.circuit.CIRCUIT_LEFT
import com.quietgrid.engine.circuit.CIRCUIT_RIGHT
import com.quietgrid.engine.circuit.CIRCUIT_UP
import com.quietgrid.engine.circuit.CircuitPuzzleEntry
import com.quietgrid.engine.circuit.rotateCircuitMask
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

private val SOLUTION = listOf(
    CIRCUIT_RIGHT or CIRCUIT_DOWN, CIRCUIT_LEFT or CIRCUIT_RIGHT or CIRCUIT_DOWN, CIRCUIT_LEFT or CIRCUIT_DOWN,
    CIRCUIT_UP or CIRCUIT_DOWN, CIRCUIT_UP or CIRCUIT_DOWN, CIRCUIT_UP or CIRCUIT_DOWN,
    CIRCUIT_UP, CIRCUIT_UP, CIRCUIT_UP,
)

private val PUZZLE = CircuitPuzzleEntry(
    id = "stop",
    rows = 3,
    cols = 3,
    difficulty = "medium",
    source = 4,
    solution = SOLUTION,
    start = SOLUTION.map { rotateCircuitMask(it, 3) },
)

class CircuitJourneyTest {

    private val adapter = CircuitPuzzleAdapter(mockk<BundledFiles>(relaxed = true), FakeHistoryStore())

    @Before
    fun setUp() {
        mockkObject(CircuitPuzzleBank)
        coEvery { CircuitPuzzleBank.puzzleById(any(), any(), any()) } returns null
        coEvery { CircuitPuzzleBank.puzzleById(any(), Difficulty.MEDIUM, "stop") } returns PUZZLE
    }

    @After
    fun tearDown() {
        unmockkObject(CircuitPuzzleBank)
    }

    @Test
    fun `journey start loads the stop puzzle by id`() {
        assertEquals(PUZZLE, runBlocking { adapter.journeySession(Difficulty.MEDIUM, "stop", 0) }?.puzzle)
        assertNull(runBlocking { adapter.journeySession(Difficulty.MEDIUM, "missing", 0) })
    }

    @Test
    fun `journey signals count hints only`() {
        val session = createCircuitSession(PUZZLE).copy(hintsUsed = 2)
        assertEquals(JourneySignals(hintsUsed = 2, mistakes = 0), adapter.journeySignals(session))
    }
}