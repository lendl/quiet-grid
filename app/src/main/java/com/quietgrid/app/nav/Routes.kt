package com.quietgrid.app.nav

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import java.time.LocalDate

object Routes {
    const val TABS = "tabs"
    const val PICKER = "picker/{gameId}"
    const val PLAY = "play/{gameId}/{difficulty}/{resume}?daily={daily}"
    const val CHALLENGER = "challenger/{gameId}"
    const val CHALLENGER_RESULT = "challengerResult/{gameId}/{puzzlesSolved}/{tier}/{score}/{isNewHighScore}/{reason}/{previousBest}/{fastestSolveSeconds}"
    const val COMPLETION = "completion/{gameId}/{difficulty}/{score}/{accuracyPct}/{elapsedSeconds}/{isFirstSolve}/{isNewHighScore}/{bestTile}?daily={daily}"
    const val LOSS = "loss/{gameId}/{difficulty}/{elapsedSeconds}/{reason}/{score}/{bestTile}?daily={daily}"
    const val ANALYZER = "analyzer/{gameId}"
    const val SUPPORT_INFO = "supportInfo/{key}"
    const val SETTINGS = "settings"
    const val SUPPORT = "support"
    const val TRUST = "trust"
    const val ABOUT = "about"
    const val MIX_EDITOR = "mixEditor/{mixId}"

    fun picker(gameId: GameId) = "picker/${gameId.key}"
    fun play(gameId: GameId, difficulty: Difficulty, resume: Boolean, daily: LocalDate? = null) =
        "play/${gameId.key}/${difficulty.key}/$resume" + (daily?.let { "?daily=$it" } ?: "")

    fun challenger(gameId: GameId) = "challenger/${gameId.key}"

    fun challengerResult(
        gameId: GameId,
        puzzlesSolved: Int,
        tier: Difficulty,
        score: Int,
        isNewHighScore: Boolean,
        reason: String,
        previousBest: Int,
        fastestSolveSeconds: Double?,
    ) = "challengerResult/${gameId.key}/$puzzlesSolved/${tier.key}/$score/$isNewHighScore/$reason/$previousBest/${fastestSolveSeconds?.toFloat() ?: -1f}"

    fun completion(
        gameId: GameId,
        difficulty: Difficulty,
        score: Int,
        accuracyPct: Int,
        elapsedSeconds: Int,
        isFirstSolve: Boolean,
        isNewHighScore: Boolean,
        bestTile: Int,
        daily: String? = null,
    ) = "completion/${gameId.key}/${difficulty.key}/$score/$accuracyPct/$elapsedSeconds/$isFirstSolve/$isNewHighScore/$bestTile" +
        (daily?.let { "?daily=$it" } ?: "")

    fun loss(gameId: GameId, difficulty: Difficulty, elapsedSeconds: Int, reason: String, score: Int, bestTile: Int, daily: String? = null) =
        "loss/${gameId.key}/${difficulty.key}/$elapsedSeconds/$reason/$score/$bestTile" + (daily?.let { "?daily=$it" } ?: "")

    fun analyzer(gameId: GameId) = "analyzer/${gameId.key}"

    fun supportInfo(key: String) = "supportInfo/$key"

    fun mixEditor(mixId: String) = "mixEditor/$mixId"
}
