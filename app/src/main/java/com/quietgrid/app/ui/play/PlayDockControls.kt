package com.quietgrid.app.ui.play

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun PlaySegmentedButtons(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: (Int) -> Boolean = { true },
) {
    Row(modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).padding(3.dp)) {
        labels.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Surface(
                selected = selected,
                onClick = { if (!selected) onSelect(index) },
                enabled = isEnabled(index),
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = RoundedCornerShape(10.dp),
                color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                contentColor = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                shadowElevation = if (selected) 1.dp else 0.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
