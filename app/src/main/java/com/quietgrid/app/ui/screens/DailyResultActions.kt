package com.quietgrid.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R
import com.quietgrid.app.ui.components.OutlinedGlowButton

@Composable
fun DailyResultPrimaryButton(hasNextDaily: Boolean, onPlayNextDaily: () -> Unit, onShareDaily: () -> Unit) {
    OutlinedGlowButton(
        onClick = if (hasNextDaily) onPlayNextDaily else onShareDaily,
        modifier = Modifier.padding(top = 24.dp),
    ) {
        Text(
            stringResource(if (hasNextDaily) R.string.daily_play_next else R.string.daily_share),
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun DailyResultLinks(
    hasNextDaily: Boolean,
    onShareDaily: () -> Unit,
    onBackToDaily: () -> Unit,
    onBackToGames: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (hasNextDaily) {
            DailyResultLink(R.string.daily_share, onShareDaily)
            DailyResultDivider()
        }
        DailyResultLink(R.string.daily_back_to_daily, onBackToDaily)
        DailyResultDivider()
        DailyResultLink(R.string.daily_back_to_games, onBackToGames)
    }
}

@Composable
private fun DailyResultLink(labelRes: Int, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(stringResource(labelRes), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DailyResultDivider() {
    Box(Modifier.width(1.dp).height(16.dp).background(MaterialTheme.colorScheme.outlineVariant))
}
