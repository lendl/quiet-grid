package com.quietgrid.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class StatsOverviewTest {
    @Test
    fun `best score cell shows a dash when nothing was solved`() {
        assertEquals("-", bestScoreCellText(0))
    }

    @Test
    fun `best score cell shows the raw score otherwise`() {
        assertEquals("1240", bestScoreCellText(1240))
    }
}
