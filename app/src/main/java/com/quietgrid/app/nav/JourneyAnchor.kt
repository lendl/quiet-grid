package com.quietgrid.app.nav

import com.quietgrid.app.core.GameId
import kotlin.reflect.KClass

data class JourneyBackEntry(val route: KClass<*>, val gameKey: String?, val chapter: Int?)

data class JourneyReturnPlan(val popTo: KClass<*>, val inclusive: Boolean, val push: List<Any>)

fun journeyAnchorRoute(gameKey: String, backStack: List<JourneyBackEntry>): KClass<*> {
    val topMap = backStack.lastOrNull { it.route == JourneyMapRoute::class }
    val topJourney = backStack.lastOrNull { it.route == JourneyRoute::class }
    return when {
        topMap?.gameKey == gameKey -> JourneyMapRoute::class
        topJourney?.gameKey == gameKey -> JourneyRoute::class
        backStack.any { it.route == PickerRoute::class } -> PickerRoute::class
        else -> TabsRoute::class
    }
}

fun journeyReturnPlan(gameId: GameId, chapter: Int?, backStack: List<JourneyBackEntry>): JourneyReturnPlan {
    val mapRoute = chapter?.let { JourneyMapRoute(gameId, it) }
    return when (val anchor = journeyAnchorRoute(gameId.key, backStack)) {
        JourneyMapRoute::class -> {
            val topChapter = backStack.last { it.route == JourneyMapRoute::class }.chapter
            if (mapRoute == null || topChapter == chapter) {
                JourneyReturnPlan(JourneyMapRoute::class, inclusive = false, push = emptyList())
            } else {
                JourneyReturnPlan(JourneyMapRoute::class, inclusive = true, push = listOf(mapRoute))
            }
        }
        JourneyRoute::class -> JourneyReturnPlan(JourneyRoute::class, inclusive = false, push = listOfNotNull(mapRoute))
        else -> JourneyReturnPlan(anchor, inclusive = false, push = listOfNotNull(JourneyRoute(gameId), mapRoute))
    }
}

fun journeyListPlan(gameId: GameId, backStack: List<JourneyBackEntry>): JourneyReturnPlan {
    if (backStack.lastOrNull { it.route == JourneyRoute::class }?.gameKey == gameId.key) {
        return JourneyReturnPlan(JourneyRoute::class, inclusive = false, push = emptyList())
    }
    val anchor = journeyAnchorRoute(gameId.key, backStack)
    return JourneyReturnPlan(anchor, inclusive = anchor == JourneyMapRoute::class, push = listOf(JourneyRoute(gameId)))
}
