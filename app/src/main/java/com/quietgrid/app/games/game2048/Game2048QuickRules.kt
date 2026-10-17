package com.quietgrid.app.games.game2048

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import com.quietgrid.app.ui.theme.LocalIsPencilTheme

private val MINI_TILE_SIZE = 14.dp
private val MINI_TILE_SHAPE = RoundedCornerShape(3.dp)

@Composable
internal fun Game2048SlidePicture() = Game2048MiniRow(listOf(2, 4, null, null))

@Composable
internal fun Game2048MergePicture() = Game2048MiniRow(listOf(2, 2), result = listOf(4))

@Composable
internal fun Game2048OneMergePicture() = Game2048MiniRow(listOf(2, 2, 4), result = listOf(4, 4))

@Composable
private fun Game2048MiniRow(tiles: List<Int?>, result: List<Int?>? = null) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        tiles.forEach { Game2048MiniTile(it) }
        if (result != null) {
            Text(
                "→",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            result.forEach { Game2048MiniTile(it) }
        }
    }
}

@Composable
private fun Game2048MiniTile(value: Int?) {
    val pencil = LocalIsPencilTheme.current
    Box(
        Modifier
            .size(MINI_TILE_SIZE)
            .background(if (value == null) MaterialTheme.colorScheme.surfaceVariant else tileColor(value, pencil), MINI_TILE_SHAPE),
        contentAlignment = Alignment.Center,
    ) {
        if (value != null) {
            Text(
                value.toString(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, lineHeight = 8.sp, fontWeight = FontWeight.Bold),
                color = tileTextColor(value, pencil),
            )
        }
    }
}
