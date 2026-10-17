package com.quietgrid.app.games.themeclear

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
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
private val MINI_TILE_SHAPE = RoundedCornerShape(3.dp)

@Composable
internal fun ThemeClearAnyOrderPicture() {
    val rows = listOf(listOf('L' to true, 'A' to false, 'O' to true), listOf('T' to false, 'W' to true, 'E' to false))
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                row.forEach { (letter, selected) -> ThemeClearMiniTile(letter, selected) }
            }
        }
    }
}

@Composable
internal fun ThemeClearThemeWordPicture() = ThemeClearMiniChip("OWL", removable = false)

@Composable
internal fun ThemeClearTakeBackPicture() = ThemeClearMiniChip("OWL", removable = true)

@Composable
private fun ThemeClearMiniTile(letter: Char, selected: Boolean) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier.size(MINI_TILE_SIZE).background(if (selected) colors.primary else colors.surfaceVariant, MINI_TILE_SHAPE),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            letter.toString(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, lineHeight = 8.sp, fontWeight = FontWeight.Bold),
            color = if (selected) colors.onPrimary else colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun ThemeClearMiniChip(word: String, removable: Boolean) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.background(colors.secondaryContainer, RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            word,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 10.sp, fontWeight = FontWeight.SemiBold),
            color = colors.onSecondaryContainer,
        )
        if (removable) {
            Text(
                "×",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 11.sp, fontWeight = FontWeight.Bold),
                color = colors.onSecondaryContainer,
            )
        }
    }
}
