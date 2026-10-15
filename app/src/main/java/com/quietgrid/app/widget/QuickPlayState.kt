package com.quietgrid.app.widget

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameCatalog
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.mix.Mix
import com.quietgrid.app.core.mix.resolvedCandidates
import com.quietgrid.app.data.ActiveSessionSummary
import com.quietgrid.app.data.PlayRecord
import com.quietgrid.app.nav.Routes

data class QuickPlayContinue(val gameId: GameId, val difficulty: Difficulty?)

data class QuickPlayGame(val gameId: GameId, val difficulty: Difficulty)

data class QuickPlayMix(val mixId: String, val name: String, val gameIds: List<GameId>)

data class QuickPlayState(val continueGame: QuickPlayContinue?, val games: List<QuickPlayGame>)

data class MixWidgetState(val continueGame: QuickPlayContinue?, val mix: QuickPlayMix?)

val QUICK_PLAY_DEFAULT_GAMES: List<GameId> = listOf(GameId.SUDOKU, GameId.WORDSEARCH, GameId.MINESWEEPER)

private const val QUICK_PLAY_GAME_COUNT = 3
private const val QUICK_PLAY_MIX_ICON_COUNT = 3

private val playCountThenRecency = compareByDescending<List<PlayRecord>> { it.size }
    .thenByDescending { records -> records.maxOf { it.timestampMillis } }

fun quickPlayContinue(activeSession: ActiveSessionSummary?): QuickPlayContinue? {
    val session = activeSession?.takeIf { Routes.resume(it) != null } ?: return null
    val gameId = GameId.entries.firstOrNull { it.key == session.gameId } ?: return null
    return QuickPlayContinue(gameId, Difficulty.entries.firstOrNull { it.key == session.difficulty })
}

fun quickPlayState(
    activeSession: ActiveSessionSummary?,
    history: List<PlayRecord>,
    betaGamesEnabled: Boolean,
    favoriteGameIds: Set<String> = emptySet(),
): QuickPlayState = QuickPlayState(
    continueGame = quickPlayContinue(activeSession),
    games = rankedGames(history, GameCatalog.enabledGameIds(betaGamesEnabled), favoriteGameIds),
)

fun mixWidgetState(
    activeSession: ActiveSessionSummary?,
    mixes: List<Mix>,
    lastStartedMixId: String?,
    betaGamesEnabled: Boolean,
): MixWidgetState {
    val enabled = GameCatalog.enabledGameIds(betaGamesEnabled)
    val playableMix = (mixes.filter { it.id == lastStartedMixId } + mixes)
        .firstOrNull { mix -> mix.resolvedCandidates().any { it.gameId in enabled } }
    return MixWidgetState(
        continueGame = quickPlayContinue(activeSession),
        mix = playableMix?.let { QuickPlayMix(it.id, it.name, mixGames(it, enabled)) },
    )
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
