package com.quietgrid.app.ui.analyzer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.quietgrid.app.R

@Composable
fun AnalyzerNavigationControls(
    currentIndex: Int,
    totalSteps: Int,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, enabled = currentIndex > 0) {
            Icon(Icons.Filled.SkipPrevious, contentDescription = stringResource(R.string.analyzer_back))
        }
        Text(
            stringResource(R.string.analyzer_step_of, currentIndex + 1, totalSteps),
            style = MaterialTheme.typography.labelLarge,
        )
        IconButton(onClick = onTogglePlay) {
            Icon(
                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = stringResource(if (isPlaying) R.string.analyzer_pause else R.string.analyzer_play),
            )
        }
        IconButton(onClick = onNext, enabled = currentIndex < totalSteps - 1) {
            Icon(Icons.Filled.SkipNext, contentDescription = stringResource(R.string.analyzer_next))
        }
    }
}
