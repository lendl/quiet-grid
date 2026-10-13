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
    fun `start game without a session starts it and ends any mix`() {
        assertEquals(
            LaunchAction.Navigate("play/takuzu/medium/false", endsMix = true),
            launchAction(LaunchRequest.StartGame(GameId.TAKUZU, Difficulty.MEDIUM), null, emptySet(), false),
        )
    }

    @Test
    fun `start game while a session exists resumes it`() {
        assertEquals(
            LaunchAction.Navigate("play/sudoku/easy/true", endsMix = false),
            launchAction(LaunchRequest.StartGame(GameId.TAKUZU, Difficulty.MEDIUM), session, emptySet(), false),
        )
    }

    @Test
    fun `start mix without a session starts that mix`() {
        assertEquals(LaunchAction.StartMix("mix-1"), launchAction(LaunchRequest.StartMix("mix-1"), null, setOf("mix-1"), false))
    }

    @Test
    fun `start mix while a session exists resumes it`() {
        assertEquals(
            LaunchAction.Navigate("play/sudoku/easy/true", endsMix = false),
            launchAction(LaunchRequest.StartMix("mix-1"), session, setOf("mix-1"), false),
        )
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
    fun `open games goes to the games tab even with a session or on a play screen`() {
        assertEquals(LaunchAction.OpenGamesTab, launchAction(LaunchRequest.OpenGames, null, emptySet(), false))
        assertEquals(LaunchAction.OpenGamesTab, launchAction(LaunchRequest.OpenGames, session, emptySet(), false))
        assertEquals(LaunchAction.OpenGamesTab, launchAction(LaunchRequest.OpenGames, session, emptySet(), onPlayScreen = true))
    }

    @Test
    fun `any widget request while on a play screen stays`() {
        listOf(LaunchRequest.Resume, LaunchRequest.StartMix("mix-1"), LaunchRequest.StartGame(GameId.SUDOKU, Difficulty.EASY))
            .forEach { assertEquals(LaunchAction.Stay, launchAction(it, session, setOf("mix-1"), onPlayScreen = true)) }
    }
}
