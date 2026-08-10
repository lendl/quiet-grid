package com.quietgrid.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
private fun PuzzleBoardContainerBox(
    visible: Boolean,
    playFresh: Boolean,
    zoomable: Boolean,
    onZoomChange: (Boolean) -> Unit,
    resetTrigger: Int,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        if (visible) {
            BoardEntrance(playFresh = playFresh, modifier = Modifier.fillMaxSize()) {
                if (zoomable) {
                    ZoomableBoardSurface(Modifier.fillMaxSize(), onZoomChange = onZoomChange, resetTrigger = resetTrigger) {
                        content()
                    }
                } else {
                    content()
                }
            }
        }
    }
}

@Composable
fun ColumnScope.PuzzleBoardContainer(
    visible: Boolean,
    playFresh: Boolean,
    zoomable: Boolean = true,
    onZoomChange: (Boolean) -> Unit = {},
    resetTrigger: Int = 0,
    content: @Composable () -> Unit,
) {
    PuzzleBoardContainerBox(
        visible = visible,
        playFresh = playFresh,
        zoomable = zoomable,
        onZoomChange = onZoomChange,
        resetTrigger = resetTrigger,
        modifier = Modifier.weight(1f).fillMaxWidth(),
        content = content,
    )
}

@Composable
fun RowScope.PuzzleBoardContainer(
    visible: Boolean,
    playFresh: Boolean,
    zoomable: Boolean = true,
    onZoomChange: (Boolean) -> Unit = {},
    resetTrigger: Int = 0,
    content: @Composable () -> Unit,
) {
    PuzzleBoardContainerBox(
        visible = visible,
        playFresh = playFresh,
        zoomable = zoomable,
        onZoomChange = onZoomChange,
        resetTrigger = resetTrigger,
        modifier = Modifier.weight(1f).fillMaxHeight(),
        content = content,
    )
}
