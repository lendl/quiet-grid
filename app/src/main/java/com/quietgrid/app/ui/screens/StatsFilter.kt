package com.quietgrid.app.ui.screens

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameCatalog
import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.GameStats
import com.quietgrid.app.data.PlayRecord

fun statsFilterGames(statsByGame: Map<GameId, GameStats>, records: List<PlayRecord>): List<GameId> {
    val playedByGame = GameCatalog.games.associate { meta ->
        meta.id to (statsByGame[meta.id]?.let { s -> Difficulty.entries.sumOf { s.forDifficulty(it).played } } ?: 0)
    }
    val statsGames = GameCatalog.games
        .filter { (playedByGame[it.id] ?: 0) > 0 }
        .sortedByDescending { playedByGame[it.id] ?: 0 }
        .map { it.id }
    val historyKeys = records.mapTo(HashSet()) { it.gameId }
    val historyOnly = GameCatalog.games
        .filter { it.id.key in historyKeys && it.id !in statsGames }
        .map { it.id }
    return statsGames + historyOnly
}
