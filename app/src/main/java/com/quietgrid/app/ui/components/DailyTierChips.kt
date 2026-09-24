package com.quietgrid.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.daily.DailyTierStatus
import com.quietgrid.app.core.daily.DailyTierUi
import com.quietgrid.app.core.formatElapsed
import com.quietgrid.app.core.gameDifficultyLabelRes

@Composable
fun DailyTierChips(gameId: GameId, tiers: List<DailyTierUi>, onTierClick: (DailyTierUi) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tiers.forEach { tier ->
            val label = stringResource(gameDifficultyLabelRes(gameId, tier.difficulty))
            val status = tier.status
            AssistChip(
                onClick = { onTierClick(tier) },
                label = {
                    Text(if (status is DailyTierStatus.Solved) "$label ${formatElapsed(status.elapsedSeconds)}" else label)
                },
                leadingIcon = when (status) {
                    is DailyTierStatus.Solved -> ({ Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) })
                    DailyTierStatus.Lost -> ({ Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(18.dp)) })
                    DailyTierStatus.InProgress -> ({ Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp)) })
                    DailyTierStatus.Unplayed -> null
                },
            )
        }
    }
}
