package com.quietgrid.app.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class CompactResultActionsTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    @Config(qualifiers = "w844dp-h390dp-land")
    fun `primary button sits between the links with the last link after it`() {
        composeRule.setContent {
            MaterialTheme {
                CompactResultActions(dailyResultLinks(hasNextDaily = true, {}, {}, {})) {
                    Text("Primary")
                }
            }
        }
        val share = composeRule.onNodeWithText(text(R.string.daily_share)).getBoundsInRoot()
        val backToDaily = composeRule.onNodeWithText(text(R.string.daily_back_to_daily)).getBoundsInRoot()
        val primary = composeRule.onNodeWithText("Primary").getBoundsInRoot()
        val games = composeRule.onNodeWithText(text(R.string.daily_back_to_games)).getBoundsInRoot()
        assertTrue(share.right <= backToDaily.left)
        assertTrue(backToDaily.right <= primary.left)
        assertTrue(primary.right <= games.left)
        assertEquals(primary.top.value, games.top.value, 24f)
    }

    @Test
    @Config(qualifiers = "w844dp-h390dp-land")
    fun `a single link goes after the primary button`() {
        composeRule.setContent {
            MaterialTheme {
                CompactResultActions(listOf(ResultLink(R.string.completion_try_another_game) {})) {
                    Text("Primary")
                }
            }
        }
        val primary = composeRule.onNodeWithText("Primary").getBoundsInRoot()
        val link = composeRule.onNodeWithText(text(R.string.completion_try_another_game)).getBoundsInRoot()
        assertTrue(primary.right <= link.left)
        assertFalse(link.right - link.left <= 0.dp)
    }

    @Test
    fun `sideways phones get the compact result layout and tablets keep the upright one`() {
        assertTrue(isSidewaysPhoneResult(844.dp, 390.dp))
        assertTrue(isSidewaysPhoneResult(640.dp, 360.dp))
        assertFalse(isSidewaysPhoneResult(390.dp, 844.dp))
        assertFalse(isSidewaysPhoneResult(1280.dp, 800.dp))
        assertFalse(isSidewaysPhoneResult(800.dp, 1280.dp))
    }

    private fun text(resId: Int): String = composeRule.activity.getString(resId)
}
