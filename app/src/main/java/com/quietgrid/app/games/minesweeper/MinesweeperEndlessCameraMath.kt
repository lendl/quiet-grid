package com.quietgrid.app.games.minesweeper

import com.quietgrid.engine.minesweeper.endless.ENDLESS_SECTION_SIZE
import com.quietgrid.engine.minesweeper.endless.ENDLESS_START_SECTION
import com.quietgrid.engine.minesweeper.endless.EndlessWorld
import com.quietgrid.engine.minesweeper.endless.SectionState
import com.quietgrid.engine.minesweeper.endless.WorldCell
import kotlin.math.floor
import kotlin.math.min

const val ENDLESS_DEFAULT_CELLS_ACROSS = 11
const val ENDLESS_MIN_ZOOM = 11f / 27f
const val ENDLESS_MAX_ZOOM = 1.5f
const val ENDLESS_GUTTER_CELLS = 0.25f
const val ENDLESS_SECTION_SPAN = ENDLESS_SECTION_SIZE + ENDLESS_GUTTER_CELLS

data class EndlessCameraBounds(val minX: Float, val minY: Float, val maxX: Float, val maxY: Float)

fun endlessLayoutOf(world: Int): Float = world + Math.floorDiv(world, ENDLESS_SECTION_SIZE) * ENDLESS_GUTTER_CELLS

fun endlessWorldOf(layout: Float): Int? {
    val section = floor(layout / ENDLESS_SECTION_SPAN).toInt()
    val local = layout - section * ENDLESS_SECTION_SPAN
    if (local >= ENDLESS_SECTION_SIZE) return null
    return section * ENDLESS_SECTION_SIZE + floor(local).toInt()
}

fun endlessCellPx(camera: MinesweeperEndlessCamera, viewportW: Float, viewportH: Float): Float =
    min(viewportW, viewportH) / ENDLESS_DEFAULT_CELLS_ACROSS * camera.zoom

fun endlessLayoutPoint(
    camera: MinesweeperEndlessCamera,
    viewportW: Float,
    viewportH: Float,
    screenX: Float,
    screenY: Float,
): Pair<Float, Float> {
    val cell = endlessCellPx(camera, viewportW, viewportH)
    return (camera.centerX + (screenX - viewportW / 2) / cell) to (camera.centerY + (screenY - viewportH / 2) / cell)
}

fun endlessCellAt(
    camera: MinesweeperEndlessCamera,
    viewportW: Float,
    viewportH: Float,
    screenX: Float,
    screenY: Float,
): WorldCell? {
    val (x, y) = endlessLayoutPoint(camera, viewportW, viewportH, screenX, screenY)
    val worldX = endlessWorldOf(x) ?: return null
    val worldY = endlessWorldOf(y) ?: return null
    return WorldCell(worldX, worldY)
}

fun MinesweeperEndlessCamera.panned(dxPx: Float, dyPx: Float, viewportW: Float, viewportH: Float): MinesweeperEndlessCamera {
    val cell = endlessCellPx(this, viewportW, viewportH)
    return copy(centerX = centerX - dxPx / cell, centerY = centerY - dyPx / cell)
}

fun MinesweeperEndlessCamera.zoomed(
    factor: Float,
    focusX: Float,
    focusY: Float,
    viewportW: Float,
    viewportH: Float,
): MinesweeperEndlessCamera {
    val (layoutX, layoutY) = endlessLayoutPoint(this, viewportW, viewportH, focusX, focusY)
    val next = copy(zoom = (zoom * factor).coerceIn(ENDLESS_MIN_ZOOM, ENDLESS_MAX_ZOOM))
    val cell = endlessCellPx(next, viewportW, viewportH)
    return next.copy(
        centerX = layoutX - (focusX - viewportW / 2) / cell,
        centerY = layoutY - (focusY - viewportH / 2) / cell,
    )
}

fun endlessCameraBounds(world: EndlessWorld): EndlessCameraBounds {
    val built = world.sections.values.filter { it.state != SectionState.UNBUILT }.map { it.pos }
        .ifEmpty { listOf(ENDLESS_START_SECTION) }
    return EndlessCameraBounds(
        minX = built.minOf { it.sx } * ENDLESS_SECTION_SPAN - ENDLESS_SECTION_SPAN,
        minY = built.minOf { it.sy } * ENDLESS_SECTION_SPAN - ENDLESS_SECTION_SPAN,
        maxX = (built.maxOf { it.sx } + 1) * ENDLESS_SECTION_SPAN + ENDLESS_SECTION_SPAN,
        maxY = (built.maxOf { it.sy } + 1) * ENDLESS_SECTION_SPAN + ENDLESS_SECTION_SPAN,
    )
}

fun MinesweeperEndlessCamera.clampedTo(bounds: EndlessCameraBounds): MinesweeperEndlessCamera = copy(
    centerX = centerX.coerceIn(bounds.minX, bounds.maxX),
    centerY = centerY.coerceIn(bounds.minY, bounds.maxY),
)
