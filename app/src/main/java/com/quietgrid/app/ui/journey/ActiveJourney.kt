package com.quietgrid.app.ui.journey

import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.journey.JOURNEY_GAMES

data class ActiveJourney(val gameId: GameId, val chapterIndex: Int, val stopNumber: Int)

fun journeyCandidates(gameKeysByRecency: List<String>, enabledGameIds: Set<GameId>): List<GameId> {
    val enabled = JOURNEY_GAMES.filter { it in enabledGameIds }.associateBy { it.key }
    return gameKeysByRecency.mapNotNull { enabled[it] }.distinct()
}

fun firstActiveJourney(summaries: List<Pair<GameId, JourneySummary?>>): ActiveJourney? =
    summaries.firstNotNullOfOrNull { (gameId, summary) -> summary?.activeIn(gameId) }

private fun JourneySummary.activeIn(gameId: GameId): ActiveJourney? {
    val chapter = chapterIndex ?: return null
    val stop = stopNumber ?: return null
    return ActiveJourney(gameId, chapter, stop)
}
