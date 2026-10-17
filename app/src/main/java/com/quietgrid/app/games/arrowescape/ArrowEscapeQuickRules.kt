package com.quietgrid.app.games.arrowescape

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp

private val PICTURE_WIDTH = 64.dp
private val PICTURE_HEIGHT = 32.dp

@Composable
internal fun ArrowEscapeOneDirectionPicture() {
    val arrowColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
    val laneColor = MaterialTheme.colorScheme.primary
    Canvas(Modifier.size(PICTURE_WIDTH, PICTURE_HEIGHT)) {
        val y = size.height / 2
        drawLine(
            laneColor,
            Offset(30.dp.toPx(), y),
            Offset(size.width - 2.dp.toPx(), y),
            1.5.dp.toPx(),
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())),
        )
        drawMiniArrow(arrowColor, Offset(6.dp.toPx(), y), Offset(26.dp.toPx(), y))
    }
}

@Composable
internal fun ArrowEscapeClearWayPicture() {
    val arrowColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
    val blockerColor = MaterialTheme.colorScheme.error
    Canvas(Modifier.size(PICTURE_WIDTH, PICTURE_HEIGHT)) {
        val y = size.height / 2
        drawMiniArrow(arrowColor, Offset(6.dp.toPx(), y), Offset(26.dp.toPx(), y))
        val blockerX = 42.dp.toPx()
        drawMiniArrow(blockerColor, Offset(blockerX, size.height - 4.dp.toPx()), Offset(blockerX, 4.dp.toPx()))
    }
}

@Composable
internal fun ArrowEscapeLivesPicture() {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(3) { index ->
            Icon(
                if (index < 2) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

private fun DrawScope.drawMiniArrow(color: Color, from: Offset, to: Offset) {
    val stroke = 3.dp.toPx()
    drawLine(color, from, to, stroke, cap = StrokeCap.Round)
    val head = 5.dp.toPx()
    val direction = (to - from) / (to - from).getDistance()
    val normal = Offset(-direction.y, direction.x)
    val back = to - direction * head
    drawLine(color, to, back + normal * head, stroke, cap = StrokeCap.Round)
    drawLine(color, to, back - normal * head, stroke, cap = StrokeCap.Round)
}
