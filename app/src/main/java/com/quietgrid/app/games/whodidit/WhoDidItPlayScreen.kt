package com.quietgrid.app.games.whodidit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.ui.components.CollectPuzzleResult
import com.quietgrid.app.ui.components.ElapsedTimerText
import com.quietgrid.app.ui.components.EndPuzzleDialog
import com.quietgrid.app.ui.components.EndPuzzleIconButton
import com.quietgrid.app.ui.components.GameBackButton
import com.quietgrid.app.ui.components.PuzzleBoardContainer
import com.quietgrid.app.ui.components.rememberHapticController
import com.quietgrid.engine.whodidit.WhoDidItPuzzleEntry
import com.quietgrid.engine.whodidit.WhoDidItRef
import java.time.LocalDate

private val PANEL_MAX_HEIGHT = 190.dp
private val PANEL_MIN_HEIGHT = 120.dp
private const val PANEL_SHARE = 0.34f
private const val PANEL_MAX_SHARE = 0.45f

fun whoDidItPanelHeight(available: Dp): Dp =
    (available * PANEL_SHARE).coerceIn(PANEL_MIN_HEIGHT, PANEL_MAX_HEIGHT).coerceAtMost(available * PANEL_MAX_SHARE)

@Composable
fun WhoDidItPlayScreen(
    difficulty: Difficulty,
    resume: Boolean,
    dailyDate: LocalDate?,
    onBack: () -> Unit,
    onFinished: (WhoDidItResult) -> Unit,
) {
    val viewModel = hiltViewModel<WhoDidItPlayViewModel, WhoDidItPlayViewModel.Factory>(
        creationCallback = { factory -> factory.create(difficulty, resume, dailyDate) },
    )
    CollectPuzzleResult(viewModel.result, onFinished)

    var showEndDialog by remember { mutableStateOf(false) }
    val session = viewModel.session
    val haptics = rememberHapticController()
    LaunchedEffect(viewModel.wrongPickEvent) {
        if (viewModel.wrongPickEvent != null) {
            haptics.incorrectFeedback()
        }
    }

    val resources = LocalContext.current.resources
    val text = session?.puzzle?.let { puzzle -> remember(puzzle.id) { WhoDidItText(resources, puzzle) } }
    val hint = viewModel.hint
    val hintCells = remember(hint, session?.puzzle?.id) {
        val categoryCount = session?.puzzle?.categories?.size
        if (categoryCount == null) emptyMap() else whoDidItHintCells(categoryCount, hint?.step)
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            GameBackButton(onBack)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = viewModel::toggleHint, enabled = !viewModel.isComputingHint) {
                if (viewModel.isComputingHint) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = Icons.Filled.Lightbulb,
                        contentDescription = stringResource(
                            if (hint != null) R.string.whodidit_hint_hide else R.string.whodidit_hint_show,
                        ),
                        tint = if (hint != null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = viewModel::onUndo) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = stringResource(R.string.whodidit_undo),
                )
            }
            EndPuzzleIconButton(onClick = { showEndDialog = true })
        }

        ElapsedTimerText(
            viewModel.elapsedSeconds.toInt(),
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
        )

        if (hint != null) {
            Card(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Column(Modifier.padding(12.dp)) {
                    val step = hint.step
                    if (step == null || text == null) {
                        Text(stringResource(R.string.whodidit_hint_none), style = MaterialTheme.typography.bodySmall)
                    } else {
                        hint.lead?.let { lead ->
                            Text(
                                text.hintLead(lead),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                        }
                        Text(text.hintTitle(step), style = MaterialTheme.typography.titleSmall)
                        Text(text.hintBody(step), style = MaterialTheme.typography.bodySmall)
                        text.hintAction(step)?.let { action ->
                            Text(action, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 2.dp))
                        }
                        if (hint.hasWrongMarks) {
                            Text(
                                stringResource(R.string.whodidit_hint_wrong_marks),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
        }

        if (text != null) {
            Card(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(text.caseTitle(), style = MaterialTheme.typography.titleSmall)
                    Text(text.caseQuestion(), style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val panelHeight = whoDidItPanelHeight(maxHeight)
            Column(Modifier.fillMaxSize()) {
                PuzzleBoardContainer(
                    visible = session != null && text != null,
                    playFresh = !resume,
                    zoomable = true,
                ) {
                    if (session != null && text != null) {
                        WhoDidItGrid(
                            puzzle = session.puzzle,
                            blocks = session.blocks,
                            text = text,
                            hintCells = hintCells,
                            wrongPickEvent = viewModel.wrongPickEvent,
                            onCellTap = viewModel::onCellTap,
                        )
                    }
                }

                if (viewModel.showNotQuite) {
                    Text(
                        stringResource(R.string.whodidit_not_quite),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                if (session != null && text != null) {
                    Spacer(Modifier.height(8.dp))
                    WhoDidItInfoPanel(puzzle = session.puzzle, text = text, modifier = Modifier.height(panelHeight))
                }
            }
        }
    }

    EndPuzzleDialog(
        visible = showEndDialog,
        onDismiss = { showEndDialog = false },
        onConfirm = {
            showEndDialog = false
            viewModel.endPuzzle()
        },
    )
}

private enum class InfoTab(val labelRes: Int) {
    CLUES(R.string.whodidit_tab_clues),
    SUSPECTS(R.string.whodidit_tab_suspects),
    PLACES(R.string.whodidit_tab_places),
    ITEMS(R.string.whodidit_tab_items),
    STATEMENTS(R.string.whodidit_tab_statements),
}

@Composable
private fun WhoDidItInfoPanel(puzzle: WhoDidItPuzzleEntry, text: WhoDidItText, modifier: Modifier = Modifier) {
    val tabs = remember(puzzle.id) {
        InfoTab.entries.filter { it != InfoTab.STATEMENTS || puzzle.statements.isNotEmpty() }
    }
    var selected by remember(puzzle.id) { mutableIntStateOf(0) }
    Column(modifier.fillMaxWidth()) {
        ScrollableTabRow(selectedTabIndex = selected, edgePadding = 0.dp) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = index == selected,
                    onClick = { selected = index },
                    modifier = Modifier.testTag("whodidit-tab-${tab.name.lowercase()}"),
                    text = { Text(stringResource(tab.labelRes), style = MaterialTheme.typography.labelMedium) },
                )
            }
        }
        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            when (tabs[selected.coerceIn(0, tabs.lastIndex)]) {
                InfoTab.CLUES -> puzzle.clues.indices.forEach { index ->
                    Text("${index + 1}. ${text.clue(index)}", style = MaterialTheme.typography.bodyMedium)
                }
                InfoTab.SUSPECTS -> EntryCards(puzzle, text, 0)
                InfoTab.PLACES -> EntryCards(puzzle, text, 1)
                InfoTab.ITEMS -> EntryCards(puzzle, text, 2)
                InfoTab.STATEMENTS -> puzzle.statements.indices.forEach { index ->
                    Text(text.statement(index), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun EntryCards(puzzle: WhoDidItPuzzleEntry, text: WhoDidItText, category: Int) {
    repeat(puzzle.size) { slot ->
        val ref = WhoDidItRef(category, slot)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(10.dp)) {
                Text(text.entryName(ref), style = MaterialTheme.typography.titleSmall)
                text.cardLines(ref).forEach { line ->
                    Text(line, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
