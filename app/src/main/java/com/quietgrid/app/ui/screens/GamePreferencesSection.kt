package com.quietgrid.app.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.quietgrid.app.R
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.HINT_CAPABLE_GAMES
import com.quietgrid.app.core.MISTAKE_CHECK_GAMES
import com.quietgrid.app.core.PHONE_PORTRAIT_ONLY_GAMES
import com.quietgrid.app.core.ZOOMABLE_GAMES
import com.quietgrid.app.data.AppSettings
import com.quietgrid.app.data.GameToggle
import com.quietgrid.app.data.RepositoriesViewModel
import com.quietgrid.app.data.ScreenRotation
import com.quietgrid.app.data.toggleDefault
import com.quietgrid.app.ui.play.isPhoneDevice
import kotlinx.coroutines.launch

@get:StringRes
internal val ScreenRotation.labelRes: Int
    get() = when (this) {
        ScreenRotation.AUTO -> R.string.settings_screen_rotation_auto
        ScreenRotation.PORTRAIT -> R.string.settings_screen_rotation_portrait
        ScreenRotation.LANDSCAPE -> R.string.settings_screen_rotation_landscape
    }

@get:StringRes
internal val GameToggle.labelRes: Int
    get() = when (this) {
        GameToggle.TIMER -> R.string.settings_show_timer_in_play_label
        GameToggle.HINTS -> R.string.settings_show_hints_label
        GameToggle.KEEP_SCREEN_ON -> R.string.settings_keep_screen_on_label
        GameToggle.HAPTICS -> R.string.settings_haptics_label
        GameToggle.MISTAKES -> R.string.settings_show_mistakes_label
        GameToggle.REDUCE_MOTION -> R.string.settings_reduce_motion_label
        GameToggle.LEFT_HANDED -> R.string.settings_left_handed_label
        GameToggle.PINCH_ZOOM -> R.string.settings_pinch_zoom_label
        GameToggle.CONFIRM_END -> R.string.settings_confirm_end_label
        GameToggle.REMEMBER_DIFFICULTY -> R.string.settings_remember_difficulty_label
    }

@get:StringRes
internal val GameToggle.detailRes: Int
    get() = when (this) {
        GameToggle.TIMER -> R.string.settings_show_timer_in_play_detail
        GameToggle.HINTS -> R.string.settings_show_hints_detail
        GameToggle.KEEP_SCREEN_ON -> R.string.settings_keep_screen_on_detail
        GameToggle.HAPTICS -> R.string.settings_haptics_detail
        GameToggle.MISTAKES -> R.string.settings_show_mistakes_detail
        GameToggle.REDUCE_MOTION -> R.string.settings_reduce_motion_detail
        GameToggle.LEFT_HANDED -> R.string.settings_left_handed_detail
        GameToggle.PINCH_ZOOM -> R.string.settings_pinch_zoom_detail
        GameToggle.CONFIRM_END -> R.string.settings_confirm_end_detail
        GameToggle.REMEMBER_DIFFICULTY -> R.string.settings_remember_difficulty_detail
    }

private fun GameToggle.appliesTo(gameId: GameId): Boolean = when (this) {
    GameToggle.HINTS -> gameId in HINT_CAPABLE_GAMES
    GameToggle.MISTAKES -> gameId in MISTAKE_CHECK_GAMES
    GameToggle.PINCH_ZOOM -> gameId in ZOOMABLE_GAMES
    else -> true
}

private fun togglesFor(gameId: GameId): List<GameToggle> = GameToggle.entries.filter { it.appliesTo(gameId) }

private data class PreferenceOption(val label: String, val selected: Boolean, val onSelect: () -> Unit)

@Composable
fun GamePreferencesSection(gameId: GameId, modifier: Modifier = Modifier) {
    val repositories: RepositoriesViewModel = hiltViewModel()
    val scope = rememberCoroutineScope()
    val settings by repositories.settingsRepository.settings.collectAsState(initial = AppSettings())
    val overridesFlow = remember(gameId) { repositories.gamePreferencesRepository.overrides(gameId) }
    val overrides by overridesFlow.collectAsState(initial = null)
    val current = overrides ?: return
    val gamePreferences = repositories.gamePreferencesRepository
    val onLabel = stringResource(R.string.game_settings_play_on)
    val offLabel = stringResource(R.string.game_settings_play_off)
    val portraitOnlyOnPhone = gameId in PHONE_PORTRAIT_ONLY_GAMES &&
        isPhoneDevice(LocalConfiguration.current.smallestScreenWidthDp)

    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.game_settings_play_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = { scope.launch { gamePreferences.clear(gameId) } },
                enabled = !current.isEmpty,
            ) {
                Text(stringResource(R.string.game_settings_play_reset))
            }
        }
        Text(
            stringResource(R.string.game_settings_play_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        togglesFor(gameId).forEach { toggle ->
            val override = current.toggles[toggle]
            val defaultLabel = stringResource(
                R.string.game_settings_play_default,
                if (settings.toggleDefault(toggle)) onLabel else offLabel,
            )
            fun select(value: Boolean?) {
                scope.launch { gamePreferences.setToggle(gameId, toggle, value) }
            }
            PreferenceChoiceRow(
                label = stringResource(toggle.labelRes),
                detail = stringResource(toggle.detailRes),
                value = when (override) {
                    null -> defaultLabel
                    true -> onLabel
                    false -> offLabel
                },
                options = listOf(
                    PreferenceOption(defaultLabel, override == null) { select(null) },
                    PreferenceOption(onLabel, override == true) { select(true) },
                    PreferenceOption(offLabel, override == false) { select(false) },
                ),
            )
        }

        val rotationDefaultLabel = stringResource(
            R.string.game_settings_play_default,
            stringResource(settings.screenRotation.labelRes),
        )
        val rotationLabels = ScreenRotation.entries.associateWith { stringResource(it.labelRes) }
        PreferenceChoiceRow(
            label = stringResource(R.string.settings_screen_rotation_label),
            detail = if (portraitOnlyOnPhone) stringResource(R.string.game_settings_play_portrait_only) else null,
            enabled = !portraitOnlyOnPhone,
            value = current.rotation?.let { rotationLabels.getValue(it) } ?: rotationDefaultLabel,
            options = listOf(
                PreferenceOption(rotationDefaultLabel, current.rotation == null) {
                    scope.launch { gamePreferences.setRotation(gameId, null) }
                },
            ) + ScreenRotation.entries.map { rotation ->
                PreferenceOption(rotationLabels.getValue(rotation), current.rotation == rotation) {
                    scope.launch { gamePreferences.setRotation(gameId, rotation) }
                }
            },
        )
    }
}

@Composable
private fun PreferenceChoiceRow(
    label: String,
    value: String,
    options: List<PreferenceOption>,
    detail: String? = null,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { expanded = true }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyLarge)
                if (detail != null) {
                    Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (enabled) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    Icon(Icons.Filled.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    trailingIcon = {
                        if (option.selected) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    onClick = {
                        expanded = false
                        option.onSelect()
                    },
                )
            }
        }
    }
}
