package com.quietgrid.app.games.blockescape

import com.quietgrid.app.session.ChallengerConfig
import com.quietgrid.app.session.ChallengerSession

const val BLOCKESCAPE_CHALLENGER_STARTING_SECONDS = 120.0
const val BLOCKESCAPE_CHALLENGER_BONUS_SECONDS = 30.0
const val BLOCKESCAPE_CHALLENGER_SOLVES_PER_TIER = 3

val BLOCKESCAPE_CHALLENGER_CONFIG = ChallengerConfig(
    startingSeconds = BLOCKESCAPE_CHALLENGER_STARTING_SECONDS,
    bonusSeconds = BLOCKESCAPE_CHALLENGER_BONUS_SECONDS,
    solvesPerTier = BLOCKESCAPE_CHALLENGER_SOLVES_PER_TIER,
)

typealias BlockEscapeChallengerSession = ChallengerSession<BlockEscapeSession>
