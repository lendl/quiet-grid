package com.quietgrid.app.games.sequencememory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val MINI_PAD_SIZE = 16.dp
private val MINI_PAD_GAP = 3.dp
private val MINI_PAD_SHAPE = RoundedCornerShape(4.dp)
private const val IDLE_PAD_ALPHA = 0.3f

@Composable
internal fun SequenceWatchPicture() = SequenceMiniPads(lit = setOf(1), labels = emptyMap())

@Composable
internal fun SequenceRepeatPicture() = SequenceMiniPads(lit = emptySet(), labels = mapOf(2 to "1", 1 to "2"))

@Composable
internal fun SequenceGrowPicture() {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(2) { SequenceDot(SEQUENCE_MEMORY_PAD_COLORS[it]) }
        Text(
            "→",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 9.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        repeat(3) { SequenceDot(SEQUENCE_MEMORY_PAD_COLORS[it]) }
    }
}

@Composable
private fun SequenceMiniPads(lit: Set<Int>, labels: Map<Int, String>) {
    Column(verticalArrangement = Arrangement.spacedBy(MINI_PAD_GAP)) {
        for (row in 0 until 2) {
            Row(horizontalArrangement = Arrangement.spacedBy(MINI_PAD_GAP)) {
                for (col in 0 until 2) {
                    val pad = row * 2 + col
                    val base = SEQUENCE_MEMORY_PAD_COLORS[pad]
                    Box(
                        Modifier.size(MINI_PAD_SIZE).background(if (pad in lit) base else base.copy(alpha = IDLE_PAD_ALPHA), MINI_PAD_SHAPE),
                        contentAlignment = Alignment.Center,
                    ) {
                        labels[pad]?.let { label ->
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, lineHeight = 8.sp, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SequenceDot(color: Color) {
    Box(Modifier.size(8.dp).background(color, CircleShape))
}
