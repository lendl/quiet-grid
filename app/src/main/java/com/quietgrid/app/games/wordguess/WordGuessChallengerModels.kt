package com.quietgrid.app.games.wordguess

import com.quietgrid.app.session.ChallengerConfig
import com.quietgrid.app.session.ChallengerSession

const val WORDGUESS_CHALLENGER_STARTING_LIVES = 3
const val WORDGUESS_CHALLENGER_STARTING_SECONDS = 150.0
const val WORDGUESS_CHALLENGER_BONUS_SECONDS = 35.0
const val WORDGUESS_CHALLENGER_SOLVES_PER_TIER = 3

val WORDGUESS_CHALLENGER_CONFIG = ChallengerConfig(
    startingSeconds = WORDGUESS_CHALLENGER_STARTING_SECONDS,
    bonusSeconds = WORDGUESS_CHALLENGER_BONUS_SECONDS,
    solvesPerTier = WORDGUESS_CHALLENGER_SOLVES_PER_TIER,
    startingLives = WORDGUESS_CHALLENGER_STARTING_LIVES,
)

typealias WordGuessChallengerSession = ChallengerSession<WordGuessSession>
