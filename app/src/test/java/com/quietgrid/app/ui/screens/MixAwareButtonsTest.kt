package com.quietgrid.app.ui.screens

import com.quietgrid.app.R
import org.junit.Assert.assertEquals
import org.junit.Test

class MixAwareButtonsTest {

    @Test
    fun `returns the default label when no mix is active`() {
        assertEquals(R.string.completion_play_again, mixAwarePrimaryLabel(isMixActive = false, defaultLabelRes = R.string.completion_play_again))
    }

    @Test
    fun `returns the shared next-puzzle label when a mix is active`() {
        assertEquals(R.string.mix_next_puzzle, mixAwarePrimaryLabel(isMixActive = true, defaultLabelRes = R.string.completion_play_again))
    }
}
