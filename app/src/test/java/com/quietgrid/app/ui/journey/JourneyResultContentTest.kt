package com.quietgrid.app.ui.journey

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.journey.JourneySignals
import com.quietgrid.app.core.journey.JourneyStopProgress
import com.quietgrid.engine.journey.JourneyChapter
import com.quietgrid.engine.journey.JourneyFile
import com.quietgrid.engine.journey.JourneyStop
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w390dp-h844dp-port")
class JourneyResultContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val file = JourneyFile(
        "arrowescape",
        listOf(JourneyChapter(2, 100, 400, (1..5).map { JourneyStop("s$it", "medium", 100 + it, 90) })),
    )

    private fun model(stopId: String, progress: Map<String, JourneyStopProgress>, elapsed: Int, signals: JourneySignals) =
        journeyResultModel(journeyRows(file, progress), stopId, elapsed, signals)!!

    private fun show(model: JourneyResultModel, reduceMotion: Boolean) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            MaterialTheme {
                JourneyResultContent(GameId.ARROWESCAPE, model, reduceMotion, onNext = {}, onReplay = {}, onMap = {})
            }
        }
        composeRule.mainClock.advanceTimeByFrame()
    }

    private fun starScales(description: String): List<Float> =
        composeRule.onAllNodesWithContentDescription(description).fetchSemanticsNodes().map { it.config[StarRevealScale] }

    @Test
    fun `reduce motion shows earned and missed stars at once`() {
        val twoStars = model("s1", mapOf("s1" to JourneyStopProgress(stars = 2, cleared = true)), 200, JourneySignals(0, 0))
        show(twoStars, reduceMotion = true)
        assertEquals(listOf(1f, 1f), starScales("Star earned"))
        composeRule.onAllNodesWithContentDescription("Star not earned").assertCountEquals(1)
    }

    @Test
    fun `with motion earned stars start hidden`() {
        val twoStars = model("s1", mapOf("s1" to JourneyStopProgress(stars = 2, cleared = true)), 200, JourneySignals(0, 0))
        show(twoStars, reduceMotion = false)
        assertEquals(listOf(0f, 0f), starScales("Star earned"))
    }

    @Test
    fun `three-star stop hides replay`() {
        show(model("s1", mapOf("s1" to JourneyStopProgress(stars = 3, cleared = true)), 60, JourneySignals(0, 0)), reduceMotion = true)
        composeRule.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Text, listOf(androidx.compose.ui.text.AnnotatedString("Replay for 3 stars"))))
            .assertCountEquals(0)
        composeRule.onNodeWithText("Next stop").performScrollTo()
    }

    @Test
    fun `replay with fewer stars shows the best line`() {
        show(model("s1", mapOf("s1" to JourneyStopProgress(stars = 2, cleared = true)), 200, JourneySignals(1, 0)), reduceMotion = true)
        composeRule.onNodeWithText("Best: 2 of 3 stars").performScrollTo()
        composeRule.onNodeWithText("Replay for 3 stars").performScrollTo()
    }

    @Test
    fun `last stop hides next`() {
        val allCleared = file.chapters.single().stops.associate { it.puzzleId to JourneyStopProgress(stars = 1, cleared = true) }
        show(model("s5", allCleared, 200, JourneySignals(0, 0)), reduceMotion = true)
        composeRule.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Text, listOf(androidx.compose.ui.text.AnnotatedString("Next stop"))))
            .assertCountEquals(0)
        composeRule.onNodeWithText("Map").performScrollTo()
    }

    @Test
    fun `missed clean goal shows hint and mistake detail`() {
        show(model("s1", emptyMap(), 60, JourneySignals(hintsUsed = 1, mistakes = 2)), reduceMotion = true)
        composeRule.onNodeWithText("1 hint · 2 mistakes").performScrollTo()
        composeRule.onNodeWithText("Chapter 2 · Stop 1").performScrollTo()
        composeRule.onNodeWithText("Under 1:30").performScrollTo()
    }
}
