package com.quietgrid.app.core.daily

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameCatalog
import com.quietgrid.app.core.GameId

val DAILY_GAMES: Set<GameId> = setOf(
    GameId.ANIMALDOKU,
    GameId.SUDOKU,
    GameId.TAKUZU,
    GameId.WORDSEARCH,
    GameId.WORDGUESS,
)

fun dailyEligibleGames(): List<GameId> =
    GameCatalog.games.filter { it.id in DAILY_GAMES && !it.beta }.map { it.id }

fun availableDailyTiers(poolSizes: Map<Difficulty, Int>): List<Difficulty> =
    Difficulty.entries.filter { (poolSizes[it] ?: 0) >= DAILY_MIN_POOL }
