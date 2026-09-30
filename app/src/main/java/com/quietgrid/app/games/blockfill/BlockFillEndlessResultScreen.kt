package com.quietgrid.app.games.blockfill

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R
import com.quietgrid.app.ui.components.CrownIcon

private const val RESULT_FADE_MS = 300

@Composable
fun BlockFillEndlessResultScreen(
    result: BlockFillEndlessResult,
    onPlayAgain: () -> Unit,
    onBack: () -> Unit,
) {
    val fade = remember { Animatable(0f) }
    LaunchedEffect(Unit) { fade.animateTo(1f, tween(RESULT_FADE_MS)) }
    val titleRes = if (result.reason == BLOCKFILL_ENDLESS_REASON_STUCK) {
        R.string.blockfill_endless_result_stuck_title
    } else {
        R.string.blockfill_endless_result_abandoned_title
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp)
            .graphicsLayer { alpha = fade.value },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(titleRes), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        if (result.isNewBest) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CrownIcon(Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Text(
                    stringResource(R.string.blockfill_endless_result_new_best),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Text(result.score.toString(), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        ResultRow(stringResource(R.string.blockfill_endless_result_level), stringResource(R.string.blockfill_multiplier_value, result.levelReached))
        ResultRow(stringResource(R.string.blockfill_endless_result_lines), result.linesCleared.toString())
        ResultRow(stringResource(R.string.blockfill_endless_result_moves), result.moves.toString())
        if (!result.isNewBest && result.previousBest > 0) {
            ResultRow(stringResource(R.string.blockfill_endless_stats_best_score), result.previousBest.toString())
        }
        Spacer(Modifier.height(32.dp))
        Button(onClick = onPlayAgain, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.blockfill_endless_play_again))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.blockfill_endless_back))
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}
