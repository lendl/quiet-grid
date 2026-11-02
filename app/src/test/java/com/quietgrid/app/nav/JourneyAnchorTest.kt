package com.quietgrid.app.nav

import com.quietgrid.app.core.GameId
import org.junit.Assert.assertEquals
import org.junit.Test

class JourneyAnchorTest {
    private val tabs = JourneyBackEntry(Routes.TABS, null, null)
    private fun picker(game: String) = JourneyBackEntry(Routes.PICKER, game, null)
    private fun journey(game: String) = JourneyBackEntry(Routes.JOURNEY, game, null)
    private fun map(game: String, chapter: Int) = JourneyBackEntry(Routes.JOURNEY_MAP, game, chapter)
    private fun play(game: String) = JourneyBackEntry(Routes.PLAY, game, null)

    @Test
    fun `the same game's journey list is the anchor`() {
        assertEquals(Routes.JOURNEY, journeyAnchorRoute("animaldoku", listOf(tabs, picker("animaldoku"), journey("animaldoku"))))
    }

    @Test
    fun `another game's journey list is skipped for the picker`() {
        val stack = listOf(tabs, picker("animaldoku"), journey("animaldoku"), play("arrowescape"))
        assertEquals(Routes.PICKER, journeyAnchorRoute("arrowescape", stack))
    }

    @Test
    fun `tabs when nothing else is on the stack`() {
        assertEquals(Routes.TABS, journeyAnchorRoute("arrowescape", listOf(tabs, play("arrowescape"))))
    }

    @Test
    fun `same game's map is the anchor before its chapter list`() {
        val stack = listOf(tabs, picker("arrowescape"), journey("arrowescape"), map("arrowescape", 2), play("arrowescape"))
        assertEquals(Routes.JOURNEY_MAP, journeyAnchorRoute("arrowescape", stack))
    }

    @Test
    fun `another game's map is skipped`() {
        val stack = listOf(tabs, picker("animaldoku"), journey("animaldoku"), map("animaldoku", 1), play("arrowescape"))
        assertEquals(Routes.PICKER, journeyAnchorRoute("arrowescape", stack))
    }

    @Test
    fun `returning to the same chapter pops back to its map`() {
        val stack = listOf(tabs, picker("arrowescape"), journey("arrowescape"), map("arrowescape", 2), play("arrowescape"))
        assertEquals(JourneyReturnPlan(Routes.JOURNEY_MAP, inclusive = false, push = emptyList()), journeyReturnPlan(GameId.ARROWESCAPE, 2, stack))
    }

    @Test
    fun `returning to another chapter replaces the map`() {
        val stack = listOf(tabs, picker("arrowescape"), journey("arrowescape"), map("arrowescape", 2), play("arrowescape"))
        assertEquals(
            JourneyReturnPlan(Routes.JOURNEY_MAP, inclusive = true, push = listOf("journeyMap/arrowescape/3")),
            journeyReturnPlan(GameId.ARROWESCAPE, 3, stack),
        )
    }

    @Test
    fun `returning from the chapter list pushes the stop's map`() {
        val stack = listOf(tabs, picker("arrowescape"), journey("arrowescape"), play("arrowescape"))
        assertEquals(
            JourneyReturnPlan(Routes.JOURNEY, inclusive = false, push = listOf("journeyMap/arrowescape/4")),
            journeyReturnPlan(GameId.ARROWESCAPE, 4, stack),
        )
    }

    @Test
    fun `returning without a map on the stack opens the list and the stop's map`() {
        val stack = listOf(tabs, play("arrowescape"))
        assertEquals(
            JourneyReturnPlan(Routes.TABS, inclusive = false, push = listOf("journey/arrowescape", "journeyMap/arrowescape/5")),
            journeyReturnPlan(GameId.ARROWESCAPE, 5, stack),
        )
    }

    @Test
    fun `unknown chapter returns to the chapter list`() {
        val stack = listOf(tabs, picker("arrowescape"), play("arrowescape"))
        assertEquals(
            JourneyReturnPlan(Routes.PICKER, inclusive = false, push = listOf("journey/arrowescape")),
            journeyReturnPlan(GameId.ARROWESCAPE, null, stack),
        )
    }
}
