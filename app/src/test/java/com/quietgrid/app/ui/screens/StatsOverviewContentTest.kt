package com.quietgrid.app.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StatsOverviewContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `each difficulty row is announced with its column labels`() {
        val overview = StatsOverviewModel(
            totalSolved = 3,
            totalPlayed = 5,
            streak = 1,
            winRate = 60,
            rows = listOf(
                StatsDifficultyRow(
                    difficulty = Difficulty.EXPERT,
                    labelRes = R.string.sudoku_difficulty_expert,
                    played = 5,
                    solved = 3,
                    bestScore = 0,
                    winRate = 60,
                ),
            ),
        )
        composeRule.setContent { StatsOverviewContent(overview) }

        composeRule.onNodeWithContentDescription("Expert, Solved: 3/5, Win Rate: 60%, Best score: -").assertExists()
    }
}
