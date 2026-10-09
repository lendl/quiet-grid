package com.quietgrid.app.widget

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp

data class QuickPlayMetrics(val vertical: Boolean, val iconSize: Dp, val textSize: TextUnit)

private const val ICON_FRACTION = 0.5f
private const val TEXT_FRACTION = 0.18f
private const val MIN_TEXT_SP = 16f
private const val MAX_TEXT_SP = 48f

fun quickPlayMetrics(width: Dp, height: Dp, buttonCount: Int): QuickPlayMetrics {
    val vertical = buttonCount > 1 && height > width
    val cellWidth = if (vertical) width else width / buttonCount
    val cellHeight = if (vertical) height / buttonCount else height
    val short = min(cellWidth, cellHeight)
    return QuickPlayMetrics(
        vertical = vertical,
        iconSize = short * ICON_FRACTION,
        textSize = (short.value * TEXT_FRACTION).coerceIn(MIN_TEXT_SP, MAX_TEXT_SP).sp,
    )
}
