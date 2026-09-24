package com.quietgrid.app.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Nightlight
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
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.quietgrid.app.R
import com.quietgrid.app.core.AppLocale
import com.quietgrid.app.core.localeFlagEmoji
import com.quietgrid.app.data.AppSettings
import com.quietgrid.app.data.RepositoriesViewModel
import com.quietgrid.app.data.ThemeMode
import kotlinx.coroutines.launch
import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.DisposableEffect
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.quietgrid.app.notifications.DailyReminderNotifier
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val DARK_ICON_COLOR = Color(0xFFA78BFA)
private val LIGHT_ICON_COLOR = Color(0xFFF2B705)
private val PENCIL_ICON_COLOR = Color(0xFF1A1A1A)
private val SILK_ICON_COLOR = Color(0xFFFF7700)
private val NORD_ICON_COLOR = Color(0xFF5E81AC)
private val COFFEE_ICON_COLOR = Color(0xFFDB924C)
private val DRACULA_ICON_COLOR = Color(0xFFFF79C6)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private data class ThemeOption(val mode: ThemeMode, val labelRes: Int, val icon: ImageVector, val iconColor: Color?)

private val THEME_OPTIONS = listOf(
    ThemeOption(ThemeMode.SYSTEM, R.string.common_system_default, Icons.Filled.SettingsSuggest, null),
    ThemeOption(ThemeMode.DARK, R.string.settings_dark, Icons.Filled.DarkMode, DARK_ICON_COLOR),
    ThemeOption(ThemeMode.LIGHT, R.string.settings_light, Icons.Filled.LightMode, LIGHT_ICON_COLOR),
    ThemeOption(ThemeMode.PENCIL, R.string.settings_pencil, Icons.Filled.Create, PENCIL_ICON_COLOR),
    ThemeOption(ThemeMode.SILK, R.string.settings_silk, Icons.Filled.AutoAwesome, SILK_ICON_COLOR),
    ThemeOption(ThemeMode.NORD, R.string.settings_nord, Icons.Filled.AcUnit, NORD_ICON_COLOR),
    ThemeOption(ThemeMode.COFFEE, R.string.settings_coffee, Icons.Filled.Coffee, COFFEE_ICON_COLOR),
    ThemeOption(ThemeMode.DRACULA, R.string.settings_dracula, Icons.Filled.Nightlight, DRACULA_ICON_COLOR),
)

private data class LanguageOption(val tag: String, val labelRes: Int, val flag: String)

private val LANGUAGE_OPTIONS = listOf(
    LanguageOption("", R.string.settings_language_system_detail, localeFlagEmoji("")),
    LanguageOption("en", R.string.settings_language_english_detail, localeFlagEmoji("en")),
    LanguageOption("nl", R.string.settings_language_dutch_detail, localeFlagEmoji("nl")),
    LanguageOption("de", R.string.settings_language_german_detail, localeFlagEmoji("de")),
    LanguageOption("fr", R.string.settings_language_french_detail, localeFlagEmoji("fr")),
    LanguageOption("es", R.string.settings_language_spanish_detail, localeFlagEmoji("es")),
    LanguageOption("pt", R.string.settings_language_portuguese_detail, localeFlagEmoji("pt")),
)

private val PUZZLE_LANGUAGE_OPTIONS = listOf(
    LanguageOption("", R.string.settings_puzzle_language_system_detail, localeFlagEmoji("")),
    LanguageOption("en", R.string.settings_puzzle_language_english_detail, localeFlagEmoji("en")),
    LanguageOption("nl", R.string.settings_puzzle_language_dutch_detail, localeFlagEmoji("nl")),
    LanguageOption("de", R.string.settings_puzzle_language_german_detail, localeFlagEmoji("de")),
    LanguageOption("fr", R.string.settings_puzzle_language_french_detail, localeFlagEmoji("fr")),
    LanguageOption("es", R.string.settings_puzzle_language_spanish_detail, localeFlagEmoji("es")),
    LanguageOption("pt", R.string.settings_puzzle_language_portuguese_detail, localeFlagEmoji("pt")),
)

@Composable
fun SettingsPageScreen() {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        PreferencesSection()
    }
}

@Composable
fun PreferencesSection() {
    val repositories: RepositoriesViewModel = hiltViewModel()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val settings by repositories.settingsRepository.settings.collectAsState(initial = AppSettings())
    var themeMenuOpen by remember { mutableStateOf(false) }
    var languageMenuOpen by remember { mutableStateOf(false) }
    var puzzleLanguageMenuOpen by remember { mutableStateOf(false) }
    val currentLanguageTag = AppLocale.currentTag(context)

    Column(Modifier.fillMaxWidth()) {
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
                            scope.launch { repositories.settingsRepository.setThemeMode(option.mode) }
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
                            AppLocale.setLanguage(context, option.tag)
                            context.findActivity()?.recreate()
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

        val selectedPuzzleLanguage = PUZZLE_LANGUAGE_OPTIONS.firstOrNull { it.tag == settings.puzzleLanguage } ?: PUZZLE_LANGUAGE_OPTIONS[0]
        Box {
            SettingsDropdownRow(
                emoji = selectedPuzzleLanguage.flag,
                label = stringResource(R.string.settings_puzzle_language),
                detail = stringResource(R.string.settings_puzzle_language_dropdown_detail),
                value = stringResource(selectedPuzzleLanguage.labelRes),
                expanded = puzzleLanguageMenuOpen,
                onClick = { puzzleLanguageMenuOpen = true },
            )
            DropdownMenu(expanded = puzzleLanguageMenuOpen, onDismissRequest = { puzzleLanguageMenuOpen = false }) {
                PUZZLE_LANGUAGE_OPTIONS.forEach { option ->
                    DropdownMenuItem(
                        leadingIcon = { Text(option.flag, style = MaterialTheme.typography.titleMedium) },
                        trailingIcon = {
                            if (option.tag == settings.puzzleLanguage) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        text = { Text(stringResource(option.labelRes)) },
                        onClick = {
                            puzzleLanguageMenuOpen = false
                            scope.launch { repositories.settingsRepository.setPuzzleLanguage(option.tag) }
                        },
                    )
                }
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 4.dp))

        SettingsToggleRow(
            label = stringResource(R.string.settings_show_timer_in_play_label),
            checked = settings.showTimerInPlay,
            onCheckedChange = { scope.launch { repositories.settingsRepository.setShowTimerInPlay(it) } },
        )

        HorizontalDivider(Modifier.padding(vertical = 4.dp))

        DailyReminderSettings(settings)

        HorizontalDivider(Modifier.padding(vertical = 4.dp))

        SettingsToggleRow(
            label = stringResource(R.string.settings_beta_games_label),
            detail = stringResource(R.string.settings_beta_games_detail),
            checked = settings.betaGamesEnabled,
            onCheckedChange = { scope.launch { repositories.settingsRepository.setBetaGamesEnabled(it) } },
        )
    }
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
private fun SettingsToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, detail: String? = null) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (detail != null) {
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DailyReminderSettings(settings: AppSettings) {
    val repositories: RepositoriesViewModel = hiltViewModel()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionGranted by remember { mutableStateOf(DailyReminderNotifier.hasPermission(context)) }
    var showTimePicker by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permissionGranted = DailyReminderNotifier.hasPermission(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionGranted = DailyReminderNotifier.hasPermission(context)
        if (granted) scope.launch { repositories.settingsRepository.setDailyReminderEnabled(true) }
    }

    fun openSystemNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    val active = settings.dailyReminderEnabled && permissionGranted

    SettingsToggleRow(
        label = stringResource(R.string.settings_daily_reminder),
        detail = stringResource(R.string.settings_daily_reminder_detail),
        checked = active,
        onCheckedChange = { turnOn ->
            if (!turnOn) {
                scope.launch { repositories.settingsRepository.setDailyReminderEnabled(false) }
            } else if (permissionGranted) {
                scope.launch { repositories.settingsRepository.setDailyReminderEnabled(true) }
            } else {
                scope.launch { repositories.settingsRepository.setDailyReminderEnabled(true) }
                val activity = context.findActivity()
                val permanentlyDenied = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    settings.dailyReminderPermissionAsked &&
                    activity != null &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !permanentlyDenied) {
                    scope.launch { repositories.settingsRepository.markDailyReminderPermissionAsked() }
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    openSystemNotificationSettings()
                }
            }
        },
    )

    val timeText = settings.dailyReminderTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = active) { showTimePicker = true }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val alpha = if (active) 1f else 0.38f
        Text(
            stringResource(R.string.settings_daily_reminder_time),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
        )
        Text(
            timeText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
        )
    }

    if (showTimePicker) {
        val pickerState = rememberTimePickerState(
            initialHour = settings.dailyReminderTime.hour,
            initialMinute = settings.dailyReminderTime.minute,
            is24Hour = DateFormat.is24HourFormat(context),
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showTimePicker = false
                    scope.launch { repositories.settingsRepository.setDailyReminderTime(LocalTime.of(pickerState.hour, pickerState.minute)) }
                }) { Text(stringResource(R.string.settings_daily_reminder_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.common_cancel)) }
            },
            text = { TimePicker(state = pickerState) },
        )
    }
}
