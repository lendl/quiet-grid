package com.quietgrid.app.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.datetime.LocalDate

class SeasonalTest {

    @Test
    fun halloweenIsOnlyTheThirtyFirstOfOctober() {
        assertFalse(isHalloween(LocalDate(2026, 10, 30)))
        assertTrue(isHalloween(LocalDate(2026, 10, 31)))
        assertFalse(isHalloween(LocalDate(2026, 11, 1)))
        assertFalse(isHalloween(LocalDate(2026, 3, 31)))
    }

    @Test
    fun halloweenRepeatsEveryYear() {
        assertTrue(isHalloween(LocalDate(2027, 10, 31)))
        assertTrue(isHalloween(LocalDate(2028, 10, 31)))
    }
}
