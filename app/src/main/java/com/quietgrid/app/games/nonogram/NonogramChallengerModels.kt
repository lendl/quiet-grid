package com.quietgrid.app.games.nonogram

import com.quietgrid.app.session.ChallengerConfig
import com.quietgrid.app.session.ChallengerSession

const val NONOGRAM_CHALLENGER_STARTING_SECONDS = 110.0
const val NONOGRAM_CHALLENGER_BONUS_SECONDS = 20.0
const val NONOGRAM_CHALLENGER_SOLVES_PER_TIER = 3

val NONOGRAM_CHALLENGER_CONFIG = ChallengerConfig(
    startingSeconds = NONOGRAM_CHALLENGER_STARTING_SECONDS,
    bonusSeconds = NONOGRAM_CHALLENGER_BONUS_SECONDS,
    solvesPerTier = NONOGRAM_CHALLENGER_SOLVES_PER_TIER,
)

typealias NonogramChallengerSession = ChallengerSession<NonogramSession>
