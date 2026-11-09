package com.quietgrid.app.games.wordswap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quietgrid.engine.wordswap.WORDSWAP_HOLE

private val MINI_TILE = 16.dp
private val MINI_GAP = 2.dp

private enum class MiniState { PLAIN, SELECTED, DONE }

@Composable
internal fun WordSwapSwapPicture() {
    Row(horizontalArrangement = Arrangement.spacedBy(MINI_GAP), verticalAlignment = Alignment.CenterVertically) {
        MiniTile('t', MiniState.SELECTED)
        MiniTile('a', MiniState.PLAIN)
        MiniTile('c', MiniState.SELECTED)
        Icon(
            Icons.Outlined.SwapHoriz,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(MINI_TILE),
        )
        MiniTile('c', MiniState.DONE)
        MiniTile('a', MiniState.DONE)
        MiniTile('t', MiniState.DONE)
    }
}

@Composable
internal fun WordSwapCrossingPicture() {
    MiniBoard(listOf("bad", "a.o", "tog"), done = setOf(0, 1, 2, 3, 5, 6, 7, 8))
}

@Composable
internal fun WordSwapFewestPicture() {
    MiniBoard(listOf("bat", "a.o", "dgo"), done = setOf(0, 1, 2))
}

@Composable
private fun MiniBoard(rows: List<String>, done: Set<Int>) {
    Column(verticalArrangement = Arrangement.spacedBy(MINI_GAP)) {
        rows.forEachIndexed { r, line ->
            Row(horizontalArrangement = Arrangement.spacedBy(MINI_GAP)) {
                line.forEachIndexed { c, letter ->
                    if (letter == WORDSWAP_HOLE) {
                        Box(Modifier.size(MINI_TILE))
                    } else {
                        MiniTile(letter, if (r * line.length + c in done) MiniState.DONE else MiniState.PLAIN)
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniTile(letter: Char, state: MiniState) {
    val colors = rememberWordSwapColors()
    val (fill, text) = when (state) {
        MiniState.PLAIN -> colors.tile to colors.onTile
        MiniState.SELECTED -> colors.selected to colors.onSelected
        MiniState.DONE -> colors.done to colors.onDone
    }
    Box(
        Modifier.size(MINI_TILE).background(fill, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(letter.uppercaseChar().toString(), color = text, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}
