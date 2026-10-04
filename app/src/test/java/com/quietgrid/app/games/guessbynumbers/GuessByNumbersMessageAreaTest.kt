package com.quietgrid.app.games.guessbynumbers

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GuessByNumbersMessageAreaTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `invalid-word flash does not change the message area height`() {
        var invalidFlash by mutableStateOf(false)
        composeRule.setContent {
            GuessByNumbersMessageArea(
                revealWord = null,
                invalidFlash = invalidFlash,
                modifier = Modifier.testTag("area"),
            )
        }
        val heightWithoutFlash = composeRule.onNodeWithTag("area").fetchSemanticsNode().size.height

        invalidFlash = true
        composeRule.waitForIdle()
        val heightWithFlash = composeRule.onNodeWithTag("area").fetchSemanticsNode().size.height

        assertTrue(heightWithFlash > 0)
        assertEquals(heightWithFlash, heightWithoutFlash)
    }
}
