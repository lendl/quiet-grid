package com.quietgrid.app.nav

import com.quietgrid.app.core.GameId
import org.junit.Assert.assertEquals
import org.junit.Test

class JourneyAnchorTest {
    private val tabs = JourneyBackEntry(TabsRoute::class, null, null)
    private fun picker(game: String) = JourneyBackEntry(PickerRoute::class, game, null)
    private fun journey(game: String) = JourneyBackEntry(JourneyRoute::class, game, null)
    private fun map(game: String, chapter: Int) = JourneyBackEntry(JourneyMapRoute::class, game, chapter)
    private fun play(game: String) = JourneyBackEntry(PlayRoute::class, game, null)

    @Test
    fun `the same game's journey list is the anchor`() {
        assertEquals(JourneyRoute::class, journeyAnchorRoute("animaldoku", listOf(tabs, picker("animaldoku"), journey("animaldoku"))))
    }

    @Test
    fun `another game's journey list is skipped for the picker`() {
        val stack = listOf(tabs, picker("animaldoku"), journey("animaldoku"), play("arrowescape"))
        assertEquals(PickerRoute::class, journeyAnchorRoute("arrowescape", stack))
    }

    @Test
    fun `tabs when nothing else is on the stack`() {
        assertEquals(TabsRoute::class, journeyAnchorRoute("arrowescape", listOf(tabs, play("arrowescape"))))
    }

    @Test
    fun `same game's map is the anchor before its chapter list`() {
        val stack = listOf(tabs, picker("arrowescape"), journey("arrowescape"), map("arrowescape", 2), play("arrowescape"))
        assertEquals(JourneyMapRoute::class, journeyAnchorRoute("arrowescape", stack))
    }

    @Test
    fun `another game's map is skipped`() {
        val stack = listOf(tabs, picker("animaldoku"), journey("animaldoku"), map("animaldoku", 1), play("arrowescape"))
        assertEquals(PickerRoute::class, journeyAnchorRoute("arrowescape", stack))
    }

    @Test
    fun `returning to the same chapter pops back to its map`() {
        val stack = listOf(tabs, picker("arrowescape"), journey("arrowescape"), map("arrowescape", 2), play("arrowescape"))
        assertEquals(JourneyReturnPlan(JourneyMapRoute::class, inclusive = false, push = emptyList()), journeyReturnPlan(GameId.ARROWESCAPE, 2, stack))
    }

    @Test
    fun `returning to another chapter replaces the map`() {
        val stack = listOf(tabs, picker("arrowescape"), journey("arrowescape"), map("arrowescape", 2), play("arrowescape"))
        assertEquals(
            JourneyReturnPlan(JourneyMapRoute::class, inclusive = true, push = listOf(JourneyMapRoute(GameId.ARROWESCAPE, 3))),
            journeyReturnPlan(GameId.ARROWESCAPE, 3, stack),
        )
    }

    @Test
    fun `returning from the chapter list pushes the stop's map`() {
        val stack = listOf(tabs, picker("arrowescape"), journey("arrowescape"), play("arrowescape"))
        assertEquals(
            JourneyReturnPlan(JourneyRoute::class, inclusive = false, push = listOf(JourneyMapRoute(GameId.ARROWESCAPE, 4))),
            journeyReturnPlan(GameId.ARROWESCAPE, 4, stack),
        )
    }

    @Test
    fun `returning without a map on the stack opens the list and the stop's map`() {
        val stack = listOf(tabs, play("arrowescape"))
        assertEquals(
            JourneyReturnPlan(TabsRoute::class, inclusive = false, push = listOf(JourneyRoute(GameId.ARROWESCAPE), JourneyMapRoute(GameId.ARROWESCAPE, 5))),
            journeyReturnPlan(GameId.ARROWESCAPE, 5, stack),
        )
    }

    @Test
    fun `journey list pops back to the same game's list`() {
        val stack = listOf(tabs, picker("arrowescape"), journey("arrowescape"), map("arrowescape", 2))
        assertEquals(JourneyReturnPlan(JourneyRoute::class, inclusive = false, push = emptyList()), journeyListPlan(GameId.ARROWESCAPE, stack))
    }

    @Test
    fun `journey list is pushed when it is not on the stack`() {
        assertEquals(
            JourneyReturnPlan(TabsRoute::class, inclusive = false, push = listOf(JourneyRoute(GameId.ARROWESCAPE))),
            journeyListPlan(GameId.ARROWESCAPE, listOf(tabs)),
        )
        assertEquals(
            JourneyReturnPlan(PickerRoute::class, inclusive = false, push = listOf(JourneyRoute(GameId.ARROWESCAPE))),
            journeyListPlan(GameId.ARROWESCAPE, listOf(tabs, picker("animaldoku"), journey("animaldoku"))),
        )
    }

    @Test
    fun `unknown chapter returns to the chapter list`() {
        val stack = listOf(tabs, picker("arrowescape"), play("arrowescape"))
        assertEquals(
            JourneyReturnPlan(PickerRoute::class, inclusive = false, push = listOf(JourneyRoute(GameId.ARROWESCAPE))),
            journeyReturnPlan(GameId.ARROWESCAPE, null, stack),
        )
    }
}
