package com.quietgrid.app.nav

import com.quietgrid.app.LaunchRequest
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.ActiveSessionSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchActionTest {
    private val session = ActiveSessionSummary("sudoku", null)

    @Test
    fun `daily tab always opens the daily tab`() {
        assertEquals(LaunchAction.OpenDailyTab, launchAction(LaunchRequest.DailyTab, session, emptySet(), onPlayScreen = true))
    }

    @Test
    fun `resume with a session navigates to its resume route`() {
        assertEquals(
            LaunchAction.Navigate("play/sudoku/easy/true", endsMix = false),
            launchAction(LaunchRequest.Resume, session, emptySet(), false),
        )
    }

    @Test
    fun `resume without a session opens tabs`() {
        assertEquals(LaunchAction.OpenTabs, launchAction(LaunchRequest.Resume, null, emptySet(), false))
    }

    @Test
    fun `resume while on a play screen stays`() {
        assertEquals(LaunchAction.Stay, launchAction(LaunchRequest.Resume, session, emptySet(), onPlayScreen = true))
    }

    @Test
    fun `start game starts a new puzzle and ends any mix, with or without a session or play screen`() {
        val expected = LaunchAction.Navigate("play/takuzu/medium/false", endsMix = true)
        val request = LaunchRequest.StartGame(GameId.TAKUZU, Difficulty.MEDIUM)
        assertEquals(expected, launchAction(request, null, emptySet(), false))
        assertEquals(expected, launchAction(request, session, emptySet(), false))
        assertEquals(expected, launchAction(request, session, emptySet(), onPlayScreen = true))
    }

    @Test
    fun `start mix starts that mix, with or without a session or play screen`() {
        val request = LaunchRequest.StartMix("mix-1")
        assertEquals(LaunchAction.StartMix("mix-1"), launchAction(request, null, setOf("mix-1"), false))
        assertEquals(LaunchAction.StartMix("mix-1"), launchAction(request, session, setOf("mix-1"), false))
        assertEquals(LaunchAction.StartMix("mix-1"), launchAction(request, session, setOf("mix-1"), onPlayScreen = true))
    }

    @Test
    fun `start mix for a deleted mix opens tabs`() {
        assertEquals(LaunchAction.OpenTabs, launchAction(LaunchRequest.StartMix("gone"), null, setOf("mix-1"), false))
    }

    @Test
    fun `a session for an unknown game counts as no session`() {
        assertEquals(
            LaunchAction.OpenTabs,
            launchAction(LaunchRequest.Resume, ActiveSessionSummary("flowfree", null), emptySet(), false),
        )
    }

    @Test
    fun `open mixes goes to the mixes tab even with a session or on a play screen`() {
        assertEquals(LaunchAction.OpenMixesTab, launchAction(LaunchRequest.OpenMixes, null, emptySet(), false))
        assertEquals(LaunchAction.OpenMixesTab, launchAction(LaunchRequest.OpenMixes, session, emptySet(), false))
        assertEquals(LaunchAction.OpenMixesTab, launchAction(LaunchRequest.OpenMixes, session, emptySet(), onPlayScreen = true))
    }
}
