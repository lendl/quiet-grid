package com.quietgrid.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.quietgrid.app.core.formatElapsed

@Composable
fun ElapsedTimerText(
    elapsedSeconds: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = Color.Unspecified,
) {
    Text(formatElapsed(elapsedSeconds), style = style, color = color, modifier = modifier)
}
