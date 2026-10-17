package com.quietgrid.app.ui.play

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.gameDifficultyLabelRes
import com.quietgrid.app.core.localeFlagEmoji
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.os.ConfigurationCompat
import com.quietgrid.app.ui.components.ChallengerHeartIcon

@Composable
internal fun InfoStrip(items: List<InfoItem>, metrics: PlayMetrics, modifier: Modifier = Modifier, stacked: Boolean = false) {
    val emphasizedIndex = items.indexOfFirst { it is InfoItem.Label && it.emphasized }
    CompositionLocalProvider(
        LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
        LocalTextStyle provides MaterialTheme.typography.bodySmall.copy(fontSize = metrics.stripTextSize),
    ) {
        if (stacked) {
            Column(
                modifier.fillMaxWidth().heightIn(min = metrics.stripHeight),
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            ) {
                items.forEachIndexed { index, item -> InfoItemContent(item, index == emphasizedIndex) }
            }
        } else {
            Row(
                modifier.fillMaxWidth().height(metrics.stripHeight).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEachIndexed { index, item ->
                    if (index > 0) Text(stringResource(R.string.play_info_separator))
                    InfoItemContent(item, index == emphasizedIndex)
                }
            }
        }
    }
}

@Composable
private fun InfoItemContent(item: InfoItem, emphasized: Boolean) {
    when (item) {
        is InfoItem.Label -> Text(
            item.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (emphasized) MaterialTheme.colorScheme.onSurface else Color.Unspecified,
            fontWeight = if (emphasized) FontWeight.SemiBold else null,
        )
        is InfoItem.Custom -> item.content()
    }
}

fun heartsInfo(livesRemaining: Int, startingLives: Int, shakeTrigger: Int = 0): InfoItem = InfoItem.Custom {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(startingLives) { index ->
            ChallengerHeartIcon(
                filled = index < livesRemaining,
                shakeTrigger = if (index == livesRemaining) shakeTrigger else 0,
                tint = LocalContentColor.current,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
fun puzzleLanguageInfo(puzzleLocale: String): InfoItem? {
    val appLanguage = ConfigurationCompat.getLocales(LocalConfiguration.current).get(0)?.language
    return if (puzzleLocale.isNotEmpty() && puzzleLocale != appLanguage) InfoItem.Label(localeFlagEmoji(puzzleLocale)) else null
}

@Composable
internal fun playContextItems(gameId: GameId, difficulty: Difficulty?): List<InfoItem> {
    val context = LocalPlayContext.current
    val dailyKey = context.dailyKey
    val items = mutableListOf<InfoItem>()
    if (dailyKey != null) {
        items += InfoItem.Label(stringResource(R.string.daily_banner, dailyKey))
    }
    if (difficulty != null && (dailyKey != null || context.fromMix)) {
        items += InfoItem.Label(stringResource(gameDifficultyLabelRes(gameId, difficulty)))
    }
    return items
}
