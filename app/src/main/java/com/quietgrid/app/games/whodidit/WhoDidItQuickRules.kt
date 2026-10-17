package com.quietgrid.app.games.whodidit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp

private val MINI_CELL_SIZE = 11.dp

@Composable
internal fun WhoDidItOneMatchPicture() {
    val matches = setOf(0 to 1, 1 to 2, 2 to 0)
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        for (row in 0 until 3) {
            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                for (col in 0 until 3) WhoDidItMiniCell(matched = (row to col) in matches)
            }
        }
    }
}

@Composable
internal fun WhoDidItCluesPicture() {
    Icon(Icons.Outlined.Description, contentDescription = null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun WhoDidItCulpritPicture() {
    Icon(Icons.Outlined.PersonSearch, contentDescription = null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun WhoDidItMiniCell(matched: Boolean) {
    val markColor = MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        Modifier.size(MINI_CELL_SIZE).border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(1.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (matched) {
            Box(Modifier.size(5.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
        } else {
            Canvas(Modifier.fillMaxSize().padding(3.dp)) {
                val stroke = 1.dp.toPx()
                drawLine(markColor, Offset.Zero, Offset(size.width, size.height), stroke)
                drawLine(markColor, Offset(size.width, 0f), Offset(0f, size.height), stroke)
            }
        }
    }
}
