package com.quietgrid.app.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.ui.theme.QuietGridTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp")
class StatsTableWordBreakTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `english words never break`() = assertNoWordBreaks()

    @Test
    @Config(qualifiers = "+de")
    fun `german words never break`() = assertNoWordBreaks()

    @Test
    @Config(qualifiers = "+es")
    fun `spanish words never break`() = assertNoWordBreaks()

    @Test
    @Config(qualifiers = "+fr")
    fun `french words never break`() = assertNoWordBreaks()

    @Test
    @Config(qualifiers = "+it")
    fun `italian words never break`() = assertNoWordBreaks()

    @Test
    @Config(qualifiers = "+nl")
    fun `dutch words never break`() = assertNoWordBreaks()

    @Test
    @Config(qualifiers = "+pl")
    fun `polish words never break`() = assertNoWordBreaks()

    @Test
    @Config(qualifiers = "+pt")
    fun `portuguese words never break`() = assertNoWordBreaks()

    @Test
    @Config(qualifiers = "+pt", fontScale = 2.0f)
    fun `portuguese words never break at double font size`() = assertNoWordBreaks()

    @Test
    @Config(fontScale = 2.0f)
    fun `english words never break at double font size`() = assertNoWordBreaks()

    private fun assertNoWordBreaks() {
        val rows = Difficulty.entries.map { difficulty ->
            StatsDifficultyRow(
                difficulty = difficulty,
                labelRes = when (difficulty) {
                    Difficulty.EASY -> R.string.sudoku_difficulty_easy
                    Difficulty.MEDIUM -> R.string.sudoku_difficulty_medium
                    Difficulty.HARD -> R.string.sudoku_difficulty_hard
                    Difficulty.EXPERT -> R.string.sudoku_difficulty_expert
                },
                played = 135,
                solved = 120,
                bestScore = 12345,
                winRate = 100,
            )
        }
        val overview = StatsOverviewModel(totalSolved = 480, totalPlayed = 540, streak = 12, winRate = 89, rows = rows)
        composeRule.setContent {
            QuietGridTheme {
                Box(Modifier.width(328.dp).verticalScroll(rememberScrollState())) {
                    StatsOverviewContent(overview)
                }
            }
        }

        val textNodes = composeRule.onAllNodes(
            hasAnyAncestor(hasTestTag(STATS_DIFFICULTY_TABLE_TAG)) and
                SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),
            useUnmergedTree = true,
        ).fetchSemanticsNodes()
        assertTrue(textNodes.size >= 3 + rows.size * 4)
        textNodes.forEach { node ->
            val results = mutableListOf<TextLayoutResult>()
            node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
            val layout = results.single()
            val text = layout.layoutInput.text.text
            assertFalse("'$text' has lines cut off", layout.didOverflowHeight)
            for (line in 0 until layout.lineCount - 1) {
                val end = layout.getLineEnd(line)
                assertTrue("'$text' breaks mid-word after '${text.substring(0, end)}'", text[end - 1].isWhitespace())
            }
        }
    }
}
