package com.quietgrid.app.session

data class ChallengerConfig(
    val startingSeconds: Double,
    val bonusSeconds: Double,
    val solvesPerTier: Int,
    val startingLives: Int = 0,
)
