package com.quietgrid.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CompletionExtrasTest {
    @Test
    fun caseSolvedIsReturnedOnceThenCleared() {
        CompletionExtras.set(CompletionHighlight.CaseSolved("It was the cook."))
        assertEquals(CompletionHighlight.CaseSolved("It was the cook."), CompletionExtras.consume())
        assertNull(CompletionExtras.consume())
    }
}
