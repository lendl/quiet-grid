package com.quietgrid.app.games.chimptest

import com.quietgrid.app.session.ChallengerConfig
import com.quietgrid.app.session.ChallengerSession

const val CHIMPTEST_CHALLENGER_STARTING_LIVES = 3
const val CHIMPTEST_CHALLENGER_STARTING_SECONDS = 90.0
const val CHIMPTEST_CHALLENGER_BONUS_SECONDS = 25.0
const val CHIMPTEST_CHALLENGER_SOLVES_PER_TIER = 3

val CHIMPTEST_CHALLENGER_CONFIG = ChallengerConfig(
    startingSeconds = CHIMPTEST_CHALLENGER_STARTING_SECONDS,
    bonusSeconds = CHIMPTEST_CHALLENGER_BONUS_SECONDS,
    solvesPerTier = CHIMPTEST_CHALLENGER_SOLVES_PER_TIER,
    startingLives = CHIMPTEST_CHALLENGER_STARTING_LIVES,
)

typealias ChimpTestChallengerSession = ChallengerSession<ChimpTestSession>
