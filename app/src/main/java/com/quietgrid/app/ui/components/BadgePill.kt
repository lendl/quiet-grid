package com.quietgrid.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.quietgrid.app.ui.theme.LocalIsDarkTheme

@Composable
fun BadgePill(emoji: String, text: String, borderColor: Color, textColor: Color, modifier: Modifier = Modifier) {
    val isDarkTheme = LocalIsDarkTheme.current
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = if (isDarkTheme) 6.dp else 0.dp,
        shadowElevation = if (isDarkTheme) 3.dp else 0.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(emoji, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            Text(text, style = MaterialTheme.typography.labelMedium, color = textColor, maxLines = 1)
        }
    }
}
