package com.quietgrid.app.games.chimptest

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val MINI_TILE_SIZE = 14.dp
private val MINI_TILE_GAP = 3.dp
private val MINI_TILE_SHAPE = RoundedCornerShape(3.dp)

@Composable
internal fun ChimpStudyPicture() {
    val layout = listOf(listOf(null, 3, null), listOf(1, null, 2))
    Column(verticalArrangement = Arrangement.spacedBy(MINI_TILE_GAP)) {
        layout.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(MINI_TILE_GAP)) {
                row.forEach { number -> if (number == null) Spacer(Modifier.size(MINI_TILE_SIZE)) else ChimpMiniTile(number) }
            }
        }
    }
}

@Composable
internal fun ChimpTapOrderPicture() {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        ChimpMiniTile(1)
        ChimpArrow()
        ChimpMiniTile(2)
        ChimpArrow()
        ChimpMiniTile(3)
    }
}

@Composable
internal fun ChimpVanishPicture() {
    Row(horizontalArrangement = Arrangement.spacedBy(MINI_TILE_GAP)) {
        Spacer(Modifier.size(MINI_TILE_SIZE))
        ChimpMiniTile(null)
        ChimpMiniTile(null)
    }
}

@Composable
private fun ChimpArrow() {
    Text(
        "→",
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 9.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ChimpMiniTile(number: Int?) {
    Box(
        Modifier
            .size(MINI_TILE_SIZE)
            .background(MaterialTheme.colorScheme.surfaceVariant, MINI_TILE_SHAPE)
            .border(1.5.dp, MaterialTheme.colorScheme.primary, MINI_TILE_SHAPE),
        contentAlignment = Alignment.Center,
    ) {
        if (number != null) {
            Text(
                number.toString(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, lineHeight = 8.sp, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
