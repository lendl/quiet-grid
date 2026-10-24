package com.quietgrid.app.games.minesweeper

import com.quietgrid.engine.minesweeper.endless.EndlessSection
import com.quietgrid.engine.minesweeper.endless.EndlessWorld
import com.quietgrid.engine.minesweeper.endless.SectionPos
import com.quietgrid.engine.minesweeper.endless.SectionState
import com.quietgrid.engine.minesweeper.endless.WorldCell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MinesweeperEndlessCameraMathTest {

    private val width = 1100f
    private val height = 2000f

    @Test
    fun `default camera puts eleven cells across the short side`() {
        assertEquals(100f, endlessCellPx(MinesweeperEndlessCamera(), width, height), 0.001f)
        assertEquals(100f, endlessCellPx(MinesweeperEndlessCamera(), height, width), 0.001f)
    }

    @Test
    fun `screen centre maps to the camera centre cell`() {
        assertEquals(WorldCell(4, 4), endlessCellAt(MinesweeperEndlessCamera(), width, height, width / 2, height / 2))
        assertEquals(WorldCell(0, 4), endlessCellAt(MinesweeperEndlessCamera(), width, height, 105f, height / 2))
        assertNull(endlessCellAt(MinesweeperEndlessCamera(), width, height, 95f, height / 2))
        assertEquals(WorldCell(-1, 4), endlessCellAt(MinesweeperEndlessCamera(), width, height, 70f, height / 2))
    }

    @Test
    fun `a touch in a gutter maps to no cell`() {
        assertNull(endlessCellAt(MinesweeperEndlessCamera(), width, height, 1010f, height / 2))
        assertEquals(WorldCell(9, 4), endlessCellAt(MinesweeperEndlessCamera(), width, height, 1030f, height / 2))
    }

    @Test
    fun `layout and world coordinates round-trip`() {
        for (x in -30..30) assertEquals("x $x", x, endlessWorldOf(endlessLayoutOf(x) + 0.5f))
        assertEquals(-1.25f, endlessLayoutOf(-1), 0.0001f)
        assertEquals(9.25f, endlessLayoutOf(9), 0.0001f)
    }

    @Test
    fun `cell lookup follows pan and zoom`() {
        val panned = MinesweeperEndlessCamera().panned(-100f, 0f, width, height)
        assertEquals(WorldCell(5, 4), endlessCellAt(panned, width, height, width / 2, height / 2))

        val zoomedOut = MinesweeperEndlessCamera(zoom = 0.5f)
        assertEquals(50f, endlessCellPx(zoomedOut, width, height), 0.001f)
        assertEquals(WorldCell(5, 4), endlessCellAt(zoomedOut, width, height, width / 2 + 60f, height / 2))
    }

    @Test
    fun `zoom stays between three sections across and one and a half times`() {
        val camera = MinesweeperEndlessCamera()

        assertEquals(ENDLESS_MAX_ZOOM, camera.zoomed(100f, 0f, 0f, width, height).zoom, 0.0001f)
        assertEquals(ENDLESS_MIN_ZOOM, camera.zoomed(0.001f, 0f, 0f, width, height).zoom, 0.0001f)
        assertEquals(27f, width / endlessCellPx(camera.zoomed(0.001f, 0f, 0f, width, height), width, height), 0.01f)
    }

    @Test
    fun `zooming keeps the point under the fingers in place`() {
        val camera = MinesweeperEndlessCamera(centerX = 3f, centerY = -2f)
        val focusX = 200f
        val focusY = 300f

        val before = endlessLayoutPoint(camera, width, height, focusX, focusY)
        val after = endlessLayoutPoint(camera.zoomed(1.2f, focusX, focusY, width, height), width, height, focusX, focusY)

        assertEquals(before.first, after.first, 0.001f)
        assertEquals(before.second, after.second, 0.001f)
    }

    @Test
    fun `panning stops one section past the built area`() {
        val emptyBounds = endlessCameraBounds(EndlessWorld.EMPTY)
        assertEquals(EndlessCameraBounds(-9.25f, -9.25f, 18.5f, 18.5f), emptyBounds)

        val clamped = MinesweeperEndlessCamera(1000f, -1000f, 0.8f).clampedTo(emptyBounds)
        assertEquals(MinesweeperEndlessCamera(18.5f, -9.25f, 0.8f), clamped)

        val blank = ".".repeat(81)
        val hidden = "-".repeat(81)
        val world = EndlessWorld.of(
            listOf(
                EndlessSection(SectionPos(0, 0), SectionState.OPEN, 1, blank, hidden),
                EndlessSection(SectionPos(1, 0), SectionState.BUILT, 1, blank, hidden),
                EndlessSection(SectionPos(2, 0), SectionState.UNBUILT, 0, blank, hidden),
            ),
        )
        assertEquals(EndlessCameraBounds(-9.25f, -9.25f, 27.75f, 18.5f), endlessCameraBounds(world))
    }
}
