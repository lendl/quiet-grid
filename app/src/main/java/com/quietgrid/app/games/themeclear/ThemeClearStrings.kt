package com.quietgrid.app.games.themeclear

import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.ui.components.QuickStartContent
import java.util.Locale

val THEMECLEAR_SUPPORTED_LOCALES = setOf("en", "nl", "de", "es", "fr", "pl", "pt")

fun currentThemeClearLocale(puzzleLanguageOverride: String): String {
    val candidate = puzzleLanguageOverride.ifEmpty { Locale.getDefault().language }
    return if (candidate in THEMECLEAR_SUPPORTED_LOCALES) candidate else "en"
}

fun themeClearDifficultyLabelRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.themeclear_difficulty_easy
    Difficulty.MEDIUM -> R.string.themeclear_difficulty_medium
    Difficulty.HARD -> R.string.themeclear_difficulty_hard
    Difficulty.EXPERT -> R.string.themeclear_difficulty_expert
}

fun themeClearDifficultyDescriptionRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.themeclear_difficulty_desc_easy
    Difficulty.MEDIUM -> R.string.themeclear_difficulty_desc_medium
    Difficulty.HARD -> R.string.themeclear_difficulty_desc_hard
    Difficulty.EXPERT -> R.string.themeclear_difficulty_desc_expert
}

val ThemeClearQuickStart = QuickStartContent(
    goalRes = R.string.themeclear_quickstart_goal,
    bulletRes = listOf(
        R.string.themeclear_quickstart_bullet_1,
        R.string.themeclear_quickstart_bullet_2,
        R.string.themeclear_quickstart_bullet_3,
    ),
    hookRes = R.string.themeclear_quickstart_hook,
)
