package com.quietgrid.app.games.wordsearch

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val WORD_LIST_SIDE_BY_SIDE_MIN_WIDTH = 600.dp

fun shouldShowWordListSideBySide(regionWidth: Dp, regionHeight: Dp): Boolean =
    regionWidth > regionHeight && regionWidth >= WORD_LIST_SIDE_BY_SIDE_MIN_WIDTH
