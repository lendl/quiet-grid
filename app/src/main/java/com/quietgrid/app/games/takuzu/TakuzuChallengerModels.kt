package com.quietgrid.app.games.takuzu

import com.quietgrid.app.session.ChallengerConfig
import com.quietgrid.app.session.ChallengerSession

const val TAKUZU_CHALLENGER_STARTING_SECONDS = 100.0
const val TAKUZU_CHALLENGER_BONUS_SECONDS = 20.0
const val TAKUZU_CHALLENGER_SOLVES_PER_TIER = 3

val TAKUZU_CHALLENGER_CONFIG = ChallengerConfig(
    startingSeconds = TAKUZU_CHALLENGER_STARTING_SECONDS,
    bonusSeconds = TAKUZU_CHALLENGER_BONUS_SECONDS,
    solvesPerTier = TAKUZU_CHALLENGER_SOLVES_PER_TIER,
)

typealias TakuzuChallengerSession = ChallengerSession<TakuzuSession>
