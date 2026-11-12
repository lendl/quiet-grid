package com.quietgrid.app.games.circuit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.quietgrid.engine.circuit.CIRCUIT_DOWN
import com.quietgrid.engine.circuit.CIRCUIT_LEFT
import com.quietgrid.engine.circuit.CIRCUIT_RIGHT
import com.quietgrid.engine.circuit.CIRCUIT_UP

private val PICTURE_HEIGHT = 40.dp

private class MiniTile(val col: Int, val row: Int, val mask: Int, val lit: Boolean, val source: Boolean = false)

@Composable
internal fun CircuitJoinPicture() {
    CircuitMiniPicture(
        cols = 3,
        rows = 1,
        tiles = listOf(
            MiniTile(0, 0, CIRCUIT_RIGHT, lit = true),
            MiniTile(1, 0, CIRCUIT_LEFT or CIRCUIT_RIGHT, lit = true, source = true),
            MiniTile(2, 0, CIRCUIT_LEFT, lit = true),
        ),
    )
}

@Composable
internal fun CircuitNoLooseEndsPicture() {
    CircuitMiniPicture(
        cols = 3,
        rows = 1,
        tiles = listOf(
            MiniTile(0, 0, CIRCUIT_RIGHT, lit = true),
            MiniTile(1, 0, CIRCUIT_LEFT or CIRCUIT_RIGHT, lit = true, source = true),
            MiniTile(2, 0, CIRCUIT_UP, lit = false),
        ),
    )
}

@Composable
internal fun CircuitNoLoopsPicture() {
    CircuitMiniPicture(
        cols = 2,
        rows = 2,
        tiles = listOf(
            MiniTile(0, 0, CIRCUIT_RIGHT or CIRCUIT_DOWN, lit = false),
            MiniTile(1, 0, CIRCUIT_LEFT or CIRCUIT_DOWN, lit = false),
            MiniTile(0, 1, CIRCUIT_UP or CIRCUIT_RIGHT, lit = false),
            MiniTile(1, 1, CIRCUIT_UP or CIRCUIT_LEFT, lit = false),
        ),
    )
}

@Composable
private fun CircuitMiniPicture(cols: Int, rows: Int, tiles: List<MiniTile>) {
    val palette = rememberCircuitPalette()
    val cell = PICTURE_HEIGHT / rows
    Canvas(Modifier.size(cell * cols, PICTURE_HEIGHT)) {
        val cellPx = size.height / rows
        tiles.forEach { tile ->
            drawCircuitTile(
                mask = tile.mask,
                topLeft = Offset(tile.col * cellPx, tile.row * cellPx),
                cellSize = cellPx,
                lit = tile.lit,
                isSource = tile.source,
                locked = false,
                palette = palette,
            )
        }
    }
}
