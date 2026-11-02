package com.quietgrid.app.ui.journey.map

import com.quietgrid.app.core.GameId
import com.quietgrid.app.ui.components.GAME_ICON_GLYPHS
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.random.Random

const val JOURNEY_GLYPH_PAGE_HEIGHT = 844f
const val JOURNEY_GLYPHS_PER_PAGE = 16
private const val GLYPH_MIN_SIZE = 16
private const val GLYPH_MAX_SIZE = 22
private const val GLYPH_SPACING = 34f
private const val GLYPH_ATTEMPTS_PER_PAGE = 4000

data class GlyphPlacement(val gameId: GameId, val x: Float, val y: Float, val size: Float)

fun journeyGlyphs(layout: JourneyRoadLayout, seed: Int): List<GlyphPlacement> {
    val games = GameId.entries.filter { it != GameId.SUDOKU && GAME_ICON_GLYPHS.containsKey(it) }
    if (games.size < 2 || layout.width < GLYPH_MAX_SIZE) return emptyList()
    val random = Random(seed * 17 + 5)
    val samples = layout.roadSamples()
    val placed = mutableListOf<GlyphPlacement>()
    var top = 0f
    while (top < layout.height) {
        val span = minOf(JOURNEY_GLYPH_PAGE_HEIGHT, layout.height - top)
        val target = (JOURNEY_GLYPHS_PER_PAGE * span / JOURNEY_GLYPH_PAGE_HEIGHT).roundToInt()
        var count = 0
        var attempts = 0
        while (count < target && attempts < GLYPH_ATTEMPTS_PER_PAGE && span >= GLYPH_MAX_SIZE) {
            attempts++
            val size = (GLYPH_MIN_SIZE + random.nextInt(GLYPH_MAX_SIZE - GLYPH_MIN_SIZE + 1)).toFloat()
            val half = size / 2
            val x = half + random.nextFloat() * (layout.width - size)
            val y = top + half + random.nextFloat() * (span - size)
            val bounds = SceneBounds(x - half, y - half, x + half, y + half)
            if (layout.distanceToRoad(bounds, samples) < JOURNEY_SCENE_ROAD_CLEARANCE) continue
            if (placed.any { hypot(it.x - x, it.y - y) < GLYPH_SPACING }) continue
            val previous = placed.lastOrNull()?.gameId
            val choices = games.filter { it != previous }
            placed += GlyphPlacement(choices[random.nextInt(choices.size)], x, y, size)
            count++
        }
        top += JOURNEY_GLYPH_PAGE_HEIGHT
    }
    return placed
}
