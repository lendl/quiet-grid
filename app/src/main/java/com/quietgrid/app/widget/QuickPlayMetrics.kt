package com.quietgrid.app.widget

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp

val QUICK_PLAY_PADDING = 8.dp
val QUICK_PLAY_GAP = 8.dp

data class QuickPlayGrid(val vertical: Boolean, val cell: DpSize)

data class QuickPlaySplit(val vertical: Boolean, val main: DpSize, val side: DpSize)

data class QuickPlayTileMetrics(val horizontal: Boolean, val maxIconSize: Dp, val titleSize: TextUnit, val detailSize: TextUnit)

data class QuickPlayButtonMetrics(val horizontal: Boolean, val iconSize: Dp, val titleSize: TextUnit, val labelSize: TextUnit)

private const val MAIN_SHARE = 2f / 3f
private const val TILE_HORIZONTAL_ASPECT = 1.3f
private const val BUTTON_HORIZONTAL_ASPECT = 1.2f
private const val TILE_TITLE_FRACTION = 0.15f
private const val DETAIL_RATIO = 0.85f
private const val LABEL_RATIO = 0.6f
private const val MIN_TITLE_SP = 13f
private const val MAX_TILE_TITLE_SP = 30f
private const val MAX_BUTTON_TITLE_SP = 40f
private const val MIN_LABEL_SP = 12f

const val CONTINUE_TITLE_FRACTION = 0.17f
const val MIX_TITLE_FRACTION = 0.19f

fun quickPlayGrid(content: DpSize, count: Int): QuickPlayGrid {
    val vertical = count > 1 && content.height > content.width
    val gaps = QUICK_PLAY_GAP * (count - 1).coerceAtLeast(0)
    val cell = if (vertical) {
        DpSize(content.width, (content.height - gaps) / count)
    } else {
        DpSize((content.width - gaps) / count, content.height)
    }
    return QuickPlayGrid(vertical, cell)
}

fun quickPlaySplit(content: DpSize): QuickPlaySplit = if (content.height > content.width) {
    val free = content.height - QUICK_PLAY_GAP
    QuickPlaySplit(true, DpSize(content.width, free * MAIN_SHARE), DpSize(content.width, free * (1 - MAIN_SHARE)))
} else {
    val free = content.width - QUICK_PLAY_GAP
    QuickPlaySplit(false, DpSize(free * MAIN_SHARE, content.height), DpSize(free * (1 - MAIN_SHARE), content.height))
}

fun quickPlayTileMetrics(cell: DpSize): QuickPlayTileMetrics {
    val horizontal = cell.width > cell.height * TILE_HORIZONTAL_ASPECT
    val title = (min(cell.width, cell.height).value * TILE_TITLE_FRACTION).coerceIn(MIN_TITLE_SP, MAX_TILE_TITLE_SP)
    val maxIcon = if (horizontal) min(cell.height * 0.66f, cell.width * 0.34f) else min(cell.width * 0.66f, cell.height * 0.56f)
    return QuickPlayTileMetrics(horizontal, maxIcon, title.sp, (title * DETAIL_RATIO).sp)
}

fun quickPlayButtonMetrics(button: DpSize, titleFraction: Float): QuickPlayButtonMetrics {
    val horizontal = button.width > button.height * BUTTON_HORIZONTAL_ASPECT
    val basis = if (horizontal) min(button.height, button.width * 0.6f) else min(button.width, button.height * 0.6f)
    val title = (basis.value * titleFraction).coerceIn(MIN_TITLE_SP, MAX_BUTTON_TITLE_SP)
    val icon = if (horizontal) min(button.height * 0.4f, button.width * 0.3f) else min(button.width * 0.42f, button.height * 0.34f)
    return QuickPlayButtonMetrics(horizontal, icon, title.sp, (title * LABEL_RATIO).coerceAtLeast(MIN_LABEL_SP).sp)
}
