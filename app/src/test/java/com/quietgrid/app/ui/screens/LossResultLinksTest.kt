package com.quietgrid.app.ui.screens

import com.quietgrid.app.R
import org.junit.Assert.assertEquals
import org.junit.Test

class LossResultLinksTest {
    private fun labels(dailyDate: String? = null, isMixActive: Boolean = false, onBackToJourney: (() -> Unit)? = null) =
        lossResultLinks(
            dailyDate = dailyDate,
            hasNextDaily = false,
            isMixActive = isMixActive,
            onShareDaily = {},
            onBackToDaily = {},
            onOtherDifficulty = {},
            onTryAnotherGame = {},
            onBackToJourney = onBackToJourney,
        ).map { it.labelRes }

    @Test
    fun `journey loss shows back to map instead of other difficulty`() {
        assertEquals(listOf(R.string.journey_back_to_map, R.string.loss_try_another_game), labels(onBackToJourney = {}))
    }

    @Test
    fun `plain loss keeps other difficulty unless a mix is active`() {
        assertEquals(listOf(R.string.loss_other_difficulty, R.string.loss_try_another_game), labels())
        assertEquals(listOf(R.string.loss_try_another_game), labels(isMixActive = true))
    }
}
