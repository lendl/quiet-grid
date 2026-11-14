package com.quietgrid.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R
import com.quietgrid.app.core.GameCatalog
import com.quietgrid.app.core.badges.BadgeCount
import com.quietgrid.app.core.badges.BadgeDef
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun rememberBadgeDate(date: LocalDate): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(date, locale) { date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)) }
}

@Composable
fun badgeStatusText(earnedOn: LocalDate?, count: BadgeCount?): String = when {
    earnedOn != null -> stringResource(R.string.milestones_earned_on, rememberBadgeDate(earnedOn))
    count != null -> stringResource(R.string.milestones_progress, count.done, count.total)
    else -> stringResource(R.string.milestones_not_yet)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadgeDetailSheet(
    def: BadgeDef,
    earnedOn: LocalDate?,
    count: BadgeCount?,
    onDismiss: () -> Unit,
    dots: List<Boolean>? = null,
) {
    val text = badgeText(def)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BadgeHex(def, earned = earnedOn != null, size = 112.dp, dots = dots)
            def.gameId?.let { gameId ->
                Text(
                    stringResource(GameCatalog.get(gameId).titleRes),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Text(stringResource(text.nameRes), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(
                stringResource(text.descRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                badgeStatusText(earnedOn, count),
                style = MaterialTheme.typography.labelLarge,
                color = if (earnedOn != null) badgeAccent(def) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            if (earnedOn == null && count != null && count.total > 0) {
                BadgeProgressBar(count, badgeAccent(def), Modifier.width(140.dp).height(4.dp))
            }
        }
    }
}

@Composable
fun BadgeProgressBar(count: BadgeCount, color: Color, modifier: Modifier = Modifier) {
    val fraction = if (count.total > 0) (count.done.toFloat() / count.total).coerceIn(0f, 1f) else 0f
    Box(modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(fraction).clip(CircleShape).background(color))
    }
}
