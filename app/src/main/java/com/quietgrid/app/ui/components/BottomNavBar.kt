package com.quietgrid.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.quietgrid.app.R

private data class BottomNavEntry(
    val tab: AppTab,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val BOTTOM_NAV_ENTRIES = listOf(
    BottomNavEntry(AppTab.GAMES, R.string.tab_games, Icons.Filled.Extension, Icons.Outlined.Extension),
    BottomNavEntry(AppTab.STATS, R.string.tab_stats, Icons.Filled.Insights, Icons.Outlined.Insights),
    BottomNavEntry(AppTab.SETTINGS, R.string.tab_settings, Icons.Filled.Settings, Icons.Outlined.Settings),
    BottomNavEntry(AppTab.SUPPORT, R.string.tab_support, Icons.AutoMirrored.Filled.HelpOutline, Icons.AutoMirrored.Outlined.HelpOutline),
)

/**
 * Play Store-style bottom icon nav (icon + label per tab), padded for the system
 * navigation bar/gesture area automatically via [NavigationBar]'s default window insets.
 */
@Composable
fun BottomNavBar(selectedTab: AppTab, onSelectTab: (AppTab) -> Unit) {
    // A subtly different tone than the page background — same idea as the Play Store's bottom
    // bar — so its extent (and the system nav bar area it extends under, edge-to-edge) reads as
    // one distinct band instead of blending into the content above it.
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
        BOTTOM_NAV_ENTRIES.forEach { entry ->
            val selected = entry.tab == selectedTab
            NavigationBarItem(
                selected = selected,
                onClick = { onSelectTab(entry.tab) },
                icon = {
                    Icon(
                        if (selected) entry.selectedIcon else entry.unselectedIcon,
                        contentDescription = null,
                    )
                },
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
