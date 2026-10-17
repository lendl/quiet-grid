package com.quietgrid.app.games.nonogram

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quietgrid.app.R
import com.quietgrid.app.ui.play.PlaySegmentedButtons
import com.quietgrid.app.ui.play.PlaySizeClass
import com.quietgrid.app.ui.play.isTablet

private val MINI_CELL_SIZE = 10.dp
private val MINI_CELL_SHAPE = RoundedCornerShape(2.dp)

internal fun nonogramDockHeight(sizeClass: PlaySizeClass): Dp = if (sizeClass.isTablet) 72.dp else 56.dp

@Composable
internal fun NonogramModeDock(sizeClass: PlaySizeClass, mode: NonogramInputMode, onModeChange: (NonogramInputMode) -> Unit) {
    Box(Modifier.fillMaxWidth().padding(top = 6.dp), contentAlignment = Alignment.TopCenter) {
        PlaySegmentedButtons(
            labels = listOf(stringResource(R.string.nonogram_input_mode_fill), stringResource(R.string.nonogram_input_mode_cross)),
            selectedIndex = if (mode == NonogramInputMode.CROSS) 1 else 0,
            onSelect = { index -> onModeChange(if (index == 1) NonogramInputMode.CROSS else NonogramInputMode.FILL) },
            modifier = Modifier.widthIn(max = 320.dp).fillMaxWidth().height(if (sizeClass.isTablet) 60.dp else 44.dp),
        )
    }
}

@Composable
internal fun NonogramCluePicture(clue: String, cells: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            clue,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 9.sp, fontWeight = FontWeight.Black),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 2.dp),
        )
        cells.forEach { cell -> NonogramMiniCell(cell) }
    }
}

@Composable
private fun NonogramMiniCell(cell: Char) {
    val filled = cell == '#'
    Box(
        Modifier
            .size(MINI_CELL_SIZE)
            .background(if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface, MINI_CELL_SHAPE)
            .border(1.dp, if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, MINI_CELL_SHAPE),
        contentAlignment = Alignment.Center,
    ) {
        if (cell == 'x') {
            Text(
                "×",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, lineHeight = 8.sp, fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
