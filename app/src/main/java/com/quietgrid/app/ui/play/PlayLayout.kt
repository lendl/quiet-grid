package com.quietgrid.app.ui.play

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp

enum class PlaySizeClass { Compact, Medium, PhoneLandscape, TabletLandscape, TabletPortrait }

private val PHONE_LANDSCAPE_MAX_HEIGHT = 480.dp
private val TABLET_MIN_WIDTH = 600.dp
private val MEDIUM_MIN_HEIGHT = 700.dp
private val WIDE_PHONE_LANDSCAPE_MIN_WIDTH = 700.dp
private val TABLET_PANEL_MAX_WIDTH = 440.dp
private const val TABLET_PANEL_WIDTH_FRACTION = 0.36f

fun isPhoneDevice(smallestScreenWidthDp: Int): Boolean = smallestScreenWidthDp < TABLET_MIN_WIDTH.value

fun playSizeClass(width: Dp, height: Dp): PlaySizeClass = when {
    height < PHONE_LANDSCAPE_MAX_HEIGHT && width > height -> PlaySizeClass.PhoneLandscape
    width >= TABLET_MIN_WIDTH && height >= PHONE_LANDSCAPE_MAX_HEIGHT ->
        if (width > height) PlaySizeClass.TabletLandscape else PlaySizeClass.TabletPortrait
    height < MEDIUM_MIN_HEIGHT -> PlaySizeClass.Compact
    else -> PlaySizeClass.Medium
}

val PlaySizeClass.isUpright: Boolean
    get() = this == PlaySizeClass.Compact || this == PlaySizeClass.Medium || this == PlaySizeClass.TabletPortrait

val PlaySizeClass.isTablet: Boolean
    get() = this == PlaySizeClass.TabletLandscape || this == PlaySizeClass.TabletPortrait

fun uprightBoardHeight(width: Dp, height: Dp, boardAspectRatio: Float, dockHeight: Dp): Dp =
    min(width / boardAspectRatio, height - dockHeight).coerceAtLeast(0.dp)

fun centeredBoardTop(pageHeight: Dp, boardHeight: Dp, minTop: Dp, maxBottom: Dp): Dp {
    val maxTop = (maxBottom - boardHeight).coerceAtLeast(minTop)
    return ((pageHeight - boardHeight) / 2).coerceIn(minTop, maxTop)
}

fun phoneLandscapePanelWidth(width: Dp): Dp = if (width < WIDE_PHONE_LANDSCAPE_MIN_WIDTH) 168.dp else 196.dp

fun tabletPanelWidth(width: Dp): Dp = min(TABLET_PANEL_MAX_WIDTH, width * TABLET_PANEL_WIDTH_FRACTION)

data class PlayMetrics(
    val topBarHeight: Dp,
    val stripHeight: Dp,
    val iconButtonSize: Dp,
    val iconSize: Dp,
    val stripTextSize: TextUnit,
    val timerTextSize: TextUnit,
    val contentPadding: Dp,
    val hintTitleSize: TextUnit = 14.sp,
    val hintBodySize: TextUnit = 13.sp,
)

fun playMetrics(sizeClass: PlaySizeClass): PlayMetrics = when (sizeClass) {
    PlaySizeClass.Compact -> PlayMetrics(52.dp, 32.dp, 44.dp, 22.dp, 12.sp, 14.sp, 8.dp)
    PlaySizeClass.Medium -> PlayMetrics(56.dp, 40.dp, 48.dp, 24.dp, 13.sp, 15.sp, 16.dp)
    PlaySizeClass.PhoneLandscape -> PlayMetrics(0.dp, 24.dp, 44.dp, 22.dp, 12.sp, 13.sp, 8.dp)
    PlaySizeClass.TabletLandscape, PlaySizeClass.TabletPortrait ->
        PlayMetrics(80.dp, 56.dp, 64.dp, 28.dp, 17.sp, 20.sp, 32.dp, hintTitleSize = 20.sp, hintBodySize = 18.sp)
}
