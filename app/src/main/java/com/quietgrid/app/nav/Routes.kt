package com.quietgrid.app.nav

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId

object Routes {
    const val TABS = "tabs"
    const val PICKER = "picker/{gameId}"
    const val PLAY = "play/{gameId}/{difficulty}/{resume}"
    const val CHALLENGER = "challenger/{gameId}"
    const val CHALLENGER_RESULT = "challengerResult/{gameId}/{puzzlesSolved}/{tier}/{score}/{isNewHighScore}/{reason}"
    const val COMPLETION = "completion/{gameId}/{difficulty}/{score}/{accuracyPct}/{elapsedSeconds}/{isFirstSolve}/{isNewHighScore}"
    const val LOSS = "loss/{gameId}/{difficulty}/{elapsedSeconds}/{reason}"
    const val ANALYZER = "analyzer/{gameId}"
    const val SUPPORT_INFO = "supportInfo/{key}"

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
    ) = "challengerResult/${gameId.key}/$puzzlesSolved/${tier.key}/$score/$isNewHighScore/$reason"

    fun completion(
        gameId: GameId,
        difficulty: Difficulty,
        score: Int,
        accuracyPct: Int,
        elapsedSeconds: Int,
        isFirstSolve: Boolean,
        isNewHighScore: Boolean,
    ) = "completion/${gameId.key}/${difficulty.key}/$score/$accuracyPct/$elapsedSeconds/$isFirstSolve/$isNewHighScore"

    fun loss(gameId: GameId, difficulty: Difficulty, elapsedSeconds: Int, reason: String) =
        "loss/${gameId.key}/${difficulty.key}/$elapsedSeconds/$reason"

    fun analyzer(gameId: GameId) = "analyzer/${gameId.key}"

    fun supportInfo(key: String) = "supportInfo/$key"
}
