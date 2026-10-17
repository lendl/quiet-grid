package com.quietgrid.app.games.takuzu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quietgrid.app.R

private val MINI_CELL_SIZE = 18.dp
private val MINI_CELL_GAP = 3.dp
private val MINI_CELL_SHAPE = RoundedCornerShape(4.dp)

@Composable
internal fun TakuzuNoThreeInARowPicture() {
    Row(horizontalArrangement = Arrangement.spacedBy(MINI_CELL_GAP)) {
        MiniCell(1)
        MiniCell(1)
        MiniCell(1, conflict = true)
    }
}

@Composable
internal fun TakuzuEqualHalvesPicture() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(MINI_CELL_GAP)) {
        MiniLine(listOf(0, 1, 1, 0))
        Text(
            stringResource(R.string.takuzu_rules_equal_halves_caption),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun TakuzuUniqueLinesPicture() {
    Column(verticalArrangement = Arrangement.spacedBy(MINI_CELL_GAP)) {
        MiniLine(listOf(0, 1, 1, 0))
        MiniLine(listOf(1, 0, 0, 1))
    }
}

@Composable
private fun MiniLine(values: List<Int>) {
    Row(horizontalArrangement = Arrangement.spacedBy(MINI_CELL_GAP)) {
        values.forEach { MiniCell(it) }
    }
}

@Composable
private fun MiniCell(value: Int, conflict: Boolean = false) {
    val error = MaterialTheme.colorScheme.error
    val cellModifier = if (conflict) {
        Modifier.border(1.5.dp, error, MINI_CELL_SHAPE)
    } else {
        Modifier.background(MaterialTheme.colorScheme.surfaceVariant, MINI_CELL_SHAPE)
    }
    Box(Modifier.size(MINI_CELL_SIZE).then(cellModifier), contentAlignment = Alignment.Center) {
        Text(
            value.toString(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 11.sp, fontWeight = FontWeight.Bold),
            color = if (conflict) error else MaterialTheme.colorScheme.onSurface,
        )
    }
}
