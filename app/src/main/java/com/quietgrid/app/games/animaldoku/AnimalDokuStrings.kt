// app/src/main/java/com/quietgrid/app/games/animaldoku/AnimalDokuStrings.kt
package com.quietgrid.app.games.animaldoku

import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty

fun animalDokuDifficultyLabelRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.animaldoku_difficulty_easy
    Difficulty.MEDIUM -> R.string.animaldoku_difficulty_medium
    Difficulty.HARD -> R.string.animaldoku_difficulty_hard
    Difficulty.EXPERT -> R.string.animaldoku_difficulty_expert
}

fun animalDokuDifficultyDescriptionRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.animaldoku_difficulty_desc_easy
    Difficulty.MEDIUM -> R.string.animaldoku_difficulty_desc_medium
    Difficulty.HARD -> R.string.animaldoku_difficulty_desc_hard
    Difficulty.EXPERT -> R.string.animaldoku_difficulty_desc_expert
}
