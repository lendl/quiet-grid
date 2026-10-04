package com.quietgrid.app.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class PinnedHeaderScrollLayoutTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    @Config(qualifiers = "w640dp-h360dp-land")
    fun `header button stays tappable when content overflows and scrolls`() {
        var menuClicked = false
        composeRule.setContent {
            PinnedHeaderScrollLayout(
                header = {
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Spacer(Modifier.weight(1f))
                        Box(
                            Modifier
                                .size(48.dp)
                                .testTag("menu")
                                .clickable { menuClicked = true },
                        )
                    }
                },
            ) {
                Spacer(Modifier.fillMaxWidth().height(2000.dp))
            }
        }

        composeRule.onNodeWithTag("menu").performClick()

        assertTrue(menuClicked)
    }
}
