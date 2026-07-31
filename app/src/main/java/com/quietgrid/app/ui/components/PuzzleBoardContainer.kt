package com.quietgrid.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun ColumnScope.PuzzleBoardContainer(
    visible: Boolean,
    playFresh: Boolean,
    zoomable: Boolean = true,
    onZoomChange: (Boolean) -> Unit = {},
    resetTrigger: Int = 0,
    content: @Composable () -> Unit,
) {
    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
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
