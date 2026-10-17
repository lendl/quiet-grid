package com.quietgrid.app.ui.play

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayLayoutTest {

    @Test
    fun `sample content areas map to the six layouts`() {
        assertEquals(PlaySizeClass.Compact, playSizeClass(360.dp, 616.dp))
        assertEquals(PlaySizeClass.Medium, playSizeClass(390.dp, 780.dp))
        assertEquals(PlaySizeClass.PhoneLandscape, playSizeClass(640.dp, 336.dp))
        assertEquals(PlaySizeClass.PhoneLandscape, playSizeClass(844.dp, 366.dp))
        assertEquals(PlaySizeClass.TabletLandscape, playSizeClass(1280.dp, 752.dp))
        assertEquals(PlaySizeClass.TabletPortrait, playSizeClass(800.dp, 1232.dp))
    }

    @Test
    fun `short upright split-screen window stays an upright phone layout`() {
        assertEquals(PlaySizeClass.Compact, playSizeClass(390.dp, 400.dp))
    }

    @Test
    fun `wide but short window never gets the tablet layout`() {
        assertEquals(PlaySizeClass.PhoneLandscape, playSizeClass(1280.dp, 400.dp))
    }

    @Test
    fun `unfolded foldable counts as a tablet`() {
        assertEquals(PlaySizeClass.TabletPortrait, playSizeClass(673.dp, 793.dp))
        assertEquals(PlaySizeClass.TabletLandscape, playSizeClass(841.dp, 625.dp))
    }

    @Test
    fun `board sits in the middle of the page when there is room`() {
        assertEquals(211.dp, centeredBoardTop(pageHeight = 780.dp, boardHeight = 358.dp, minTop = 96.dp, maxBottom = 764.dp))
    }

    @Test
    fun `board never slides under the header or the dock`() {
        assertEquals(96.dp, centeredBoardTop(pageHeight = 500.dp, boardHeight = 380.dp, minTop = 96.dp, maxBottom = 480.dp))
        assertEquals(150.dp, centeredBoardTop(pageHeight = 900.dp, boardHeight = 300.dp, minTop = 96.dp, maxBottom = 450.dp))
    }

    @Test
    fun `devices under 600dp smallest width count as phones`() {
        assertEquals(true, isPhoneDevice(390))
        assertEquals(true, isPhoneDevice(599))
        assertEquals(false, isPhoneDevice(600))
        assertEquals(false, isPhoneDevice(800))
    }

    @Test
    fun `square board takes the full width when the height allows it`() {
        assertEquals(366.dp, uprightBoardHeight(366.dp, 684.dp, boardAspectRatio = 1f, dockHeight = 0.dp))
    }

    @Test
    fun `dock height caps the board height`() {
        assertEquals(300.dp, uprightBoardHeight(366.dp, 500.dp, boardAspectRatio = 1f, dockHeight = 200.dp))
    }

    @Test
    fun `non-square board height follows its aspect ratio`() {
        assertEquals(480.dp, uprightBoardHeight(360.dp, 800.dp, boardAspectRatio = 0.75f, dockHeight = 0.dp))
    }

    @Test
    fun `board height never goes negative`() {
        assertEquals(0.dp, uprightBoardHeight(360.dp, 100.dp, boardAspectRatio = 1f, dockHeight = 200.dp))
    }

    @Test
    fun `side panels scale with the window`() {
        assertEquals(168.dp, phoneLandscapePanelWidth(640.dp))
        assertEquals(196.dp, phoneLandscapePanelWidth(844.dp))
        assertEquals(440.dp, tabletPanelWidth(1280.dp))
        assertEquals(302.4f, tabletPanelWidth(840.dp).value, 0.01f)
    }
}
