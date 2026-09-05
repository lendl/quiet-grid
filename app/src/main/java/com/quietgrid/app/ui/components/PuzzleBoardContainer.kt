package com.quietgrid.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.quietgrid.app.ui.theme.LocalIsDarkTheme
import kotlinx.coroutines.delay

private val BOARD_FRAME_SHAPE = RoundedCornerShape(20.dp)
private val BOARD_FRAME_PADDING = 8.dp
private val BOARD_FRAME_ELEVATION = 4.dp

@Composable
private fun PuzzleBoardContainerBox(
    visible: Boolean,
    playFresh: Boolean,
    zoomable: Boolean,
    onZoomChange: (Boolean) -> Unit,
    resetTrigger: Int,
    panTarget: Offset?,
    onVisibleBoundsChange: (Rect) -> Unit,
    flashTrigger: Int,
    showFrame: Boolean,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    val isDarkTheme = LocalIsDarkTheme.current
    var flashActive by remember { mutableStateOf(false) }
    LaunchedEffect(flashTrigger) {
        if (flashTrigger > 0) {
            flashActive = true
            delay(400)
            flashActive = false
        }
    }
    val errorColor = MaterialTheme.colorScheme.error
    val restColor = MaterialTheme.colorScheme.outline
    val borderColor = if (flashActive) errorColor else restColor
    val frameModifier = if (isDarkTheme) {
        Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), BOARD_FRAME_SHAPE)
            .border(
                if (flashActive) 2.dp else 1.dp,
                borderColor.copy(alpha = if (flashActive) 0.9f else 0.5f),
                BOARD_FRAME_SHAPE,
            )
    } else {
        Modifier
            .shadow(BOARD_FRAME_ELEVATION, BOARD_FRAME_SHAPE, clip = false)
            .background(MaterialTheme.colorScheme.surface, BOARD_FRAME_SHAPE)
            .border(if (flashActive) 3.dp else 2.dp, borderColor, BOARD_FRAME_SHAPE)
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        if (visible) {
            if (zoomable) {
                BoardEntrance(playFresh = playFresh, modifier = Modifier.fillMaxSize()) {
                    var naturalContentSize by remember { mutableStateOf(IntSize.Zero) }
                    val density = LocalDensity.current
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (showFrame && naturalContentSize != IntSize.Zero) {
                            val naturalWidthDp = with(density) { naturalContentSize.width.toDp() }
                            val naturalHeightDp = with(density) { naturalContentSize.height.toDp() }
                            Box(
                                frameModifier.size(
                                    naturalWidthDp + BOARD_FRAME_PADDING * 2,
                                    naturalHeightDp + BOARD_FRAME_PADDING * 2,
                                ),
                            )
                        }
                        ZoomableBoardSurface(
                            Modifier.matchParentSize(),
                            onZoomChange = onZoomChange,
                            resetTrigger = resetTrigger,
                            panTarget = panTarget,
                            onVisibleBoundsChange = onVisibleBoundsChange,
                            onContentSizeChange = { naturalContentSize = it },
                        ) {
                            Box(if (showFrame) Modifier.padding(BOARD_FRAME_PADDING) else Modifier) {
                                content()
                            }
                        }
                    }
                }
            } else {
                BoardEntrance(playFresh = playFresh, modifier = Modifier.wrapContentSize()) {
                    Box(if (showFrame) frameModifier.padding(BOARD_FRAME_PADDING) else Modifier) {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
fun ColumnScope.PuzzleBoardContainer(
    visible: Boolean,
    playFresh: Boolean,
    flashTrigger: Int = 0,
    zoomable: Boolean = true,
    showFrame: Boolean = true,
    onZoomChange: (Boolean) -> Unit = {},
    resetTrigger: Int = 0,
    panTarget: Offset? = null,
    onVisibleBoundsChange: (Rect) -> Unit = {},
    content: @Composable () -> Unit,
) {
    PuzzleBoardContainerBox(
        visible = visible,
        playFresh = playFresh,
        zoomable = zoomable,
        onZoomChange = onZoomChange,
        resetTrigger = resetTrigger,
        panTarget = panTarget,
        onVisibleBoundsChange = onVisibleBoundsChange,
        flashTrigger = flashTrigger,
        showFrame = showFrame,
        modifier = Modifier.weight(1f).fillMaxWidth(),
        content = content,
    )
}

@Composable
fun RowScope.PuzzleBoardContainer(
    visible: Boolean,
    playFresh: Boolean,
    flashTrigger: Int = 0,
    zoomable: Boolean = true,
    showFrame: Boolean = true,
    onZoomChange: (Boolean) -> Unit = {},
    resetTrigger: Int = 0,
    panTarget: Offset? = null,
    onVisibleBoundsChange: (Rect) -> Unit = {},
    content: @Composable () -> Unit,
) {
    PuzzleBoardContainerBox(
        visible = visible,
        playFresh = playFresh,
        zoomable = zoomable,
        onZoomChange = onZoomChange,
        resetTrigger = resetTrigger,
        panTarget = panTarget,
        onVisibleBoundsChange = onVisibleBoundsChange,
        flashTrigger = flashTrigger,
        showFrame = showFrame,
        modifier = Modifier.weight(1f).fillMaxHeight(),
        content = content,
    )
}
