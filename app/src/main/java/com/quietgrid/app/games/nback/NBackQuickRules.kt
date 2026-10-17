package com.quietgrid.app.games.nback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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

@Composable
internal fun NBackOneSquarePicture() = NBackMiniGrid(lit = 4, cellSize = 10.dp)

@Composable
internal fun NBackStepsBackPicture() {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        NBackMiniGrid(lit = 0, cellSize = 7.dp)
        NBackMiniGrid(lit = 5, cellSize = 7.dp)
        NBackMiniGrid(lit = 0, cellSize = 7.dp)
    }
}

@Composable
internal fun NBackMatchPicture() {
    Box(
        Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 5.dp),
    ) {
        Text(
            stringResource(R.string.nback_match_button),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 10.sp, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun NBackMiniGrid(lit: Int, cellSize: Dp) {
    Column(verticalArrangement = Arrangement.spacedBy(1.5.dp)) {
        for (row in 0 until 3) {
            Row(horizontalArrangement = Arrangement.spacedBy(1.5.dp)) {
                for (col in 0 until 3) {
                    val position = row * 3 + col
                    Box(
                        Modifier.size(cellSize).background(
                            if (position == lit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(2.dp),
                        ),
                    )
                }
            }
        }
    }
}
