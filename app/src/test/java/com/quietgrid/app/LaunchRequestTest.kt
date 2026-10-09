package com.quietgrid.app

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LaunchRequestTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `every request round-trips through its intent`() {
        listOf(
            LaunchRequest.DailyTab,
            LaunchRequest.Resume,
            LaunchRequest.StartMix("mix-1"),
            LaunchRequest.StartGame(GameId.SUDOKU, Difficulty.HARD),
        ).forEach { assertEquals(it, it.toIntent(context).toLaunchRequest()) }
    }

    @Test
    fun `legacy daily notification intent parses to DailyTab`() {
        assertEquals(LaunchRequest.DailyTab, Intent().putExtra("open_tab", "daily").toLaunchRequest())
    }

    @Test
    fun `intents target MainActivity and reuse the existing task`() {
        val intent = LaunchRequest.Resume.toIntent(context)
        assertEquals(MainActivity::class.java.name, intent.component?.className)
        val expected = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        assertEquals(expected, intent.flags and expected)
    }

    @Test
    fun `start game intents for different games are not filter-equal`() {
        val sudoku = LaunchRequest.StartGame(GameId.SUDOKU, Difficulty.EASY).toIntent(context)
        val takuzu = LaunchRequest.StartGame(GameId.TAKUZU, Difficulty.EASY).toIntent(context)
        assertFalse(sudoku.filterEquals(takuzu))
    }

    @Test
    fun `unknown or incomplete intents parse to null`() {
        assertNull((null as Intent?).toLaunchRequest())
        assertNull(Intent(Intent.ACTION_MAIN).toLaunchRequest())
        assertNull(
            Intent().putExtra("launch_action", "game").putExtra("launch_game_id", "flowfree")
                .putExtra("launch_difficulty", "easy").toLaunchRequest(),
        )
        assertNull(
            Intent().putExtra("launch_action", "game").putExtra("launch_game_id", "sudoku")
                .putExtra("launch_difficulty", "endless").toLaunchRequest(),
        )
        assertNull(Intent().putExtra("launch_action", "mix").toLaunchRequest())
    }
}
