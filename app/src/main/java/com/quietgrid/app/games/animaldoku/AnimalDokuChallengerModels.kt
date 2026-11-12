package com.quietgrid.app.games.animaldoku

import com.quietgrid.app.session.ChallengerConfig
import com.quietgrid.app.session.ChallengerSession

const val ANIMALDOKU_CHALLENGER_STARTING_SECONDS = 90.0
const val ANIMALDOKU_CHALLENGER_BONUS_SECONDS = 15.0
const val ANIMALDOKU_CHALLENGER_SOLVES_PER_TIER = 3

val ANIMALDOKU_CHALLENGER_CONFIG = ChallengerConfig(
    startingSeconds = ANIMALDOKU_CHALLENGER_STARTING_SECONDS,
    bonusSeconds = ANIMALDOKU_CHALLENGER_BONUS_SECONDS,
    solvesPerTier = ANIMALDOKU_CHALLENGER_SOLVES_PER_TIER,
    startingLives = ANIMALDOKU_STARTING_LIVES,
)

typealias AnimalDokuChallengerSession = ChallengerSession<AnimalDokuSession>
