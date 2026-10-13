package com.quietgrid.app.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import com.quietgrid.app.R
import com.quietgrid.app.core.GameId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w240dp-h480dp")
class GamePageTopBarTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val favoriteLabel get() = composeRule.activity.getString(R.string.games_favorite)

    private fun setTopBar(isFavorite: Boolean, fontScale: Float = 1f, onFavoriteChange: (Boolean) -> Unit = {}) {
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                GamePageTopBar(GameId.GUESSBYNUMBERS, isFavorite = isFavorite, onFavoriteChange = onFavoriteChange, onBack = {})
            }
        }
    }

    @Test
    fun `tapping the empty star marks the game as favorite`() {
        var changedTo: Boolean? = null
        setTopBar(isFavorite = false, onFavoriteChange = { changedTo = it })

        composeRule.onNodeWithContentDescription(favoriteLabel).assertIsOff().performClick()

        assertEquals(true, changedTo)
    }

    @Test
    fun `tapping the filled star removes the favorite`() {
        var changedTo: Boolean? = null
        setTopBar(isFavorite = true, onFavoriteChange = { changedTo = it })

        composeRule.onNodeWithContentDescription(favoriteLabel).assertIsOn().performClick()

        assertEquals(false, changedTo)
    }

    @Test
    fun `star stays fully on screen and tappable on a narrow screen with large text`() {
        var changedTo: Boolean? = null
        setTopBar(isFavorite = false, fontScale = 2f, onFavoriteChange = { changedTo = it })

        val rootWidth = composeRule.onRoot().getUnclippedBoundsInRoot().width
        val star = composeRule.onNodeWithContentDescription(favoriteLabel).assertIsDisplayed()
        val bounds = star.getUnclippedBoundsInRoot()

        assertTrue("star right edge ${bounds.right} beyond screen width $rootWidth", bounds.right <= rootWidth)
        assertTrue("star top ${bounds.top} below the top bar's first row", bounds.top <= 16.dp)
        star.performClick()
        assertEquals(true, changedTo)
    }
}
