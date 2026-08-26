package com.quietgrid.app.ui.components

import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.max

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 3f

private val PAN_MARGIN = 96.dp

@Composable
fun ZoomableBoardSurface(
    modifier: Modifier = Modifier,
    onZoomChange: (Boolean) -> Unit = {},
    resetTrigger: Int = 0,
    panTarget: Offset? = null,
    onVisibleBoundsChange: (Rect) -> Unit = {},
    content: @Composable () -> Unit,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var contentSize by remember { mutableStateOf(IntSize.Zero) }
    val panMarginPx = with(LocalDensity.current) { PAN_MARGIN.toPx() }

    LaunchedEffect(resetTrigger) {
        if (resetTrigger != 0) {
            scale = 1f
            offset = Offset.Zero
            onZoomChange(false)
        }
    }

    fun clampOffset(candidate: Offset, currentScale: Float): Offset {
        val margin = panMarginPx * currentScale
        val maxX = max(0f, (contentSize.width * currentScale - viewportSize.width) / 2f) + margin
        val maxY = max(0f, (contentSize.height * currentScale - viewportSize.height) / 2f) + margin
        return Offset(candidate.x.coerceIn(-maxX, maxX), candidate.y.coerceIn(-maxY, maxY))
    }

    SideEffect {
        if (contentSize != IntSize.Zero && viewportSize != IntSize.Zero) {
            val contentCenter = Offset(contentSize.width / 2f, contentSize.height / 2f)
            val visibleCenter = contentCenter - offset / scale
            val halfWidth = viewportSize.width / (2f * scale)
            val halfHeight = viewportSize.height / (2f * scale)
            onVisibleBoundsChange(
                Rect(
                    visibleCenter.x - halfWidth,
                    visibleCenter.y - halfHeight,
                    visibleCenter.x + halfWidth,
                    visibleCenter.y + halfHeight,
                ),
            )
        }
    }

    LaunchedEffect(panTarget) {
        val target = panTarget
        if (target != null && scale > MIN_SCALE) {
            val contentCenter = Offset(contentSize.width / 2f, contentSize.height / 2f)
            val desired = (contentCenter - target) * scale
            val clamped = clampOffset(desired, scale)
            animate(
                typeConverter = Offset.VectorConverter,
                initialValue = offset,
                targetValue = clamped,
                animationSpec = tween(500),
            ) { value, _ -> offset = value }
        }
    }

    Box(
        modifier
            .clipToBounds()
            .onSizeChanged { viewportSize = it }
            .pointerInput(Unit) {
                val touchSlop = viewConfiguration.touchSlop
                awaitEachGesture {
                    var panClaimed = false
                    var dragDistance = 0f
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val pressed = event.changes.filter { it.pressed }
                        when {
                            pressed.size >= 2 -> {
                                val zoomChange = event.calculateZoom()
                                val panChange = event.calculatePan()
                                val newScale = (scale * zoomChange).coerceIn(MIN_SCALE, MAX_SCALE)
                                scale = newScale
                                offset = if (newScale > MIN_SCALE) clampOffset(offset + panChange, newScale) else Offset.Zero
                                onZoomChange(newScale > MIN_SCALE)
                                event.changes.forEach { it.consume() }
                            }
                            pressed.size == 1 && scale > MIN_SCALE -> {
                                val change = pressed[0]
                                if (!panClaimed) {
                                    dragDistance += change.positionChange().getDistance()
                                    if (dragDistance > touchSlop) panClaimed = true
                                }
                                if (panClaimed) {
                                    offset = clampOffset(offset + change.positionChange(), scale)
                                    change.consume()
                                }
                            }
                        }
                        if (event.changes.all { !it.pressed }) break
                    }
                }
            }
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offset.x,
                translationY = offset.y,
            ),
    ) {
        Box(Modifier.onSizeChanged { contentSize = it }) {
            content()
        }
    }
}
