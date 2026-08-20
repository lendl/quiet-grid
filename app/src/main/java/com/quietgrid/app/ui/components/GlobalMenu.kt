package com.quietgrid.app.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R
import com.quietgrid.app.core.REPO_URL
import com.quietgrid.app.data.ThemeMode
import com.quietgrid.app.ui.theme.PlusJakartaSansExtraBold

enum class AppTab { GAMES, STATS, SETTINGS, SUPPORT }

private val THEME_CYCLE = listOf(ThemeMode.DARK, ThemeMode.LIGHT, ThemeMode.PENCIL)

private fun nextThemeMode(current: ThemeMode, systemIsDark: Boolean): ThemeMode {
    val effective = if (current == ThemeMode.SYSTEM) {
        if (systemIsDark) ThemeMode.DARK else ThemeMode.LIGHT
    } else {
        current
    }
    val index = THEME_CYCLE.indexOf(effective)
    return if (index == -1) THEME_CYCLE[0] else THEME_CYCLE[(index + 1) % THEME_CYCLE.size]
}

private fun themeCycleIcon(mode: ThemeMode, systemIsDark: Boolean): ImageVector = when (mode) {
    ThemeMode.DARK -> Icons.Outlined.DarkMode
    ThemeMode.LIGHT -> Icons.Outlined.LightMode
    ThemeMode.PENCIL -> Icons.Outlined.Edit
    ThemeMode.SYSTEM -> if (systemIsDark) Icons.Outlined.DarkMode else Icons.Outlined.LightMode
}

@Composable
fun GlobalMenu(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    hasActiveSession: Boolean,
    onContinueSession: () -> Unit,
    subtitle: String? = null,
    showBottomDivider: Boolean = true,
) {
    val context = LocalContext.current
    val systemIsDark = isSystemInDarkTheme()

    Column(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.03f),
                        MaterialTheme.colorScheme.background.copy(alpha = 0f),
                    ),
                ),
            )
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val cellColor = MaterialTheme.colorScheme.onBackground
            val accentCellColor = MaterialTheme.colorScheme.secondary
            Column(
                modifier = Modifier.size(32.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Box(Modifier.size(14.dp).clip(RoundedCornerShape(4.dp)).background(cellColor))
                    Box(Modifier.size(14.dp).clip(RoundedCornerShape(4.dp)).background(cellColor))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Box(Modifier.size(14.dp).clip(RoundedCornerShape(4.dp)).background(cellColor))
                    Box(Modifier.size(14.dp).clip(RoundedCornerShape(4.dp)).background(accentCellColor))
                }
            }
            val brandTitle = if (subtitle != null) {
                "${stringResource(R.string.app_name)} - $subtitle"
            } else {
                stringResource(R.string.app_name)
            }
            Text(
                brandTitle,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = PlusJakartaSansExtraBold,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 12.dp),
            )
            if (hasActiveSession) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(onClick = onContinueSession),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = stringResource(R.string.common_continue_puzzle),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            IconButton(onClick = {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(REPO_URL))) }
            }) {
                Icon(
                    painter = painterResource(R.drawable.ic_github),
                    contentDescription = stringResource(R.string.home_open_repo),
                )
            }
            IconButton(onClick = { onThemeModeChange(nextThemeMode(themeMode, systemIsDark)) }) {
                Icon(themeCycleIcon(themeMode, systemIsDark), contentDescription = stringResource(R.string.home_change_theme))
            }
        }
        if (showBottomDivider) HorizontalDivider()
    }
}
