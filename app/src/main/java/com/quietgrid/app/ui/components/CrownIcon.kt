package com.quietgrid.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

@Composable
fun CrownIcon(modifier: Modifier = Modifier, tint: Color) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val crown = Path().apply {
            moveTo(0f, h * 0.78f)
            lineTo(0f, h * 0.22f)
            lineTo(w * 0.3f, h * 0.5f)
            lineTo(w * 0.5f, h * 0.05f)
            lineTo(w * 0.7f, h * 0.5f)
            lineTo(w, h * 0.22f)
            lineTo(w, h * 0.78f)
            close()
        }
        drawPath(crown, tint)
        drawRect(tint, topLeft = Offset(0f, h * 0.84f), size = Size(w, h * 0.16f))
    }
}
