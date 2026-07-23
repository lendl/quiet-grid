package com.quietgrid.app.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.quietgrid.app.ui.theme.LocalIsPencilTheme

private val PencilDifficultyColors = mapOf(
    Difficulty.EASY to Color(0xFF4D4D4D),
    Difficulty.MEDIUM to Color(0xFF7A7A7A),
    Difficulty.HARD to Color(0xFF999999),
    Difficulty.EXPERT to Color(0xFFB3B3B3),
)

private val StandardDifficultyColors = mapOf(
    Difficulty.EASY to Color(0xFF4ADE80),
    Difficulty.MEDIUM to Color(0xFFFACC15),
    Difficulty.HARD to Color(0xFFFB923C),
    Difficulty.EXPERT to Color(0xFFF87171),
)

@Composable
fun difficultyColor(difficulty: Difficulty): Color {
    val isPencil = LocalIsPencilTheme.current
    return (if (isPencil) PencilDifficultyColors else StandardDifficultyColors).getValue(difficulty)
}
