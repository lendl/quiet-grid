package com.quietgrid.app.ui.screens

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal data class GamesGridLayout(
    val columns: Int,
    val maxIconSize: Dp,
    val spacing: Dp,
    val largeLabels: Boolean,
)

private val TWO_COLUMN_BELOW = 300.dp
private val TABLET_FROM = 560.dp
private val TABLET_MIN_CELL = 140.dp
private val TABLET_SPACING = 16.dp
private const val TABLET_MAX_COLUMNS = 8

internal fun gamesGridLayout(contentWidth: Dp): GamesGridLayout = when {
    contentWidth < TWO_COLUMN_BELOW -> GamesGridLayout(columns = 2, maxIconSize = 96.dp, spacing = 10.dp, largeLabels = false)
    contentWidth < TABLET_FROM -> GamesGridLayout(columns = 3, maxIconSize = 76.dp, spacing = 10.dp, largeLabels = false)
    else -> GamesGridLayout(
        columns = ((contentWidth + TABLET_SPACING) / (TABLET_MIN_CELL + TABLET_SPACING)).toInt().coerceIn(3, TABLET_MAX_COLUMNS),
        maxIconSize = 112.dp,
        spacing = TABLET_SPACING,
        largeLabels = true,
    )
}
