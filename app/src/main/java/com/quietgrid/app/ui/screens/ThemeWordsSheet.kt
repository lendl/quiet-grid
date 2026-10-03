package com.quietgrid.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.appVersionName
import com.quietgrid.app.core.buildThemeWordsIssueUrl
import com.quietgrid.app.core.themes.MAX_THEME_WORD_CHANGES
import com.quietgrid.app.core.themes.ThemeWordIndex
import com.quietgrid.app.core.themes.matchesThemeWordSearch
import com.quietgrid.app.core.themes.parseWordSuggestions
import com.quietgrid.app.core.themes.themeIcon
import com.quietgrid.app.core.themes.themeLabelRes
import kotlinx.coroutines.launch
import java.text.Collator
import java.util.Locale

private enum class ThemeWordsPage { WORDS, SUGGEST }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeWordsSheet(gameId: GameId, themeId: String, locale: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val words by produceState<List<String>?>(null, locale, themeId) {
        value = ThemeWordIndex.wordsFor(context, locale, themeId)
            .sortedWith(Collator.getInstance(Locale.forLanguageTag(locale)))
    }
    var page by rememberSaveable { mutableStateOf(ThemeWordsPage.WORDS) }
    var query by rememberSaveable { mutableStateOf("") }
    var reported by rememberSaveable { mutableStateOf(listOf<String>()) }
    var input by rememberSaveable { mutableStateOf("") }
    val close: () -> Unit = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() } }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        BackHandler(enabled = page == ThemeWordsPage.SUGGEST) { page = ThemeWordsPage.WORDS }
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            val currentWords = words
            when {
                currentWords == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                page == ThemeWordsPage.WORDS -> ThemeWordsListPage(
                    themeId = themeId,
                    words = currentWords,
                    query = query,
                    onQueryChange = { query = it },
                    reported = reported,
                    onToggleReport = { word -> reported = if (word in reported) reported - word else reported + word },
                    onSuggest = { page = ThemeWordsPage.SUGGEST },
                    onClose = close,
                )
                else -> ThemeWordsSuggestPage(
                    themeId = themeId,
                    locale = locale,
                    words = currentWords,
                    input = input,
                    onInputChange = { input = it },
                    reported = reported,
                    onUnreport = { word -> reported = reported - word },
                    onBack = { page = ThemeWordsPage.WORDS },
                    onClose = close,
                    onOpenIssue = { added ->
                        val url = buildThemeWordsIssueUrl(
                            gameKey = gameId.key,
                            themeId = themeId,
                            locale = locale,
                            added = added,
                            removed = reported,
                            appVersion = appVersionName(context),
                        )
                        val opened = runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }.isSuccess
                        if (opened) close()
                    },
                )
            }
        }
    }
}

@Composable
private fun ThemeWordsHeader(themeId: String, subtitle: String, onClose: () -> Unit, onBack: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
            }
        }
        themeIcon(themeId)?.let {
            Text(it, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(end = 12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(stringResource(themeLabelRes(themeId)), style = MaterialTheme.typography.titleLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onClose) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.common_close))
        }
    }
}

@Composable
private fun ColumnScope.ThemeWordsListPage(
    themeId: String,
    words: List<String>,
    query: String,
    onQueryChange: (String) -> Unit,
    reported: List<String>,
    onToggleReport: (String) -> Unit,
    onSuggest: () -> Unit,
    onClose: () -> Unit,
) {
    val filtered = remember(words, query) { words.filter { matchesThemeWordSearch(it, query) } }

    ThemeWordsHeader(themeId, stringResource(R.string.theme_words_count, words.size), onClose)
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text(stringResource(R.string.theme_words_search)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = if (query.isEmpty()) {
            null
        } else {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.common_clear))
                }
            }
        },
    )
    Text(
        stringResource(R.string.theme_words_report_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
    Box(Modifier.weight(1f).fillMaxWidth()) {
        if (filtered.isEmpty()) {
            Text(
                stringResource(R.string.theme_words_no_results),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp),
            )
        } else {
            LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize()) {
                items(filtered, key = { it }) { word ->
                    val isReported = word in reported
                    Text(
                        word,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isReported) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        textDecoration = if (isReported) TextDecoration.LineThrough else null,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(value = isReported, role = Role.Checkbox, onValueChange = { onToggleReport(word) })
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
    Button(onClick = onSuggest, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text(
            if (reported.isEmpty()) {
                stringResource(R.string.theme_words_suggest)
            } else {
                stringResource(R.string.theme_words_suggest_with_reports, reported.size)
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ColumnScope.ThemeWordsSuggestPage(
    themeId: String,
    locale: String,
    words: List<String>,
    input: String,
    onInputChange: (String) -> Unit,
    reported: List<String>,
    onUnreport: (String) -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onOpenIssue: (List<String>) -> Unit,
) {
    val suggestions = remember(input, words) { parseWordSuggestions(input, words) }
    val tooMany = suggestions.added.size > MAX_THEME_WORD_CHANGES || reported.size > MAX_THEME_WORD_CHANGES
    val canOpen = (suggestions.added.isNotEmpty() || reported.isNotEmpty()) && !tooMany
    val appLocale = LocalConfiguration.current.locales[0]
    val language = remember(locale, appLocale) {
        Locale.forLanguageTag(locale).getDisplayLanguage(appLocale).replaceFirstChar { it.titlecase(appLocale) }
    }
    val rules = listOf(
        stringResource(R.string.theme_words_rule_single, language),
        stringResource(R.string.theme_words_rule_common),
        stringResource(R.string.theme_words_rule_brands),
        stringResource(R.string.theme_words_rule_safe),
        stringResource(R.string.theme_words_rule_kids),
    )

    ThemeWordsHeader(themeId, stringResource(R.string.theme_words_suggest), onClose, onBack)
    Column(
        Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.theme_words_rules_title), style = MaterialTheme.typography.titleSmall)
                rules.forEach { rule ->
                    Row {
                        Text("•", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(end = 8.dp))
                        Text(rule, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            label = { Text(stringResource(R.string.theme_words_input_label)) },
            isError = tooMany,
            supportingText = {
                Text(
                    when {
                        tooMany -> stringResource(R.string.theme_words_too_many, MAX_THEME_WORD_CHANGES)
                        input.isBlank() -> stringResource(R.string.theme_words_input_supporting)
                        else -> stringResource(
                            R.string.theme_words_input_summary,
                            suggestions.added.size,
                            suggestions.duplicates.size,
                            suggestions.invalid.size,
                        )
                    },
                )
            },
        )
        if (reported.isNotEmpty()) {
            Text(stringResource(R.string.theme_words_reported_title), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                reported.forEach { word ->
                    InputChip(
                        selected = false,
                        onClick = { onUnreport(word) },
                        label = { Text(word) },
                        trailingIcon = {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(R.string.theme_words_unreport, word),
                                modifier = Modifier.size(InputChipDefaults.IconSize),
                            )
                        },
                    )
                }
            }
        }
        Text(
            stringResource(R.string.theme_words_github_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Button(
        onClick = { onOpenIssue(suggestions.added) },
        enabled = canOpen,
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
    ) {
        Text(stringResource(R.string.theme_words_open_issue))
    }
}
