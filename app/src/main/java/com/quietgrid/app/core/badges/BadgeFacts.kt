package com.quietgrid.app.core.badges

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import java.time.LocalDate

const val SEASON_HALLOWEEN = "halloween"

data class BadgeFacts(
    val hintsUsed: Int = 0,
    val mistakes: Int = 0,
    val guesses: Int? = null,
    val allMinesFlagged: Boolean = false,
    val hiddenWordSolved: Boolean = false,
    val maxLinesAtOnce: Int = 0,
    val boardEmptied: Boolean = false,
    val maxTile: Int = 0,
    val reached: Int = 0,
    val seasonal: String? = null,
)

data class FinishFacts(
    val gameId: GameId,
    val difficulty: Difficulty,
    val solved: Boolean,
    val elapsedSeconds: Int,
    val date: LocalDate,
    val startedDate: LocalDate?,
    val journeyStopId: String?,
    val mixId: String?,
    val facts: BadgeFacts,
)
