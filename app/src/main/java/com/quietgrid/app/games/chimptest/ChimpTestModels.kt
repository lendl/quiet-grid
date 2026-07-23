package com.quietgrid.app.games.chimptest

import kotlinx.serialization.Serializable

enum class ChimpTestStatus { PLAYING, WON }

@Serializable
data class ChimpTestPuzzle(
    val id: String,
    val difficulty: String,
    val gridSize: Int,
    val startCount: Int,
    val maxCount: Int,
)

@Serializable
data class ChimpTestCell(
    val number: Int,
    val row: Int,
    val col: Int,
    val hidden: Boolean,
)

@Serializable
data class ChimpTestSession(
    val puzzle: ChimpTestPuzzle,
    val currentCount: Int,
    val cells: List<ChimpTestCell>,
    val nextExpected: Int,
    val revealAll: Boolean,
    val wrongTapCell: Int?,
    val roundTimes: List<Double>,
    val roundStartElapsed: Double,
    val status: ChimpTestStatus,
)

sealed class ChimpTestEffect {
    data object WrongTap : ChimpTestEffect()
}

data class ChimpTestActionResult(
    val changed: Boolean,
    val session: ChimpTestSession,
    val effects: List<ChimpTestEffect>,
)
