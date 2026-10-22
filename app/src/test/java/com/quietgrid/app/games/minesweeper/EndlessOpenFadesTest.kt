package com.quietgrid.app.games.minesweeper

import com.quietgrid.engine.minesweeper.endless.EndlessSection
import com.quietgrid.engine.minesweeper.endless.EndlessWorld
import com.quietgrid.engine.minesweeper.endless.SectionPos
import com.quietgrid.engine.minesweeper.endless.SectionState
import org.junit.Assert.assertEquals
import org.junit.Test

class EndlessOpenFadesTest {

    private val east = SectionPos(1, 0)

    private fun world(state: SectionState, cells: String = "-".repeat(81)) =
        EndlessWorld.of(listOf(EndlessSection(east, state, 1, ".".repeat(81), cells)))

    @Test
    fun `a section that opens fades from dim to clear`() {
        val fades = EndlessOpenFades(durationMillis = 400)
        fades.observe(world(SectionState.BUILT), nowMillis = 0, animate = true)
        fades.observe(world(SectionState.OPEN), nowMillis = 1000, animate = true)

        assertEquals(1f, fades.dim(east, 1000), 0.001f)
        assertEquals(0.5f, fades.dim(east, 1200), 0.001f)
        assertEquals(0f, fades.dim(east, 1400), 0.001f)
    }

    @Test
    fun `another change mid-fade does not stop the fade`() {
        val fades = EndlessOpenFades(durationMillis = 400)
        fades.observe(world(SectionState.BUILT), nowMillis = 0, animate = true)
        fades.observe(world(SectionState.OPEN), nowMillis = 1000, animate = true)

        fades.observe(world(SectionState.OPEN, cells = "o" + "-".repeat(80)), nowMillis = 1100, animate = true)

        assertEquals(0.5f, fades.dim(east, 1200), 0.001f)
        assertEquals(0f, fades.dim(east, 1400), 0.001f)
        assertEquals(false, fades.hasActive(1400))
    }

    @Test
    fun `restoring a run does not fade anything`() {
        val fades = EndlessOpenFades(durationMillis = 400)

        fades.observe(world(SectionState.OPEN), nowMillis = 1000, animate = true)

        assertEquals(0f, fades.dim(east, 1000), 0.001f)
        assertEquals(false, fades.hasActive(1000))
    }

    @Test
    fun `no fade when animations are off`() {
        val fades = EndlessOpenFades(durationMillis = 400)
        fades.observe(world(SectionState.BUILT), nowMillis = 0, animate = false)
        fades.observe(world(SectionState.OPEN), nowMillis = 1000, animate = false)

        assertEquals(0f, fades.dim(east, 1000), 0.001f)
    }
}
