package com.quietgrid.app.nav

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import com.quietgrid.app.LaunchRequest
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LaunchRequestEffectTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `a request that suspends while being handled still finishes before it is cleared`() {
        var request by mutableStateOf<LaunchRequest?>(null)
        val storageWrite = CompletableDeferred<Unit>()
        val finished = mutableListOf<LaunchRequest>()
        composeRule.setContent {
            LaunchRequestEffect(request, onHandled = { request = null }) { started ->
                storageWrite.await()
                finished += started
            }
        }

        val startGame = LaunchRequest.StartGame(GameId.SUDOKU, Difficulty.EASY)
        composeRule.runOnIdle { request = startGame }
        composeRule.waitForIdle()
        storageWrite.complete(Unit)
        composeRule.waitForIdle()

        assertEquals(listOf(startGame), finished)
        assertNull(request)
    }
}
