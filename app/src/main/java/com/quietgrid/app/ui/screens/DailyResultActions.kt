package com.quietgrid.app.ui.screens

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.quietgrid.app.R
import com.quietgrid.app.ui.components.OutlinedGlowButton

@Composable
fun DailyResultPrimaryButton(
    hasNextDaily: Boolean,
    onPlayNextDaily: () -> Unit,
    onShareDaily: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedGlowButton(
        onClick = if (hasNextDaily) onPlayNextDaily else onShareDaily,
        modifier = modifier,
    ) {
        Text(
            stringResource(if (hasNextDaily) R.string.daily_play_next else R.string.daily_share),
            fontWeight = FontWeight.Bold,
        )
    }
}

fun dailyResultLinks(
    hasNextDaily: Boolean,
    onShareDaily: () -> Unit,
    onBackToDaily: () -> Unit,
    onBackToGames: () -> Unit,
): List<ResultLink> = buildList {
    if (hasNextDaily) add(ResultLink(R.string.daily_share, onShareDaily))
    add(ResultLink(R.string.daily_back_to_daily, onBackToDaily))
    add(ResultLink(R.string.daily_back_to_games, onBackToGames))
}
