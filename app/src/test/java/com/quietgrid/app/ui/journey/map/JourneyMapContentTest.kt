package com.quietgrid.app.ui.journey.map

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.journey.JourneyStopProgress
import com.quietgrid.app.ui.journey.JourneyStopRow
import com.quietgrid.app.ui.journey.journeyRows
import com.quietgrid.engine.journey.JourneyChapter
import com.quietgrid.engine.journey.JourneyFile
import com.quietgrid.engine.journey.JourneyStop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w390dp-h844dp-port")
class JourneyMapContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val tiers = List(10) { "easy" } + List(10) { "medium" } + List(10) { "hard" }
    private val file = JourneyFile(
        "arrowescape",
        listOf(JourneyChapter(1, 0, 999, tiers.mapIndexed { i, tier -> JourneyStop("s${i + 1}", tier, i, 120) })),
    )
    private val progress = (1..19).associate { "s$it" to JourneyStopProgress(stars = 2, cleared = true, bestSeconds = 100) }
    private val rows: List<JourneyStopRow> = journeyRows(file, progress)

    private var selected by mutableStateOf("s20")

    private fun show(reduceMotion: Boolean = true, initial: String = "s20") {
        selected = initial
        composeRule.setContent {
            MaterialTheme {
                JourneyMapContent(
                    gameId = GameId.ARROWESCAPE,
                    chapter = 1,
                    rows = rows,
                    selectedStopId = selected,
                    onSelect = { selected = it },
                    preview = null,
                    reduceMotion = reduceMotion,
                    onPlay = {},
                    onSkip = {},
                )
            }
        }
    }

    private fun stopNode(number: Int) = composeRule.onNode(
        SemanticsMatcher("stop $number") { node ->
            node.config.getOrNull(SemanticsProperties.ContentDescription)?.any { it.startsWith("Stop $number,") } == true
        },
    )

    private fun assertSelectedStopAndCardFit(number: Int) {
        val root = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        val viewport = composeRule.onNodeWithTag(JOURNEY_MAP_VIEWPORT_TAG).fetchSemanticsNode().boundsInRoot
        val stop = stopNode(number).fetchSemanticsNode().boundsInRoot
        val card = composeRule.onNodeWithTag(JOURNEY_STOP_CARD_TAG).fetchSemanticsNode().boundsInRoot
        val density = composeRule.density.density
        assertTrue("stop $stop height", stop.height >= 46 * density)
        assertTrue("stop $stop in $viewport", viewport.containsRect(stop))
        assertTrue("card $card in $root", root.containsRect(card))
        assertTrue("card $card overlaps stop $stop", !card.overlaps(stop))
    }

    private fun Rect.containsRect(other: Rect) =
        other.left >= left - 0.5f && other.top >= top - 0.5f && other.right <= right + 0.5f && other.bottom <= bottom + 0.5f

    @Test
    fun `every stop is a button with its description`() {
        show()
        val stopButtons = SemanticsMatcher("stop button") { node ->
            node.config.getOrNull(SemanticsProperties.Role) == Role.Button &&
                node.config.getOrNull(SemanticsProperties.ContentDescription)?.any { it.startsWith("Stop ") } == true
        }
        composeRule.onAllNodes(stopButtons).assertCountEquals(30)
        stopNode(14).assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Stop 14, Medium, Cleared, 2 of 3 stars")))
        stopNode(22).assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Stop 22, Hard, Open, 0 of 3 stars")))
    }

    @Test
    fun `selected stop and card both fit`() {
        show()
        assertSelectedStopAndCardFit(20)
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-port")
    fun `selected stop and card both fit on a small phone`() {
        show()
        assertSelectedStopAndCardFit(20)
    }

    @Test
    @Config(qualifiers = "w640dp-h360dp-land")
    fun `selected stop and card both fit on a small phone sideways`() {
        show()
        assertSelectedStopAndCardFit(20)
    }

    @Test
    @Config(qualifiers = "w844dp-h390dp-land")
    fun `selected stop and card both fit on a phone sideways`() {
        show()
        assertSelectedStopAndCardFit(20)
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-land")
    fun `selected stop and card both fit on a tablet`() {
        show()
        assertSelectedStopAndCardFit(20)
    }

    @Test
    @Config(qualifiers = "w800dp-h1280dp-port")
    fun `selected stop and card both fit on an upright tablet`() {
        show()
        assertSelectedStopAndCardFit(20)
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-port")
    fun `selecting a far stop scrolls it clear of the card`() {
        show()
        selected = "s1"
        composeRule.waitForIdle()
        assertSelectedStopAndCardFit(1)
    }

    @Test
    fun `tapping a stop selects it and changes the card`() {
        show()
        composeRule.onNodeWithText("Stop 20").assertExists()
        stopNode(19).performClick()
        composeRule.waitForIdle()
        assertEquals("s19", selected)
        composeRule.onNodeWithText("Stop 19").assertExists()
        composeRule.onNode(hasText("Replay") and hasClickAction()).assertExists()
    }

    @Test
    fun `later stop card has a disabled play button`() {
        show(initial = "s30")
        composeRule.onNodeWithText("Stop 30").assertExists()
        composeRule.onNode(hasText("Play") and hasClickAction()).assertIsNotEnabled()
        composeRule.onAllNodes(hasText("Skip") and hasClickAction()).assertCountEquals(0)
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-port")
    fun `the map opens at the selected stop without animating`() {
        composeRule.mainClock.autoAdvance = false
        show(reduceMotion = false, initial = "s20")
        composeRule.mainClock.advanceTimeByFrame()
        assertSelectedStopAndCardFit(20)
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-port")
    fun `reduce motion jumps to the current stop`() {
        composeRule.mainClock.autoAdvance = false
        show(reduceMotion = true, initial = "s20")
        composeRule.mainClock.advanceTimeByFrame()
        assertSelectedStopAndCardFit(20)
    }
}
