package com.quietgrid.app.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.quietgrid.app.R
import com.quietgrid.app.core.mix.Mix
import com.quietgrid.app.core.mix.MixEntry
import com.quietgrid.app.core.mix.MixEntryMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MixesScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val sampleMix = Mix(
        id = "mix-1",
        name = "Evenings",
        entries = listOf(MixEntry(gameId = "sudoku", mode = MixEntryMode.PUZZLE, difficulty = "hard", weight = 2)),
    )

    @Test
    fun `empty mixes list renders the empty-state text`() {
        composeRule.setContent {
            MixesScreen(mixes = emptyList(), onPlay = {}, onEdit = {}, onNewMix = {}, onOpenAccount = {})
        }

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.mix_empty_state)).assertIsDisplayed()
    }

    @Test
    fun `tapping the new-mix button invokes onNewMix`() {
        var newMixTapped = false
        composeRule.setContent {
            MixesScreen(mixes = emptyList(), onPlay = {}, onEdit = {}, onNewMix = { newMixTapped = true }, onOpenAccount = {})
        }

        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.mix_new_content_description)).performClick()

        assertEquals(true, newMixTapped)
    }

    @Test
    fun `tapping a mix row invokes onEdit with that mix's id`() {
        var editedId: String? = null
        composeRule.setContent {
            MixesScreen(mixes = listOf(sampleMix), onPlay = {}, onEdit = { id -> editedId = id }, onNewMix = {}, onOpenAccount = {})
        }

        composeRule.onNodeWithText(sampleMix.name).performClick()

        assertEquals(sampleMix.id, editedId)
    }
}
