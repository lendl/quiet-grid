package com.quietgrid.app.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R

enum class AppTab { GAMES, DAILY, MIXES, STATS }

private data class BottomNavEntry(
    val tab: AppTab,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val BOTTOM_NAV_ENTRIES = listOf(
    BottomNavEntry(AppTab.GAMES, R.string.tab_games, Icons.Filled.Extension, Icons.Outlined.Extension),
    BottomNavEntry(AppTab.DAILY, R.string.tab_daily, Icons.Filled.Today, Icons.Outlined.Today),
    BottomNavEntry(AppTab.MIXES, R.string.tab_mixes, Icons.Filled.Repeat, Icons.Outlined.Repeat),
    BottomNavEntry(AppTab.STATS, R.string.tab_stats, Icons.Filled.Insights, Icons.Outlined.Insights),
)

@Composable
fun BottomNavBar(selectedTab: AppTab, onSelectTab: (AppTab) -> Unit) {
    val context = LocalContext.current
    val reduceMotion = remember { systemAnimationsDisabled(context) }

    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
        BOTTOM_NAV_ENTRIES.forEach { entry ->
            val selected = entry.tab == selectedTab
            NavigationBarItem(
                selected = selected,
                onClick = { onSelectTab(entry.tab) },
                icon = { BottomNavIcon(entry, selected, reduceMotion) },
                label = {
                    Text(stringResource(entry.labelRes), maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

@Composable
private fun BottomNavIcon(entry: BottomNavEntry, selected: Boolean, reduceMotion: Boolean) {
    val scale = remember { Animatable(1f) }
    var firstComposition by remember { mutableStateOf(true) }
    LaunchedEffect(selected, reduceMotion) {
        if (!selected) return@LaunchedEffect
        if (reduceMotion || firstComposition) {
            scale.snapTo(1f)
        } else {
            scale.animateTo(0.8f, tween(80))
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        }
        firstComposition = false
    }
    Crossfade(
        targetState = selected,
        animationSpec = if (reduceMotion) snap() else tween(150),
        label = "bottomNavIconCrossfade",
    ) { isSelected ->
        Icon(
            if (isSelected) entry.selectedIcon else entry.unselectedIcon,
            contentDescription = null,
            modifier = Modifier.size(24.dp).scale(scale.value),
        )
    }
}
