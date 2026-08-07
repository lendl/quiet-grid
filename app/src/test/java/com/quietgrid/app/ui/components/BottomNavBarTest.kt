package com.quietgrid.app.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.quietgrid.app.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BottomNavBarTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `tapping a tab reports it as selected`() {
        var selected = AppTab.GAMES
        composeRule.setContent {
            BottomNavBar(selectedTab = selected, onSelectTab = { selected = it })
        }

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.tab_settings)).performClick()

        assertEquals(AppTab.SETTINGS, selected)
    }

    @Test
    fun `every tab's label is shown regardless of which one is selected`() {
        composeRule.setContent {
            BottomNavBar(selectedTab = AppTab.STATS, onSelectTab = {})
        }

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.tab_games)).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.tab_stats)).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.tab_settings)).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.tab_support)).assertIsDisplayed()
    }

    @Test
    fun `tapping a tab invokes the callback with that tab`() {
        var callbackTab: AppTab? = null
        composeRule.setContent {
            BottomNavBar(selectedTab = AppTab.GAMES, onSelectTab = { callbackTab = it })
        }

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.tab_support)).performClick()

        assertEquals(AppTab.SUPPORT, callbackTab)
    }
}
