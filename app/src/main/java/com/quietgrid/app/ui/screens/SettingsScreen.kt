package com.quietgrid.app.ui.screens

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.quietgrid.app.R
import com.quietgrid.app.data.AppContainer
import com.quietgrid.app.data.AppSettings
import com.quietgrid.app.data.ThemeMode
import kotlinx.coroutines.launch

private val DARK_ICON_COLOR = Color(0xFFA78BFA)
private val LIGHT_ICON_COLOR = Color(0xFFF2B705)
private val PENCIL_ICON_COLOR = Color(0xFF1A1A1A)

private data class ThemeOption(val mode: ThemeMode, val labelRes: Int, val icon: ImageVector, val iconColor: Color?)

private val THEME_OPTIONS = listOf(
    ThemeOption(ThemeMode.SYSTEM, R.string.common_system_default, Icons.Filled.SettingsSuggest, null),
    ThemeOption(ThemeMode.DARK, R.string.settings_dark, Icons.Filled.DarkMode, DARK_ICON_COLOR),
    ThemeOption(ThemeMode.LIGHT, R.string.settings_light, Icons.Filled.LightMode, LIGHT_ICON_COLOR),
    ThemeOption(ThemeMode.PENCIL, R.string.settings_pencil, Icons.Filled.Create, PENCIL_ICON_COLOR),
)

private data class LanguageOption(val tag: String, val labelRes: Int, val flag: String)

private val LANGUAGE_OPTIONS = listOf(
    LanguageOption("", R.string.settings_language_system_detail, "🌐"),
    LanguageOption("en", R.string.settings_language_english_detail, "🇬🇧"),
    LanguageOption("nl", R.string.settings_language_dutch_detail, "🇳🇱"),
    LanguageOption("de", R.string.settings_language_german_detail, "🇩🇪"),
    LanguageOption("fr", R.string.settings_language_french_detail, "🇫🇷"),
    LanguageOption("es", R.string.settings_language_spanish_detail, "🇪🇸"),
)

@Composable
fun SettingsScreen() {
    val scope = rememberCoroutineScope()
    val settings by AppContainer.settingsRepository.settings.collectAsState(initial = AppSettings())
    var themeMenuOpen by remember { mutableStateOf(false) }
    var languageMenuOpen by remember { mutableStateOf(false) }
    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentLanguageTag = if (currentLocales.isEmpty) "" else currentLocales[0]?.language ?: ""

    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        SettingsSectionTitle(stringResource(R.string.settings_appearance))

        val selectedTheme = THEME_OPTIONS.firstOrNull { it.mode == settings.themeMode } ?: THEME_OPTIONS[0]
        Box {
            SettingsDropdownRow(
                icon = selectedTheme.icon,
                iconTint = selectedTheme.iconColor,
                label = stringResource(R.string.settings_theme),
                value = stringResource(selectedTheme.labelRes),
                expanded = themeMenuOpen,
                onClick = { themeMenuOpen = true },
            )
            DropdownMenu(expanded = themeMenuOpen, onDismissRequest = { themeMenuOpen = false }) {
                THEME_OPTIONS.forEach { option ->
                    DropdownMenuItem(
                        leadingIcon = { Icon(option.icon, contentDescription = null, tint = option.iconColor ?: MaterialTheme.colorScheme.onSurfaceVariant) },
                        trailingIcon = {
                            if (option.mode == settings.themeMode) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        text = { Text(stringResource(option.labelRes)) },
                        onClick = {
                            themeMenuOpen = false
                            scope.launch { AppContainer.settingsRepository.setThemeMode(option.mode) }
                        },
                    )
                }
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 4.dp))

        val selectedLanguage = LANGUAGE_OPTIONS.firstOrNull { it.tag == currentLanguageTag } ?: LANGUAGE_OPTIONS[0]
        Box {
            SettingsDropdownRow(
                emoji = selectedLanguage.flag,
                label = stringResource(R.string.settings_language),
                detail = stringResource(R.string.settings_language_dropdown_detail),
                value = stringResource(selectedLanguage.labelRes),
                expanded = languageMenuOpen,
                onClick = { languageMenuOpen = true },
            )
            DropdownMenu(expanded = languageMenuOpen, onDismissRequest = { languageMenuOpen = false }) {
                LANGUAGE_OPTIONS.forEach { option ->
                    DropdownMenuItem(
                        leadingIcon = { Text(option.flag, style = MaterialTheme.typography.titleMedium) },
                        trailingIcon = {
                            if (option.tag == currentLanguageTag) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        text = { Text(stringResource(option.labelRes)) },
                        onClick = {
                            languageMenuOpen = false
                            val locales = if (option.tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(option.tag)
                            AppCompatDelegate.setApplicationLocales(locales)
                        },
                    )
                }
            }
        }
        Text(
            stringResource(R.string.settings_language_ai_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )

        HorizontalDivider(Modifier.padding(vertical = 4.dp))

        SettingsToggleRow(
            label = stringResource(R.string.settings_show_timer_in_play_label),
            detail = stringResource(R.string.settings_show_timer_in_play_detail),
            checked = settings.showTimerInPlay,
            onCheckedChange = { scope.launch { AppContainer.settingsRepository.setShowTimerInPlay(it) } },
        )

        SettingsSectionTitle(stringResource(R.string.settings_tutorials))

        SettingsToggleRow(
            label = stringResource(R.string.settings_beta_games_label),
            detail = stringResource(R.string.settings_beta_games_detail),
            checked = settings.betaGamesEnabled,
            onCheckedChange = { scope.launch { AppContainer.settingsRepository.setBetaGamesEnabled(it) } },
        )
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun SettingsDropdownRow(
    label: String,
    value: String,
    expanded: Boolean,
    onClick: () -> Unit,
    detail: String? = null,
    icon: ImageVector? = null,
    iconTint: Color? = null,
    emoji: String? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            when {
                icon != null -> Icon(icon, contentDescription = null, tint = iconTint ?: MaterialTheme.colorScheme.onSurfaceVariant)
                emoji != null -> Text(emoji, style = MaterialTheme.typography.titleMedium)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (detail != null) {
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            Icon(Icons.Filled.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsToggleRow(label: String, detail: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
