package com.quietgrid.app.games.starbattle

import com.quietgrid.app.session.ChallengerConfig
import com.quietgrid.app.session.ChallengerSession

const val STARBATTLE_CHALLENGER_STARTING_SECONDS = 90.0
const val STARBATTLE_CHALLENGER_BONUS_SECONDS = 15.0
const val STARBATTLE_CHALLENGER_SOLVES_PER_TIER = 3

val STARBATTLE_CHALLENGER_CONFIG = ChallengerConfig(
    startingSeconds = STARBATTLE_CHALLENGER_STARTING_SECONDS,
    bonusSeconds = STARBATTLE_CHALLENGER_BONUS_SECONDS,
    solvesPerTier = STARBATTLE_CHALLENGER_SOLVES_PER_TIER,
    startingLives = STARBATTLE_STARTING_LIVES,
)

typealias StarBattleChallengerSession = ChallengerSession<StarBattleSession>
