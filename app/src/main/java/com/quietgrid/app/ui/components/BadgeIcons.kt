package com.quietgrid.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.CropFree
import androidx.compose.material.icons.outlined.CropSquare
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FilterHdr
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.LooksTwo
import androidx.compose.material.icons.outlined.Looks3
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.SportsScore
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material.icons.outlined.TipsAndUpdates
import androidx.compose.material.icons.outlined.VerticalAlignBottom
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.quietgrid.app.core.badges.BadgeGlyph
import com.quietgrid.app.core.badges.JourneyStage

private val PumpkinIcon: ImageVector by lazy {
    ImageVector.Builder(name = "BadgePumpkin", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.7f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(12f, 7.5f)
            curveTo(6.5f, 7.5f, 3.5f, 10f, 3.5f, 13.75f)
            curveTo(3.5f, 17.5f, 6.5f, 20f, 12f, 20f)
            curveTo(17.5f, 20f, 20.5f, 17.5f, 20.5f, 13.75f)
            curveTo(20.5f, 10f, 17.5f, 7.5f, 12f, 7.5f)
            close()
            moveTo(12f, 7.5f)
            curveTo(9.6f, 9.5f, 9.6f, 18f, 12f, 20f)
            moveTo(12f, 7.5f)
            curveTo(14.4f, 9.5f, 14.4f, 18f, 12f, 20f)
            moveTo(12f, 7.5f)
            lineTo(12f, 5f)
            curveTo(12f, 4f, 12.8f, 3.5f, 14.2f, 3.5f)
        }
        .build()
}

fun badgeGlyphIcon(glyph: BadgeGlyph): ImageVector = when (glyph) {
    BadgeGlyph.TOUR -> Icons.Outlined.Explore
    BadgeGlyph.SUMMITS -> Icons.Outlined.Landscape
    BadgeGlyph.MIX -> Icons.Outlined.Shuffle
    BadgeGlyph.ROADS -> Icons.Outlined.Map
    BadgeGlyph.STARRY_ROAD -> Icons.Outlined.AutoAwesome
    BadgeGlyph.HOURGLASS -> Icons.Outlined.HourglassEmpty
    BadgeGlyph.BOOKMARK -> Icons.Outlined.BookmarkBorder
    BadgeGlyph.PUMPKIN -> PumpkinIcon
    BadgeGlyph.SUMMIT -> Icons.Outlined.Terrain
    BadgeGlyph.CAGE -> Icons.Outlined.CropFree
    BadgeGlyph.SMALL_SUMMIT -> Icons.Outlined.FilterHdr
    BadgeGlyph.PAW -> Icons.Outlined.Pets
    BadgeGlyph.ARROW -> Icons.Outlined.NorthEast
    BadgeGlyph.NIGHT -> Icons.Outlined.Bedtime
    BadgeGlyph.BALANCE -> Icons.Outlined.Balance
    BadgeGlyph.PICTURE -> Icons.Outlined.Image
    BadgeGlyph.SWEEP -> Icons.Outlined.CleaningServices
    BadgeGlyph.FLAG -> Icons.Outlined.Flag
    BadgeGlyph.OPEN_FIELD -> Icons.Outlined.OpenInFull
    BadgeGlyph.HIDDEN_WORD -> Icons.Outlined.Search
    BadgeGlyph.CORNER -> Icons.Outlined.Fullscreen
    BadgeGlyph.EYE -> Icons.Outlined.Visibility
    BadgeGlyph.LAST_LINE -> Icons.Outlined.VerticalAlignBottom
    BadgeGlyph.THREE -> Icons.Outlined.Looks3
    BadgeGlyph.TWO -> Icons.Outlined.LooksTwo
    BadgeGlyph.LOCK_OPEN -> Icons.Outlined.LockOpen
    BadgeGlyph.CLUES -> Icons.Outlined.TipsAndUpdates
    BadgeGlyph.FEW_CLUES -> Icons.Outlined.Lightbulb
    BadgeGlyph.SPARKLE -> Icons.Outlined.WaterDrop
    BadgeGlyph.DEEP -> Icons.Outlined.Layers
    BadgeGlyph.DEEP_SPARKLE -> Icons.Outlined.Diamond
    BadgeGlyph.TWO_LINES -> Icons.Outlined.DragHandle
    BadgeGlyph.THREE_LINES -> Icons.Outlined.Menu
    BadgeGlyph.CLEAN_BOARD -> Icons.Outlined.CropSquare
}

fun journeyStageIcon(stage: JourneyStage): ImageVector = when (stage) {
    JourneyStage.CHAPTER_ONE -> Icons.AutoMirrored.Outlined.MenuBook
    JourneyStage.JOURNEY_END -> Icons.Outlined.SportsScore
    JourneyStage.EVERY_STAR -> Icons.Outlined.StarOutline
}
