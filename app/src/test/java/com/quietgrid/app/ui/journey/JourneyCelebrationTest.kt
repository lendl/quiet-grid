package com.quietgrid.app.ui.journey

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.journey.JourneyMilestone
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
class JourneyCelebrationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val file = JourneyFile(
        "animaldoku",
        listOf(
            JourneyChapter(1, 100, 400, listOf("a1", "a2", "a3").map { JourneyStop(it, "easy", 150, 90) }),
            JourneyChapter(2, 100, 400, listOf("b1", "b2").map { JourneyStop(it, "easy", 150, 90) }),
        ),
    )

    private val played = mutableListOf<String>()
    private val maps = mutableListOf<Int>()
    private var done = 0
    private var closed = 0

    private fun won(stars: Int, clean: Boolean = false) = JourneyStopProgress(stars = stars, cleared = true, clean = clean, bestSeconds = 60)

    private fun show(milestone: JourneyMilestone, progress: Map<String, JourneyStopProgress>, playSeconds: Int = 180) {
        val actions = JourneyCelebrationActions(
            onClose = { closed++ },
            onPlay = { played += it.stop.puzzleId },
            onMap = { maps += it },
            onDone = { done++ },
        )
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            MaterialTheme {
                JourneyCelebration(GameId.ANIMALDOKU, milestone, journeyRows(file, progress), playSeconds, reduceMotion = true, actions = actions)
            }
        }
        composeRule.mainClock.advanceTimeByFrame()
    }

    @Test
    fun `chapter complete shows the chapter map, stats and the next chapter`() {
        show(JourneyMilestone.ChapterComplete(1), mapOf("a1" to won(3, clean = true), "a2" to won(2), "a3" to won(2)))

        composeRule.onNodeWithContentDescription("Chapter 1, 3 of 3 stops cleared").assertExists()
        composeRule.onNodeWithText("Chapter 1 complete").performScrollTo()
        composeRule.onNodeWithText("All 3 stops cleared").performScrollTo()
        composeRule.onNodeWithContentDescription("7/9 stars").performScrollTo()
        composeRule.onNodeWithContentDescription("1 clean boards").performScrollTo()
        composeRule.onNodeWithText("Start chapter 2").performScrollTo().performClick()
        composeRule.onNodeWithText("Replay for stars").performScrollTo().performClick()
        composeRule.onNodeWithText("Map").performScrollTo().performClick()

        assertEquals(listOf("b1", "a2"), played)
        assertEquals(listOf(1), maps)
    }

    @Test
    fun `journey complete counts stars still to go and offers a replay`() {
        val progress = mapOf("a1" to won(3), "a2" to won(3), "a3" to won(2), "b1" to won(2), "b2" to won(2))
        show(JourneyMilestone.JourneyComplete, progress, playSeconds = 180)

        composeRule.onNodeWithContentDescription("Journey map, 5 of 5 stops cleared").assertExists()
        composeRule.onNodeWithText("Journey complete").performScrollTo()
        composeRule.onNodeWithText("All 5 stops cleared · 3 stars to go for gold").performScrollTo()
        composeRule.onNodeWithContentDescription("12/15 stars").performScrollTo()
        composeRule.onNodeWithContentDescription("3:00 total time").performScrollTo()
        composeRule.onNodeWithText("Replay for stars").performScrollTo().performClick()
        composeRule.onNodeWithText("Done").performScrollTo().performClick()

        assertEquals(listOf("a3"), played)
        assertEquals(1, done)
    }

    @Test
    fun `every star collected goes gold and opens the last chapter map`() {
        val progress = listOf("a1", "a2", "a3", "b1", "b2").associateWith { won(3) }
        show(JourneyMilestone.Mastered, progress)

        composeRule.onNodeWithText("Every star collected").performScrollTo()
        composeRule.onNodeWithText("All 5 stops at three stars").performScrollTo()
        composeRule.onNodeWithContentDescription("15 stars").performScrollTo()
        composeRule.onNodeWithText("View the map").performScrollTo().performClick()
        composeRule.onNodeWithContentDescription("Close").performClick()

        assertEquals(listOf(2), maps)
        assertEquals(1, closed)
    }
}
