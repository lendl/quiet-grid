package com.quietgrid.app.games.hashi

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PICTURE_WIDTH = 64.dp
private val PICTURE_HEIGHT = 40.dp
private val ISLAND_SIZE = 14.dp

private class MiniIsland(val x: Dp, val y: Dp, val number: Int)
private class MiniBridge(val from: Int, val to: Int, val count: Int = 1, val conflict: Boolean = false)

@Composable
internal fun HashiMatchNumbersPicture() {
    HashiMiniPicture(
        islands = listOf(MiniIsland(10.dp, 20.dp, 2), MiniIsland(54.dp, 20.dp, 2)),
        bridges = listOf(MiniBridge(0, 1, count = 2)),
    )
}

@Composable
internal fun HashiStraightLinesPicture() {
    HashiMiniPicture(
        islands = listOf(MiniIsland(10.dp, 8.dp, 1), MiniIsland(54.dp, 8.dp, 2), MiniIsland(54.dp, 32.dp, 1)),
        bridges = listOf(MiniBridge(0, 1), MiniBridge(1, 2)),
    )
}

@Composable
internal fun HashiNoCrossingsPicture() {
    HashiMiniPicture(
        islands = listOf(
            MiniIsland(32.dp, 7.dp, 1),
            MiniIsland(32.dp, 33.dp, 1),
            MiniIsland(8.dp, 20.dp, 1),
            MiniIsland(56.dp, 20.dp, 1),
        ),
        bridges = listOf(MiniBridge(0, 1), MiniBridge(2, 3, conflict = true)),
    )
}

@Composable
private fun HashiMiniPicture(islands: List<MiniIsland>, bridges: List<MiniBridge>) {
    val bridgeColor = MaterialTheme.colorScheme.onSurface
    val conflictColor = MaterialTheme.colorScheme.error
    Box(Modifier.size(PICTURE_WIDTH, PICTURE_HEIGHT)) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 1.5.dp.toPx()
            val spacing = 2.5.dp.toPx()
            bridges.forEach { bridge ->
                val a = islands[bridge.from]
                val b = islands[bridge.to]
                val start = Offset(a.x.toPx(), a.y.toPx())
                val end = Offset(b.x.toPx(), b.y.toPx())
                val horizontal = a.y == b.y
                val offsets = if (bridge.count == 2) listOf(-spacing, spacing) else listOf(0f)
                offsets.forEach { shift ->
                    val delta = if (horizontal) Offset(0f, shift) else Offset(shift, 0f)
                    drawLine(if (bridge.conflict) conflictColor else bridgeColor, start + delta, end + delta, stroke)
                }
            }
        }
        islands.forEach { island ->
            Box(
                Modifier
                    .offset(island.x - ISLAND_SIZE / 2, island.y - ISLAND_SIZE / 2)
                    .size(ISLAND_SIZE)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    island.number.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, lineHeight = 8.sp, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
