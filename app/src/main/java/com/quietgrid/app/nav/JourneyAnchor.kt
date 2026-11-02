package com.quietgrid.app.nav

import com.quietgrid.app.core.GameId

data class JourneyBackEntry(val route: String?, val gameKey: String?, val chapter: Int?)

data class JourneyReturnPlan(val popTo: String, val inclusive: Boolean, val push: List<String>)

fun journeyAnchorRoute(gameKey: String, backStack: List<JourneyBackEntry>): String {
    val topMap = backStack.lastOrNull { it.route == Routes.JOURNEY_MAP }
    val topJourney = backStack.lastOrNull { it.route == Routes.JOURNEY }
    return when {
        topMap?.gameKey == gameKey -> Routes.JOURNEY_MAP
        topJourney?.gameKey == gameKey -> Routes.JOURNEY
        backStack.any { it.route == Routes.PICKER } -> Routes.PICKER
        else -> Routes.TABS
    }
}

fun journeyReturnPlan(gameId: GameId, chapter: Int?, backStack: List<JourneyBackEntry>): JourneyReturnPlan {
    val mapRoute = chapter?.let { Routes.journeyMap(gameId, it) }
    return when (val anchor = journeyAnchorRoute(gameId.key, backStack)) {
        Routes.JOURNEY_MAP -> {
            val topChapter = backStack.last { it.route == Routes.JOURNEY_MAP }.chapter
            if (mapRoute == null || topChapter == chapter) {
                JourneyReturnPlan(Routes.JOURNEY_MAP, inclusive = false, push = emptyList())
            } else {
                JourneyReturnPlan(Routes.JOURNEY_MAP, inclusive = true, push = listOf(mapRoute))
            }
        }
        Routes.JOURNEY -> JourneyReturnPlan(Routes.JOURNEY, inclusive = false, push = listOfNotNull(mapRoute))
        else -> JourneyReturnPlan(anchor, inclusive = false, push = listOfNotNull(Routes.journey(gameId), mapRoute))
    }
}
