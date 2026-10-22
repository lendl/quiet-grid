package com.quietgrid.app.ui.play

import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onRoot
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R
import com.quietgrid.app.core.GameId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
class GamePlayScaffoldTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    @Config(qualifiers = "w360dp-h640dp-port")
    fun `small phone upright keeps the board still and uncovered`() = assertHintLeavesBoardAlone(hintBesideBoard = false)

    @Test
    @Config(qualifiers = "w390dp-h844dp-port")
    fun `normal phone upright keeps the board still and uncovered`() = assertHintLeavesBoardAlone(hintBesideBoard = false)

    @Test
    @Config(qualifiers = "w640dp-h360dp-land")
    fun `small phone sideways keeps the board still and uncovered`() = assertHintLeavesBoardAlone(hintBesideBoard = true)

    @Test
    @Config(qualifiers = "w844dp-h390dp-land")
    fun `normal phone sideways keeps the board still and uncovered`() = assertHintLeavesBoardAlone(hintBesideBoard = true)

    @Test
    @Config(qualifiers = "w1280dp-h800dp-land")
    fun `tablet landscape keeps the board still and uncovered`() = assertHintLeavesBoardAlone(hintBesideBoard = true)

    @Test
    @Config(qualifiers = "w800dp-h1280dp-port")
    fun `tablet upright keeps the board still and uncovered`() = assertHintLeavesBoardAlone(hintBesideBoard = false)

    @Test
    @Config(qualifiers = "w390dp-h844dp-port")
    fun `rules button opens the picture rules and a tap closes them`() {
        composeRule.setContent {
            MaterialTheme {
                GamePlayScaffold(gameId = GameId.TAKUZU, timer = null, onBack = {}, onEndPuzzle = {}, board = { Box(Modifier.fillMaxSize()) })
            }
        }
        val activity = composeRule.activity

        composeRule.onNodeWithContentDescription(activity.getString(R.string.common_rules)).performClick()

        composeRule.onNodeWithTag(PLAY_RULES_TAG).assertIsDisplayed()
        listOf(R.string.takuzu_rule_1_title, R.string.takuzu_rule_2_title, R.string.takuzu_rule_3_title)
            .forEach { composeRule.onNodeWithText(activity.getString(it)).assertIsDisplayed() }

        composeRule.onNodeWithTag(PLAY_RULES_TAG).performClick()

        composeRule.onNodeWithTag(PLAY_RULES_TAG).assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-port")
    fun `full rules opens the game's rules page`() {
        var openedRulesFor: GameId? = null
        composeRule.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalOpenGameRules provides { gameId -> openedRulesFor = gameId }) {
                    GamePlayScaffold(gameId = GameId.TAKUZU, timer = null, onBack = {}, onEndPuzzle = {}, board = { Box(Modifier.fillMaxSize()) })
                }
            }
        }
        val activity = composeRule.activity

        composeRule.onNodeWithContentDescription(activity.getString(R.string.common_rules)).performClick()
        composeRule.onNodeWithText(activity.getString(R.string.play_full_rules)).performClick()

        assertEquals(GameId.TAKUZU, openedRulesFor)
        composeRule.onNodeWithTag(PLAY_RULES_TAG).assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-port")
    fun `hint and rules never show at the same time`() {
        var hintActive by mutableStateOf(true)
        composeRule.setContent {
            MaterialTheme {
                GamePlayScaffold(
                    gameId = GameId.TAKUZU,
                    timer = null,
                    onBack = {},
                    onEndPuzzle = {},
                    hint = PlayHintAction(active = hintActive, busy = false, contentDescription = "hint", onClick = { hintActive = !hintActive }),
                    hintMessage = PlayHintMessage("No three in a row", "Place a 0 in row 3."),
                    board = { Box(Modifier.fillMaxSize()) },
                )
            }
        }
        val activity = composeRule.activity
        composeRule.onNodeWithTag(PLAY_HINT_TAG).assertIsDisplayed()

        composeRule.onNodeWithContentDescription(activity.getString(R.string.common_rules)).performClick()
        composeRule.onNodeWithTag(PLAY_RULES_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(PLAY_HINT_TAG).assertDoesNotExist()

        composeRule.onNodeWithContentDescription("hint").performClick()
        composeRule.onNodeWithTag(PLAY_RULES_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(PLAY_HINT_TAG).assertIsDisplayed()
        assertTrue(hintActive)
    }

    @Test
    @Config(qualifiers = "w844dp-h390dp-land")
    fun `sideways game without controls stacks the strip and the hint beside the board`() {
        composeRule.setContent {
            MaterialTheme {
                GamePlayScaffold(
                    gameId = GameId.MINESWEEPER,
                    timer = null,
                    onBack = {},
                    onEndPuzzle = {},
                    hint = PlayHintAction(active = true, busy = false, contentDescription = "hint", onClick = {}),
                    hintMessage = PlayHintMessage("Guaranteed safe tile", "Reveal the highlighted tile."),
                    info = listOf(InfoItem.Label("12 mines left")),
                    board = { Box(Modifier.fillMaxSize()) },
                )
            }
        }
        val board = composeRule.onNodeWithTag(PLAY_BOARD_TAG).getBoundsInRoot()
        val strip = composeRule.onNodeWithText("12 mines left").getBoundsInRoot()
        val hint = composeRule.onNodeWithTag(PLAY_HINT_TAG).getBoundsInRoot()
        assertTrue("strip $strip not beside board $board", strip.left >= board.right)
        assertTrue("hint $hint not under strip $strip", hint.top >= strip.bottom)
        assertEquals(strip.left.value, hint.left.value, 1f)
    }

    private fun assertBoardRatio(expected: Float, tolerance: Float) {
        composeRule.setContent {
            MaterialTheme {
                GamePlayScaffold(
                    gameId = GameId.MINESWEEPER,
                    timer = null,
                    onBack = {},
                    onEndPuzzle = {},
                    boardAspectRatioFor = { if (it.isUpright) 0.75f else 1.6f },
                    board = { Box(Modifier.fillMaxSize()) },
                )
            }
        }
        val board = composeRule.onNodeWithTag(PLAY_BOARD_TAG).getBoundsInRoot()
        assertEquals(expected, (board.right - board.left).value / (board.bottom - board.top).value, tolerance)
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-port")
    fun `ratio per size class shapes an upright board`() = assertBoardRatio(0.75f, 0.01f)

    @Test
    @Config(qualifiers = "w844dp-h390dp-land")
    fun `ratio per size class shapes a sideways board`() = assertBoardRatio(1.6f, 0.02f)

    @Test
    @Config(qualifiers = "w390dp-h844dp-port")
    fun `board without a dock sits in the middle of the page`() {
        composeRule.setContent {
            MaterialTheme {
                GamePlayScaffold(gameId = GameId.TAKUZU, timer = null, onBack = {}, onEndPuzzle = {}, board = { Box(Modifier.fillMaxSize()) })
            }
        }
        val root = composeRule.onRoot().getBoundsInRoot()
        val board = composeRule.onNodeWithTag(PLAY_BOARD_TAG).getBoundsInRoot()
        val pageMiddle = (root.top + root.bottom) / 2
        val boardMiddle = (board.top + board.bottom) / 2
        assertEquals(pageMiddle.value, boardMiddle.value, 1f)
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-port")
    fun `closing the hint card keeps the hint and the hint button brings the card back`() {
        var hintActive by mutableStateOf(true)
        var toggles = 0
        composeRule.setContent {
            MaterialTheme {
                GamePlayScaffold(
                    gameId = GameId.TAKUZU,
                    timer = null,
                    onBack = {},
                    onEndPuzzle = {},
                    hint = PlayHintAction(active = hintActive, busy = false, contentDescription = "hint", onClick = {
                        toggles++
                        hintActive = !hintActive
                    }),
                    hintMessage = PlayHintMessage("No three in a row", "Place a 0 in row 3."),
                    board = { Box(Modifier.fillMaxSize()) },
                )
            }
        }

        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.common_close)).performClick()
        composeRule.onNodeWithTag(PLAY_HINT_TAG).assertDoesNotExist()
        assertEquals(0, toggles)

        composeRule.onNodeWithContentDescription("hint").performClick()
        composeRule.onNodeWithTag(PLAY_HINT_TAG).assertIsDisplayed()
        assertEquals(0, toggles)

        composeRule.onNodeWithContentDescription("hint").performClick()
        composeRule.onNodeWithTag(PLAY_HINT_TAG).assertDoesNotExist()
        assertEquals(1, toggles)
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-port")
    fun `tight small phone shows only the hint title and expands it on tap`() {
        val title = "Hidden single: 5"
        val body = "Row 3 has only one cell left where a 5 can go, so the 5 must go in r3c7."
        composeRule.setContent {
            MaterialTheme {
                GamePlayScaffold(
                    gameId = GameId.SUDOKU,
                    timer = null,
                    onBack = {},
                    onEndPuzzle = {},
                    hint = PlayHintAction(active = true, busy = false, contentDescription = "hint", onClick = {}),
                    hintMessage = PlayHintMessage(title, body),
                    dockHeight = { 220.dp },
                    dock = { Box(Modifier.fillMaxSize()) },
                    board = { Box(Modifier.fillMaxSize()) },
                )
            }
        }
        val collapsed = composeRule.onNodeWithTag(PLAY_HINT_TAG).getBoundsInRoot()
        val titleBounds = composeRule.onNodeWithText(title, useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue("title $titleBounds is cut by the card $collapsed", titleBounds.bottom <= collapsed.bottom + 1.dp)
        assertTrue("collapsed hint still shows the body", composeRule.onAllNodesWithText(body, useUnmergedTree = true).fetchSemanticsNodes().isEmpty())

        composeRule.onNodeWithTag(PLAY_HINT_TAG).performClick()

        val expanded = composeRule.onNodeWithTag(PLAY_HINT_TAG).getBoundsInRoot()
        assertTrue("hint did not expand: $collapsed -> $expanded", expanded.bottom > collapsed.bottom)
        composeRule.onNodeWithText(body, useUnmergedTree = true).assertExists()
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-port")
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `tight hint keeps the whole title and cuts the description instead`() {
        val title = "Naked pair in row 3 removes 4 and 7 from every other cell in that row"
        composeRule.setContent {
            MaterialTheme {
                GamePlayScaffold(
                    gameId = GameId.SUDOKU,
                    timer = null,
                    onBack = {},
                    onEndPuzzle = {},
                    hint = PlayHintAction(active = true, busy = false, contentDescription = "hint", onClick = {}),
                    hintMessage = PlayHintMessage(title, "Cells r3c2 and r3c8 can only hold 4 or 7, so no other cell in row 3 can hold either digit."),
                    dockHeight = { 220.dp },
                    dock = { Box(Modifier.fillMaxSize()) },
                    board = { Box(Modifier.fillMaxSize()) },
                )
            }
        }
        val card = composeRule.onNodeWithTag(PLAY_HINT_TAG).getBoundsInRoot()
        val titleNode = composeRule.onNodeWithText(title, useUnmergedTree = true)
        val titleBounds = titleNode.getUnclippedBoundsInRoot()
        assertTrue("title $titleBounds is cut by the card $card", titleBounds.bottom <= card.bottom + 1.dp)
        val layouts = mutableListOf<TextLayoutResult>()
        titleNode.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        val titleLayout = layouts.single()
        assertTrue("title did not wrap", titleLayout.lineCount >= 2)
        assertFalse("title was ellipsized", titleLayout.isLineEllipsized(titleLayout.lineCount - 1))
    }

    @Test
    @Config(qualifiers = "sw390dp-w390dp-h844dp-port")
    fun `portrait-only game locks a phone to portrait and restores it on leave`() {
        var showGame by mutableStateOf(true)
        composeRule.setContent {
            MaterialTheme {
                if (showGame) {
                    GamePlayScaffold(
                        gameId = GameId.WORDGUESS,
                        timer = null,
                        onBack = {},
                        onEndPuzzle = {},
                        phonePortraitOnly = true,
                        board = { Box(Modifier.fillMaxSize()) },
                    )
                }
            }
        }
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT, composeRule.activity.requestedOrientation)

        showGame = false
        composeRule.waitForIdle()

        assertEquals(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, composeRule.activity.requestedOrientation)
    }

    @Test
    @Config(qualifiers = "sw800dp-w1280dp-h800dp-land")
    fun `portrait-only game leaves tablets free to rotate`() {
        composeRule.setContent {
            MaterialTheme {
                GamePlayScaffold(
                    gameId = GameId.WORDGUESS,
                    timer = null,
                    onBack = {},
                    onEndPuzzle = {},
                    phonePortraitOnly = true,
                    board = { Box(Modifier.fillMaxSize()) },
                )
            }
        }
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, composeRule.activity.requestedOrientation)
    }

    private fun assertHintLeavesBoardAlone(hintBesideBoard: Boolean) {
        var hintActive by mutableStateOf(false)
        composeRule.setContent {
            MaterialTheme {
                GamePlayScaffold(
                    gameId = GameId.TAKUZU,
                    timer = null,
                    onBack = {},
                    onEndPuzzle = {},
                    hint = PlayHintAction(active = hintActive, busy = false, contentDescription = "hint", onClick = { hintActive = !hintActive }),
                    hintMessage = PlayHintMessage("No three in a row", "Place a 0 in row 3 because two 1s already sit next to each other, so the cell beside the pair must be the other digit."),
                    dockHeight = { 120.dp },
                    info = listOf(InfoItem.Label("Tier: Easy"), InfoItem.Label("Solved: 2")),
                    dock = { Box(Modifier.fillMaxWidth().height(96.dp)) },
                    board = { Box(Modifier.fillMaxSize()) },
                )
            }
        }

        val boardBefore = composeRule.onNodeWithTag(PLAY_BOARD_TAG).getBoundsInRoot()
        hintActive = true
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(PLAY_HINT_TAG).assertIsDisplayed()
        val boardAfter = composeRule.onNodeWithTag(PLAY_BOARD_TAG).getBoundsInRoot()
        val hint = composeRule.onNodeWithTag(PLAY_HINT_TAG).getBoundsInRoot()
        assertEquals(boardBefore, boardAfter)
        assertFalse("hint $hint overlaps board $boardAfter", overlaps(boardAfter, hint))
        if (hintBesideBoard) {
            assertTrue("hint $hint not beside board $boardAfter", hint.left >= boardAfter.right)
        } else {
            assertTrue("hint $hint not above board $boardAfter", hint.bottom <= boardAfter.top)
        }
    }

    private fun overlaps(a: DpRect, b: DpRect): Boolean =
        a.left < b.right && b.left < a.right && a.top < b.bottom && b.top < a.bottom
}
