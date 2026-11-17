package com.quietgrid.app.games.links

import android.content.Context
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.journey.JourneySignals
import com.quietgrid.app.testutil.FakeHistoryStore
import com.quietgrid.engine.links.LinksPuzzleEntry
import com.quietgrid.engine.links.linksEndpointsOf
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

private val SOLUTION = listOf(listOf(0, 1, 2), listOf(3, 6, 7, 8, 5, 4))

private val PUZZLE = LinksPuzzleEntry(
    id = "stop",
    size = 3,
    difficulty = "medium",
    endpoints = linksEndpointsOf(SOLUTION),
    solution = SOLUTION,
)

class LinksJourneyTest {

    private val adapter = LinksPuzzleAdapter(mockk<Context>(relaxed = true), FakeHistoryStore())

    @Before
    fun setUp() {
        mockkObject(LinksPuzzleBank)
        coEvery { LinksPuzzleBank.puzzleById(any(), any(), any()) } returns null
        coEvery { LinksPuzzleBank.puzzleById(any(), Difficulty.MEDIUM, "stop") } returns PUZZLE
    }

    @After
    fun tearDown() {
        unmockkObject(LinksPuzzleBank)
    }

    @Test
    fun `journey start loads the stop puzzle by id`() {
        assertEquals(PUZZLE, runBlocking { adapter.journeySession(Difficulty.MEDIUM, "stop", 0) }?.puzzle)
        assertNull(runBlocking { adapter.journeySession(Difficulty.MEDIUM, "missing", 0) })
    }

    @Test
    fun `journey signals count hints only`() {
        val session = createLinksSession(PUZZLE).copy(hintsUsed = 2)
        assertEquals(JourneySignals(hintsUsed = 2, mistakes = 0), adapter.journeySignals(session))
    }
}