package com.quietgrid.app.games.themeclear

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.quietgrid.app.ui.components.systemAnimationsDisabled
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val TC_MOVE_MS = 300
private const val TC_POP_MS = 220

@Composable
fun ThemeClearGrid(
    session: ThemeClearSession,
    hintTileIds: List<Int>,
    onTileTap: (Int) -> Unit,
) {
    val allTiles = remember(session.puzzle.id) { themeClearInitialColumns(session.puzzle).flatten() }
    val positions = tcTilePositions(session)
    val selected = session.selection.toSet()
    val hinted = hintTileIds.toSet()
    val animate = !systemAnimationsDisabled(LocalContext.current)

    BoxWithConstraints(contentAlignment = Alignment.Center) {
        val cellSize = min(maxWidth / session.puzzle.cols, maxHeight / session.puzzle.rows)
        Box(Modifier.size(cellSize * session.puzzle.cols, cellSize * session.puzzle.rows).clipToBounds()) {
            allTiles.forEach { tile ->
                key(tile.id) {
                    ThemeClearTile(
                        letter = tile.letter,
                        position = positions[tile.id],
                        cellSize = cellSize,
                        selected = tile.id in selected,
                        hinted = tile.id in hinted,
                        animate = animate,
                        onTap = { onTileTap(tile.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeClearTile(
    letter: Char,
    position: TCTilePosition?,
    cellSize: Dp,
    selected: Boolean,
    hinted: Boolean,
    animate: Boolean,
    onTap: () -> Unit,
) {
    val cellPx = with(LocalDensity.current) { cellSize.toPx() }
    val offsetX = remember { Animatable((position?.col ?: 0) * cellPx) }
    val offsetY = remember { Animatable((position?.row ?: 0) * cellPx) }
    val scale = remember { Animatable(if (position != null) 1f else 0f) }
    var hidden by remember { mutableStateOf(position == null) }

    LaunchedEffect(position, cellPx, animate) {
        if (position == null) {
            hidden = true
            if (animate) scale.animateTo(0f, tween(TC_POP_MS)) else scale.snapTo(0f)
            return@LaunchedEffect
        }
        val targetX = position.col * cellPx
        val targetY = position.row * cellPx
        if (hidden) {
            offsetX.snapTo(targetX)
            offsetY.snapTo(if (animate) -cellPx else targetY)
            scale.snapTo(1f)
            hidden = false
        }
        if (!animate) {
            offsetX.snapTo(targetX)
            offsetY.snapTo(targetY)
            return@LaunchedEffect
        }
        launch { offsetX.animateTo(targetX, tween(TC_MOVE_MS, easing = FastOutSlowInEasing)) }
        offsetY.animateTo(targetY, tween(TC_MOVE_MS, easing = FastOutSlowInEasing))
    }

    if (position == null && scale.value == 0f) return

    val colors = MaterialTheme.colorScheme
    val background = when {
        selected -> colors.primary
        hinted -> colors.tertiaryContainer
        else -> colors.surfaceVariant
    }
    val foreground = when {
        selected -> colors.onPrimary
        hinted -> colors.onTertiaryContainer
        else -> colors.onSurfaceVariant
    }
    Box(
        Modifier
            .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
            .size(cellSize)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .padding(3.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .clickable(enabled = position != null, onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            letter.toString(),
            color = foreground,
            fontWeight = FontWeight.Bold,
            fontSize = (cellSize.value * 0.45f).sp,
        )
    }
}
