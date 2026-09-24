package com.quietgrid.app.core.daily

import com.quietgrid.engine.wordguess.LetterState
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyShareLinesTest {

    @Test
    fun `lines join in order and skip nulls`() {
        assertEquals("H\nR\nS", joinDailyShareLines("H", "R", null, "S"))
        assertEquals("H\nR\nD\nS", joinDailyShareLines("H", "R", "D", "S"))
        assertEquals("H\nR", joinDailyShareLines("H", "R", null, null))
    }

    @Test
    fun `word guess grid maps states to squares one row per guess`() {
        val rows = listOf(
            listOf(LetterState.ABSENT, LetterState.PRESENT, LetterState.CORRECT),
            listOf(LetterState.CORRECT, LetterState.CORRECT, LetterState.CORRECT),
        )
        assertEquals("\u2B1B\uD83D\uDFE8\uD83D\uDFE9\n\uD83D\uDFE9\uD83D\uDFE9\uD83D\uDFE9", wordGuessShareGrid(rows))
    }
}
