package com.quietgrid.app.ui.journey.map

import androidx.compose.ui.graphics.toArgb
import com.quietgrid.app.ui.theme.ResolvedTheme
import com.quietgrid.app.ui.theme.colorSchemeFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JourneySceneTest {
    private val layout = journeyRoadLayout(stopCount = 30, width = 390f)

    @Test
    fun `scene is deterministic for a seed`() {
        JourneySceneKind.entries.forEach { kind ->
            assertEquals(kind.name, journeyScene(kind, layout, 42), journeyScene(kind, layout, 42))
        }
    }

    @Test
    fun `different chapters give different scenes`() {
        val one = journeyScene(JourneySceneKind.SPACE, layout, journeySceneSeed("arrowescape", 1))
        val two = journeyScene(JourneySceneKind.SPACE, layout, journeySceneSeed("arrowescape", 2))
        assertNotEquals(one, two)
        assertNotEquals(journeySceneSeed("arrowescape", 1), journeySceneSeed("animaldoku", 1))
    }

    @Test
    fun `no item comes within 60 dp of the road`() {
        listOf(360f, 480f).forEach { width ->
            val wide = journeyRoadLayout(stopCount = 30, width = width)
            JourneySceneKind.entries.forEach { kind ->
                (1..3).forEach { chapter ->
                    journeyScene(kind, wide, journeySceneSeed("arrowescape", chapter)).filterNot { it.wash }.forEach { item ->
                        val distance = wide.distanceToRoad(item.bounds)
                        assertTrue("$kind width $width ${item.bounds} at $distance", distance >= JOURNEY_SCENE_ROAD_CLEARANCE)
                    }
                }
            }
        }
    }

    @Test
    fun `scene covers the full map height`() {
        JourneySceneKind.entries.forEach { kind ->
            val items = journeyScene(kind, layout, 7)
            val pageHeight = JOURNEY_SCENE_PAGE_HEIGHT * layout.width / JOURNEY_SCENE_PAGE_WIDTH
            var top = 0f
            while (top < layout.height) {
                val bandTop = top
                assertTrue("$kind band $bandTop", items.any { it.bounds.bottom > bandTop && it.bounds.top < bandTop + pageHeight })
                top += pageHeight
            }
            assertTrue(kind.name, items.all { it.bounds.bottom > 0f && it.bounds.top < layout.height })
        }
    }

    @Test
    fun `each theme maps to its scene`() {
        assertEquals(
            mapOf(
                ResolvedTheme.DARK to JourneySceneKind.SPACE,
                ResolvedTheme.LIGHT to JourneySceneKind.SKY,
                ResolvedTheme.PENCIL to JourneySceneKind.SKETCH,
                ResolvedTheme.SILK to JourneySceneKind.ZEN,
                ResolvedTheme.NORD to JourneySceneKind.ARCTIC,
                ResolvedTheme.COFFEE to JourneySceneKind.CAFE,
                ResolvedTheme.DRACULA to JourneySceneKind.GOTHIC,
            ),
            ResolvedTheme.entries.associateWith { journeySceneFor(it) },
        )
    }

    @Test
    fun `pencil palette is grayscale`() {
        val palette = scenePalette(JourneySceneKind.SKETCH, colorSchemeFor(ResolvedTheme.PENCIL))
        ScenePaint.entries.forEach { paint ->
            val colour = palette.colorFor(paint)
            assertTrue("$paint $colour", colour.red == colour.green && colour.green == colour.blue)
        }
    }

    @Test
    fun `nord aurora uses the brief's colours`() {
        val palette = scenePalette(JourneySceneKind.ARCTIC, colorSchemeFor(ResolvedTheme.NORD))
        assertEquals(0xFF88C0D0.toInt(), palette.colorFor(ScenePaint.AURORA_1).copy(alpha = 1f).toArgb())
        assertEquals(0xFFA3BE8C.toInt(), palette.colorFor(ScenePaint.AURORA_2).copy(alpha = 1f).toArgb())
        assertEquals(0xFFB48EAD.toInt(), palette.colorFor(ScenePaint.AURORA_3).copy(alpha = 1f).toArgb())
    }

    @Test
    fun `every scene has a road style with a done stroke`() {
        JourneySceneKind.entries.forEach { kind ->
            val style = journeyRoadStyle(kind)
            assertTrue(kind.name, style.any { it.done } && style.any { !it.done })
        }
    }
}
