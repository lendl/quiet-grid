package com.quietgrid.app.games

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.journey.JourneyRequest
import com.quietgrid.app.core.journey.JourneySignals
import com.quietgrid.app.session.LossReason
import java.time.LocalDate

data class PlayRequest(
    val difficulty: Difficulty,
    val resume: Boolean,
    val dailyDate: LocalDate?,
    val journey: JourneyRequest?,
    val onRestarted: () -> Unit,
    val onBack: () -> Unit,
    val onFinished: (GameFinish) -> Unit,
)

data class GameFinish(
    val difficulty: Difficulty,
    val solved: Boolean,
    val score: Int,
    val accuracyPct: Int,
    val elapsedSeconds: Int,
    val isFirstSolve: Boolean,
    val isNewHighScore: Boolean,
    val lossReason: LossReason?,
    val journeySignals: JourneySignals = JourneySignals(0, 0),
    val bestTile: Int = 0,
)
