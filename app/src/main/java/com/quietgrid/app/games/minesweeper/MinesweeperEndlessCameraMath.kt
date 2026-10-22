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

data class EndlessCameraBounds(val minX: Float, val minY: Float, val maxX: Float, val maxY: Float)

fun endlessCellPx(camera: MinesweeperEndlessCamera, viewportW: Float, viewportH: Float): Float =
    min(viewportW, viewportH) / ENDLESS_DEFAULT_CELLS_ACROSS * camera.zoom

fun endlessWorldPoint(
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
): WorldCell {
    val (x, y) = endlessWorldPoint(camera, viewportW, viewportH, screenX, screenY)
    return WorldCell(floor(x).toInt(), floor(y).toInt())
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
    val (worldX, worldY) = endlessWorldPoint(this, viewportW, viewportH, focusX, focusY)
    val next = copy(zoom = (zoom * factor).coerceIn(ENDLESS_MIN_ZOOM, ENDLESS_MAX_ZOOM))
    val cell = endlessCellPx(next, viewportW, viewportH)
    return next.copy(
        centerX = worldX - (focusX - viewportW / 2) / cell,
        centerY = worldY - (focusY - viewportH / 2) / cell,
    )
}

fun endlessCameraBounds(world: EndlessWorld): EndlessCameraBounds {
    val built = world.sections.values.filter { it.state != SectionState.UNBUILT }.map { it.pos }
        .ifEmpty { listOf(ENDLESS_START_SECTION) }
    val size = ENDLESS_SECTION_SIZE.toFloat()
    return EndlessCameraBounds(
        minX = built.minOf { it.sx } * size - size,
        minY = built.minOf { it.sy } * size - size,
        maxX = (built.maxOf { it.sx } + 1) * size + size,
        maxY = (built.maxOf { it.sy } + 1) * size + size,
    )
}

fun MinesweeperEndlessCamera.clampedTo(bounds: EndlessCameraBounds): MinesweeperEndlessCamera = copy(
    centerX = centerX.coerceIn(bounds.minX, bounds.maxX),
    centerY = centerY.coerceIn(bounds.minY, bounds.maxY),
)
