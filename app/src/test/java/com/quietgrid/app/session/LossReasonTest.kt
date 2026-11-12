package com.quietgrid.app.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LossReasonTest {
    @Test
    fun `keys match the strings stored in play history`() {
        assertEquals(
            mapOf(
                LossReason.ABANDONED to "abandoned",
                LossReason.RULE_FAILURE to "rule-failure",
                LossReason.NO_MOVES_LEFT to "no-moves-left",
                LossReason.OUT_OF_LIVES to "out_of_lives",
                LossReason.HEARTS_EXHAUSTED to "hearts_exhausted",
            ),
            LossReason.entries.associateWith { it.key },
        )
    }

    @Test
    fun `fromKey round-trips and rejects unknown keys`() {
        LossReason.entries.forEach { assertEquals(it, LossReason.fromKey(it.key)) }
        assertNull(LossReason.fromKey("time_up"))
        assertNull(LossReason.fromKey(null))
    }
}
