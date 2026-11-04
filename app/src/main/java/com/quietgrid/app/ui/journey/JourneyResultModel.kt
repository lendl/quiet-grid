package com.quietgrid.app.ui.journey

import com.quietgrid.app.core.journey.JourneySignals
import com.quietgrid.app.core.journey.journeyRun

enum class JourneyGoalKind { CLEAR, CLEAN, TIME }

data class JourneyGoal(val kind: JourneyGoalKind, val met: Boolean)

data class JourneyResultModel(
    val chapterIndex: Int,
    val stopNumber: Int,
    val elapsedSeconds: Int,
    val targetSeconds: Int,
    val signals: JourneySignals,
    val runStars: Int,
    val bestStars: Int,
    val goals: List<JourneyGoal>,
    val next: JourneyStopRow?,
    val replay: JourneyStopRow?,
)

fun journeyResultModel(rows: List<JourneyStopRow>, stopId: String, elapsedSeconds: Int, signals: JourneySignals): JourneyResultModel? {
    val index = rows.indexOfFirst { it.stop.puzzleId == stopId }
    if (index < 0) return null
    val row = rows[index]
    val run = journeyRun(signals, elapsedSeconds, row.stop.targetSeconds)
    val bestStars = maxOf(row.progress.stars, run.stars)
    return JourneyResultModel(
        chapterIndex = row.chapterIndex,
        stopNumber = row.number,
        elapsedSeconds = elapsedSeconds,
        targetSeconds = row.stop.targetSeconds,
        signals = signals,
        runStars = run.stars,
        bestStars = bestStars,
        goals = listOf(
            JourneyGoal(JourneyGoalKind.CLEAR, true),
            JourneyGoal(JourneyGoalKind.CLEAN, run.clean),
            JourneyGoal(JourneyGoalKind.TIME, run.underTarget),
        ),
        next = rows.drop(index + 1).firstOrNull { it.state == JourneyStopState.CURRENT || it.state == JourneyStopState.OPEN },
        replay = row.takeIf { bestStars < JOURNEY_MAX_STARS_PER_STOP },
    )
}
