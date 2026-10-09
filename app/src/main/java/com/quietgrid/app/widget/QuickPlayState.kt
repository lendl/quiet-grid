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
    data object Continue : QuickPlayState
    data class PlayMix(val mixId: String) : QuickPlayState
    data class Games(val games: List<QuickPlayGame>) : QuickPlayState
}

data class QuickPlayGame(val gameId: GameId, val difficulty: Difficulty)

val QUICK_PLAY_DEFAULT_GAMES: List<GameId> = listOf(GameId.SUDOKU, GameId.WORDSEARCH, GameId.MINESWEEPER)

private const val QUICK_PLAY_GAME_COUNT = 3

private val playCountThenRecency = compareByDescending<List<PlayRecord>> { it.size }
    .thenByDescending { records -> records.maxOf { it.timestampMillis } }

fun quickPlayState(
    activeSession: ActiveSessionSummary?,
    mixes: List<Mix>,
    lastStartedMixId: String?,
    history: List<PlayRecord>,
    betaGamesEnabled: Boolean,
): QuickPlayState {
    if (activeSession?.let { Routes.resume(it) } != null) return QuickPlayState.Continue
    val enabled = GameCatalog.enabledGameIds(betaGamesEnabled)
    val playableMix = (mixes.filter { it.id == lastStartedMixId } + mixes)
        .firstOrNull { mix -> mix.resolvedCandidates().any { it.gameId in enabled } }
    if (playableMix != null) return QuickPlayState.PlayMix(playableMix.id)
    return QuickPlayState.Games(rankedGames(history, enabled))
}

private fun rankedGames(history: List<PlayRecord>, enabled: Set<GameId>): List<QuickPlayGame> {
    val ranked = history
        .asSequence()
        .filter { !it.isChallenger }
        .filter { record -> GameId.entries.firstOrNull { it.key == record.gameId } in enabled }
        .filter { record -> Difficulty.entries.any { it.key == record.difficulty } }
        .groupBy { it.gameId }
        .values
        .sortedWith(playCountThenRecency)
        .take(QUICK_PLAY_GAME_COUNT)
        .map { records ->
            val difficultyKey = records.groupBy { it.difficulty }.values.sortedWith(playCountThenRecency).first().first().difficulty
            QuickPlayGame(GameId.entries.first { it.key == records.first().gameId }, Difficulty.fromKey(difficultyKey))
        }
    val fill = QUICK_PLAY_DEFAULT_GAMES
        .filter { default -> ranked.none { it.gameId == default } }
        .map { QuickPlayGame(it, Difficulty.EASY) }
    return (ranked + fill).take(QUICK_PLAY_GAME_COUNT)
}
