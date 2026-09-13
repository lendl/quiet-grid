package com.quietgrid.app.nav

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId

object Routes {
    const val TABS = "tabs"
    const val PICKER = "picker/{gameId}"
    const val PLAY = "play/{gameId}/{difficulty}/{resume}"
    const val CHALLENGER = "challenger/{gameId}"
    const val CHALLENGER_RESULT = "challengerResult/{gameId}/{puzzlesSolved}/{tier}/{score}/{isNewHighScore}/{reason}/{previousBest}/{fastestSolveSeconds}"
    const val COMPLETION = "completion/{gameId}/{difficulty}/{score}/{accuracyPct}/{elapsedSeconds}/{isFirstSolve}/{isNewHighScore}/{bestTile}"
    const val LOSS = "loss/{gameId}/{difficulty}/{elapsedSeconds}/{reason}/{score}/{bestTile}"
    const val ANALYZER = "analyzer/{gameId}"
    const val SUPPORT_INFO = "supportInfo/{key}"
    const val MIX_EDITOR = "mixEditor/{mixId}"

    const val NEW_MIX_ID = "new"

    fun picker(gameId: GameId) = "picker/${gameId.key}"
    fun play(gameId: GameId, difficulty: Difficulty, resume: Boolean) =
        "play/${gameId.key}/${difficulty.key}/$resume"

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
    ) = "completion/${gameId.key}/${difficulty.key}/$score/$accuracyPct/$elapsedSeconds/$isFirstSolve/$isNewHighScore/$bestTile"

    fun loss(gameId: GameId, difficulty: Difficulty, elapsedSeconds: Int, reason: String, score: Int, bestTile: Int) =
        "loss/${gameId.key}/${difficulty.key}/$elapsedSeconds/$reason/$score/$bestTile"

    fun analyzer(gameId: GameId) = "analyzer/${gameId.key}"

    fun supportInfo(key: String) = "supportInfo/$key"

    fun mixEditor(mixId: String?) = "mixEditor/${mixId ?: NEW_MIX_ID}"
}
