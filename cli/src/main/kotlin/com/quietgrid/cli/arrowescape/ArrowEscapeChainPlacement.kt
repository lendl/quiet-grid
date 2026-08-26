package com.quietgrid.cli.arrowescape

import com.quietgrid.engine.arrowescape.ARROW_DIRECTION_DELTA
import com.quietgrid.engine.arrowescape.ArrowDirection
import com.quietgrid.engine.arrowescape.ArrowEscapePiece
import com.quietgrid.engine.arrowescape.CellCoord
import kotlin.random.Random

private const val MAX_PLACEMENT_ATTEMPTS = 200
const val CHAIN_LINK_LEN = 4

fun placeChain(
    rows: Int,
    cols: Int,
    length: Int,
    occupied: MutableSet<Pair<Int, Int>>,
    random: Random = Random.Default,
): List<ArrowEscapePiece>? {
    repeat(MAX_PLACEMENT_ATTEMPTS) {
        val direction = ArrowDirection.entries[random.nextInt(ArrowDirection.entries.size)]
        val (stepDr, stepDc) = ARROW_DIRECTION_DELTA.getValue(direction)
        val dr = -stepDr
        val dc = -stepDc
        val perpOptions = listOf(-dc to dr, dc to -dr)

        val (anchorRow, anchorCol) = when (direction) {
            ArrowDirection.UP -> 0 to random.nextInt(cols)
            ArrowDirection.DOWN -> (rows - 1) to random.nextInt(cols)
            ArrowDirection.LEFT -> random.nextInt(rows) to 0
            ArrowDirection.RIGHT -> random.nextInt(rows) to (cols - 1)
        }

        val pieces = mutableListOf<ArrowEscapePiece>()
        val claimed = mutableSetOf<Pair<Int, Int>>()
        var r = anchorRow
        var c = anchorCol
        var fits = true

        for (i in 0 until length) {
            val head = r to c
            if (r !in 0 until rows || c !in 0 until cols || head in occupied || head in claimed) {
                fits = false
                break
            }

            var linkCells: List<Pair<Int, Int>>? = null
            for ((perpDr, perpDc) in perpOptions) {
                val segment = mutableListOf<Pair<Int, Int>>()
                var sr = r
                var sc = c
                var ok = true
                for (k in 1 until CHAIN_LINK_LEN) {
                    sr += perpDr
                    sc += perpDc
                    val cell = sr to sc
                    if (sr !in 0 until rows || sc !in 0 until cols || cell in occupied || cell in claimed) {
                        ok = false
                        break
                    }
                    segment.add(cell)
                }
                if (ok) {
                    linkCells = segment.reversed() + head
                    break
                }
            }
            if (linkCells == null) {
                fits = false
                break
            }

            linkCells.forEach { claimed.add(it) }
            pieces.add(ArrowEscapePiece(cells = linkCells.map { (row, col) -> CellCoord(row, col) }, headDirection = direction))
            r += dr
            c += dc
        }
        if (!fits) return@repeat

        claimed.forEach { occupied.add(it) }
        return pieces
    }
    return null
}
