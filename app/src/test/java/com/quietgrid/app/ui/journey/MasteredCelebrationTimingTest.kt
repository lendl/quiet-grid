package com.quietgrid.app.ui.journey

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MasteredCelebrationTimingTest {
    private val center = Offset(150f, 150f)
    private val outline = constellationOutline(center, 138f)

    private fun assertClose(expected: Offset, actual: Offset) {
        assertEquals(expected.x, actual.x, 0.01f)
        assertEquals(expected.y, actual.y, 0.01f)
    }

    @Test
    fun `outline starts straight up and turns clockwise`() {
        assertEquals(10, outline.size)
        assertClose(Offset(150f, 12f), outline[0])
        assertTrue(outline[1].x > center.x && outline[1].y < center.y)
        assertEquals(138f * CONSTELLATION_INNER_RATIO, (outline[1] - center).getDistance(), 0.01f)
    }

    @Test
    fun `ten chapters land on the ten vertices`() {
        constellationNodes(outline, 10).zip(outline).forEach { (node, vertex) -> assertClose(vertex, node) }
    }

    @Test
    fun `five chapters land on the outer points`() {
        val nodes = constellationNodes(outline, 5)
        nodes.forEachIndexed { k, node -> assertClose(outline[2 * k], node) }
    }

    @Test
    fun `four chapters spread evenly by length`() {
        val nodes = constellationNodes(outline, 4)
        assertClose(outline[0], nodes[0])
        assertClose((outline[2] + outline[3]) / 2f, nodes[1])
        assertClose(outline[5], nodes[2])
        assertClose((outline[7] + outline[8]) / 2f, nodes[3])
    }

    @Test
    fun `radius follows the width up to the cap`() {
        assertEquals(138f, constellationOuterRadius(480f, 138f), 0f)
        assertEquals(0.36f * 300f, constellationOuterRadius(300f, 138f), 0.0001f)
    }

    @Test
    fun `lighting up takes three seconds whatever the chapter count`() {
        listOf(4, 7, 10).forEach { n ->
            assertEquals(MASTERED_LIGHT_START_MS, masteredNodeStartMs(0, n))
            assertEquals(MASTERED_LIGHT_START_MS + MASTERED_LIGHT_SPAN_MS, masteredNodeStartMs(n, n))
        }
    }

    @Test
    fun `a node lights and pops once`() {
        val start = masteredNodeStartMs(3, 10)
        assertFalse(masteredNodeFrame(start, 3, 10).lit)
        assertTrue(masteredNodeFrame(start + 1, 3, 10).lit)
        val peak = (0..MASTERED_NODE_POP_MS.toInt()).maxOf { masteredNodeFrame(start + it, 3, 10).scale }
        assertEquals(1f + MASTERED_NODE_POP_GROWTH, peak, 0.01f)
        assertEquals(1f, masteredNodeFrame(start + MASTERED_NODE_POP_MS + 1, 3, 10).scale, 0.0001f)
    }

    @Test
    fun `nodes turn gold in order`() {
        assertFalse(masteredNodeFrame(MASTERED_GOLD_START_MS, 0, 10).gold)
        assertTrue(masteredNodeFrame(MASTERED_GOLD_START_MS + 1, 0, 10).gold)
        assertFalse(masteredNodeFrame(MASTERED_GOLD_START_MS + 1, 9, 10).gold)
        assertTrue(masteredNodeFrame(MASTERED_GOLD_START_MS + 9 * MASTERED_GOLD_NODE_STAGGER_MS + 1, 9, 10).gold)
    }

    @Test
    fun `each line draws after its node and the closing line comes last`() {
        val start = masteredNodeStartMs(2, 10) + MASTERED_LINE_DELAY_MS
        assertEquals(0f, masteredLineProgress(start, 2, 10), 0f)
        assertEquals(1f, masteredLineProgress(start + 360, 2, 10), 0f)
        assertTrue(masteredLineProgress(MASTERED_LIGHT_START_MS + MASTERED_LIGHT_SPAN_MS - 100, 9, 10) < 1f)
        assertEquals(1f, masteredLineProgress(MASTERED_GOLD_START_MS, 9, 10), 0f)
    }

    @Test
    fun `lines hand over to the gold outline`() {
        assertEquals(MASTERED_LINE_ALPHA, masteredLineAlpha(MASTERED_GOLD_START_MS), 0f)
        assertEquals(0f, masteredGoldProgress(MASTERED_GOLD_START_MS), 0f)
        val done = MASTERED_GOLD_START_MS + MASTERED_GOLD_DURATION_MS
        assertEquals(0f, masteredLineAlpha(done), 0f)
        assertEquals(1f, masteredGoldProgress(done), 0f)
    }

    @Test
    fun `center star overshoots and settles upright`() {
        assertEquals(CenterStarFrame(0f, MASTERED_STAR_TURN_DEGREES, 0f), masteredCenterStarFrame(MASTERED_STAR_START_MS))
        val peak = (0..MASTERED_STAR_DURATION_MS.toInt()).maxOf { masteredCenterStarFrame(MASTERED_STAR_START_MS + it).scale }
        assertTrue("peak $peak", peak > 1.02f && peak < 1.25f)
        val settled = masteredCenterStarFrame(MASTERED_CELEBRATION_TOTAL_MS)
        assertEquals(1f, settled.scale, 0.0001f)
        assertEquals(0f, settled.rotationDegrees, 0.0001f)
        assertEquals(1f, settled.alpha, 0f)
    }

    @Test
    fun `halo fades in after the star`() {
        assertEquals(0f, masteredHaloAlpha(MASTERED_HALO_START_MS), 0f)
        assertEquals(MASTERED_HALO_ALPHA, masteredHaloAlpha(MASTERED_CELEBRATION_TOTAL_MS), 0.0001f)
    }

    @Test
    fun `star field is fixed per seed and stays inside its ranges`() {
        val sky = starField(42)
        assertEquals(STAR_FIELD_COUNT, sky.size)
        assertEquals(sky, starField(42))
        assertNotEquals(sky, starField(43))
        sky.forEach { dot ->
            assertTrue(dot.x in 0f..1f && dot.y in 0f..1f)
            assertTrue(dot.radiusDp in 0.6f..1.8f)
            assertTrue(dot.alpha in 0.15f..0.45f)
        }
    }

    @Test
    fun `twinkle never fully vanishes and holds still without motion`() {
        val dot = SkyDot(0.5f, 0.5f, 1f, 0.4f, 0f)
        val lowest = (0..628).minOf { twinkleAlpha(dot, it / 100f) }
        assertEquals(0.4f * 0.2f, lowest, 0.001f)
        assertEquals(0.4f, twinkleAlpha(dot, null), 0f)
    }
}
