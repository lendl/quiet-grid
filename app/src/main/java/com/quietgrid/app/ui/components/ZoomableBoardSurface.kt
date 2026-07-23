package com.quietgrid.app.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
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

// Extra pan range allowed beyond the board's own edge once zoomed in, so panning to an edge
// still leaves room to navigate up to it instead of stopping flush against the board. This is
// added purely to the pan clamp — it does NOT take layout space away from the board itself, so
// the board renders at the same size as before; it only affects how far you can pan once zoomed.
private val PAN_MARGIN = 96.dp

/**
 * Wraps board content with two-finger pinch-to-zoom, always, plus single-finger pan once zoomed
 * in — matching the RN app's behavior. Single-finger touches are only claimed for panning once
 * the content is actually zoomed in (and only once the drag has moved past touch-slop, so a tap
 * still reaches the board's own tap handling underneath); at rest they pass through untouched.
 * Content is clipped to the viewport so zoomed-in content never draws outside its own bounds, and
 * [resetTrigger] can be bumped (e.g. from a "reset zoom" button) to snap back to the un-zoomed view.
 */
@Composable
fun ZoomableBoardSurface(
    modifier: Modifier = Modifier,
    onZoomChange: (Boolean) -> Unit = {},
    resetTrigger: Int = 0,
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
