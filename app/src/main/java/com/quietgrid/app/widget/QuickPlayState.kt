package com.quietgrid.app.widget

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameCatalog
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.mix.Mix
import com.quietgrid.app.core.mix.resolvedCandidates
import com.quietgrid.app.data.ActiveSessionSummary
import com.quietgrid.app.data.PlayRecord
import com.quietgrid.app.nav.Routes

sealed interface QuickPlayState {
    data class Continue(val gameId: GameId, val difficulty: Difficulty?) : QuickPlayState
    data class PlayMix(val mixId: String, val name: String, val gameIds: List<GameId>) : QuickPlayState
    data class Games(val games: List<QuickPlayGame>) : QuickPlayState
}

data class QuickPlayGame(val gameId: GameId, val difficulty: Difficulty)

val QUICK_PLAY_DEFAULT_GAMES: List<GameId> = listOf(GameId.SUDOKU, GameId.WORDSEARCH, GameId.MINESWEEPER)

private const val QUICK_PLAY_GAME_COUNT = 3
private const val QUICK_PLAY_MIX_ICON_COUNT = 3

private val playCountThenRecency = compareByDescending<List<PlayRecord>> { it.size }
    .thenByDescending { records -> records.maxOf { it.timestampMillis } }

fun quickPlayState(
    activeSession: ActiveSessionSummary?,
    mixes: List<Mix>,
    lastStartedMixId: String?,
    history: List<PlayRecord>,
    betaGamesEnabled: Boolean,
    favoriteGameIds: Set<String> = emptySet(),
): QuickPlayState {
    val continueGame = activeSession
        ?.takeIf { Routes.resume(it) != null }
        ?.let { session -> GameId.entries.firstOrNull { it.key == session.gameId } }
    if (continueGame != null) {
        return QuickPlayState.Continue(continueGame, Difficulty.entries.firstOrNull { it.key == activeSession.difficulty })
    }
    val enabled = GameCatalog.enabledGameIds(betaGamesEnabled)
    val playableMix = (mixes.filter { it.id == lastStartedMixId } + mixes)
        .firstOrNull { mix -> mix.resolvedCandidates().any { it.gameId in enabled } }
    if (playableMix != null) return QuickPlayState.PlayMix(playableMix.id, playableMix.name, mixGames(playableMix, enabled))
    return QuickPlayState.Games(rankedGames(history, enabled, favoriteGameIds))
}

private fun mixGames(mix: Mix, enabled: Set<GameId>): List<GameId> =
    mix.resolvedCandidates()
        .filter { it.gameId in enabled }
        .groupBy { it.gameId }
        .entries
        .sortedByDescending { (_, candidates) -> candidates.sumOf { it.weight } }
        .map { it.key }
        .take(QUICK_PLAY_MIX_ICON_COUNT)

private fun rankedGames(history: List<PlayRecord>, enabled: Set<GameId>, favoriteGameIds: Set<String>): List<QuickPlayGame> {
    val ranked = history
        .asSequence()
        .filter { !it.isChallenger }
        .filter { record -> GameId.entries.firstOrNull { it.key == record.gameId } in enabled }
        .filter { record -> Difficulty.entries.any { it.key == record.difficulty } }
        .groupBy { it.gameId }
        .values
        .sortedWith(playCountThenRecency)
        .map { records ->
            val difficultyKey = records.groupBy { it.difficulty }.values.sortedWith(playCountThenRecency).first().first().difficulty
            QuickPlayGame(GameId.entries.first { it.key == records.first().gameId }, Difficulty.fromKey(difficultyKey))
        }
    val (rankedFavorites, rankedOthers) = ranked.partition { it.gameId.key in favoriteGameIds }
    val catalogFavorites = GameCatalog.games
        .map { it.id }
        .filter { it in enabled && it.key in favoriteGameIds }
        .map { QuickPlayGame(it, Difficulty.EASY) }
    val fill = QUICK_PLAY_DEFAULT_GAMES.map { QuickPlayGame(it, Difficulty.EASY) }
    return (rankedFavorites + catalogFavorites + rankedOthers + fill)
        .distinctBy { it.gameId }
        .take(QUICK_PLAY_GAME_COUNT)
}
