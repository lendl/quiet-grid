package com.quietgrid.app.data

import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.tips.QuietTipState
import com.quietgrid.app.core.tips.rotationTipKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

class QuietTipsRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val today = LocalDate(2026, 11, 14)

    @Test
    fun `a fresh install has no tip history`() = runTest {
        val repository = QuietTipsRepository(newDataStore(backgroundScope))

        assertEquals(QuietTipState(), repository.state().first())
    }

    @Test
    fun `signals are counted per game`() = runTest {
        val repository = QuietTipsRepository(newDataStore(backgroundScope))

        repository.recordRotationBounce(GameId.SUDOKU, ScreenRotation.PORTRAIT)
        repository.recordRotationBounce(GameId.SUDOKU, ScreenRotation.LANDSCAPE)
        repository.recordScreenTimeout(GameId.NONOGRAM)

        val state = repository.state().first()
        assertEquals(mapOf(GameId.SUDOKU to 2), state.rotationBounces)
        assertEquals(mapOf(GameId.SUDOKU to ScreenRotation.LANDSCAPE), state.rotationTargets)
        assertEquals(mapOf(GameId.NONOGRAM to 1), state.screenTimeouts)
    }

    @Test
    fun `a slow exit resets the early exit run`() = runTest {
        val repository = QuietTipsRepository(newDataStore(backgroundScope))

        repeat(3) { repository.recordResultExit(early = true) }
        assertEquals(3, repository.state().first().earlyExitsInRow)

        repository.recordResultExit(early = false)
        assertEquals(0, repository.state().first().earlyExitsInRow)
    }

    @Test
    fun `showing a tip resets the result gap and counts the showing`() = runTest {
        val repository = QuietTipsRepository(newDataStore(backgroundScope))
        val key = rotationTipKey(GameId.SUDOKU)

        repeat(4) { repository.recordResult() }
        repository.markShown(key, today)
        repository.recordResult()
        repository.markShown(key, today.plus(2, DateTimeUnit.DAY))

        val state = repository.state().first()
        assertEquals(0, state.resultsSinceLast)
        assertEquals(today.plus(2, DateTimeUnit.DAY), state.lastShownDay)
        assertEquals(mapOf(key to 2), state.shownCounts)
    }

    @Test
    fun `close, reopen, open and snooze update the tip sets`() = runTest {
        val repository = QuietTipsRepository(newDataStore(backgroundScope))
        val key = rotationTipKey(GameId.SUDOKU)

        repository.close(key)
        assertTrue(key in repository.state().first().closedKeys)
        repository.reopen(key)
        assertFalse(key in repository.state().first().closedKeys)

        repository.markOpened(key)
        assertTrue(key in repository.state().first().openedKeys)
        repository.snooze(key, today.plus(7, DateTimeUnit.DAY))

        val state = repository.state().first()
        assertFalse(key in state.openedKeys)
        assertEquals(mapOf(key to today.plus(7, DateTimeUnit.DAY)), state.snoozedUntil)
    }

    @Test
    fun `the daily tab is remembered once opened`() = runTest {
        val repository = QuietTipsRepository(newDataStore(backgroundScope))

        repository.markDailyTabOpened()

        assertTrue(repository.state().first().dailyTabOpened)
    }

    private fun newDataStore(scope: CoroutineScope) =
        preferencesDataStoreForTest(scope) { tempFolder.newFile("quiet_tips_test.preferences_pb") }
}
