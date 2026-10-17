package com.quietgrid.app.games.animaldoku

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.quietgrid.app.ui.theme.LocalIsDarkTheme

private val PLACEMENT_CELL_SIZE = 16.dp
private val PLACEMENT_CELL_GAP = 2.dp
private val PLACEMENT_CELL_SHAPE = RoundedCornerShape(3.dp)

internal typealias PlacementMarker = @Composable (Modifier) -> Unit

@Composable
private fun placementRegionColor(index: Int): Color {
    val isDark = LocalIsDarkTheme.current
    val palette = if (isDark) ANIMALDOKU_REGION_PALETTE_DARK else ANIMALDOKU_REGION_PALETTE_LIGHT
    return palette[index].copy(alpha = if (isDark) 0.55f else 0.85f)
}

@Composable
internal fun PlacementLinePicture(marker: PlacementMarker) {
    val color = placementRegionColor(0)
    Row(horizontalArrangement = Arrangement.spacedBy(PLACEMENT_CELL_GAP)) {
        for (index in 0 until 4) PlacementCell(color, if (index == 1) marker else null)
    }
}

@Composable
internal fun PlacementRegionPicture(marker: PlacementMarker) {
    val left = placementRegionColor(0)
    val right = placementRegionColor(2)
    Column(verticalArrangement = Arrangement.spacedBy(PLACEMENT_CELL_GAP)) {
        Row(horizontalArrangement = Arrangement.spacedBy(PLACEMENT_CELL_GAP)) {
            PlacementCell(left, marker)
            PlacementCell(left, null)
            PlacementCell(right, null)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(PLACEMENT_CELL_GAP)) {
            PlacementCell(left, null)
            PlacementCell(right, null)
            PlacementCell(right, marker)
        }
    }
}

@Composable
internal fun PlacementNoTouchPicture(marker: PlacementMarker) {
    val color = placementRegionColor(4)
    Column(verticalArrangement = Arrangement.spacedBy(PLACEMENT_CELL_GAP)) {
        Row(horizontalArrangement = Arrangement.spacedBy(PLACEMENT_CELL_GAP)) {
            PlacementCell(color, marker)
            PlacementCell(color, null)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(PLACEMENT_CELL_GAP)) {
            PlacementCell(color, null)
            PlacementCell(color, marker, conflict = true)
        }
    }
}

@Composable
private fun PlacementCell(color: Color, marker: PlacementMarker?, conflict: Boolean = false) {
    val base = Modifier.size(PLACEMENT_CELL_SIZE).background(color, PLACEMENT_CELL_SHAPE)
    Box(
        if (conflict) base.border(1.5.dp, MaterialTheme.colorScheme.error, PLACEMENT_CELL_SHAPE) else base,
        contentAlignment = Alignment.Center,
    ) {
        marker?.invoke(Modifier.fillMaxSize().padding(1.dp))
    }
}

internal val AnimalDokuQuickRuleMarker: PlacementMarker = { modifier ->
    AnimalDokuAnimalIcon(AnimalDokuAnimalPose.FRONT_FACING, region = 0, modifier = modifier)
}
