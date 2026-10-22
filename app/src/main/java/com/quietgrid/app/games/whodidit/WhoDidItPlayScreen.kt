package com.quietgrid.app.games.whodidit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.ui.components.CollectPuzzleResult
import com.quietgrid.app.ui.components.PuzzleBoardContainer
import com.quietgrid.app.ui.components.rememberHapticController
import com.quietgrid.app.ui.play.GamePlayScaffold
import com.quietgrid.app.ui.play.InfoItem
import com.quietgrid.app.ui.play.PlayHintAction
import com.quietgrid.app.ui.play.PlayHintMessage
import com.quietgrid.app.ui.play.PlaySizeClass
import com.quietgrid.app.ui.play.PlayTimer
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
    val finishing by viewModel.finishing.collectAsState()

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

    GamePlayScaffold(
        gameId = GameId.WHODIDIT,
        timer = PlayTimer.Elapsed(viewModel.elapsedSeconds.toInt()),
        onBack = onBack,
        onEndPuzzle = { viewModel.endPuzzle() },
        finishing = finishing,
        difficulty = session?.let { Difficulty.fromKey(it.puzzle.difficulty) },
        hint = PlayHintAction(
            active = hint != null,
            busy = viewModel.isComputingHint,
            contentDescription = stringResource(if (hint != null) R.string.whodidit_hint_hide else R.string.whodidit_hint_show),
            onClick = viewModel::toggleHint,
        ),
        hintMessage = hint?.let { current -> whoDidItHintMessage(current, text) },
        info = if (text == null) emptyList() else listOf(InfoItem.Label(text.caseTitle(), emphasized = true)),
        wideDock = true,
        dockHeight = ::whoDidItDockHeight,
        dock = {
            if (session != null && text != null) {
                Row(Modifier.fillMaxWidth().height(WHO_DID_IT_MESSAGE_HEIGHT), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (viewModel.showNotQuite) stringResource(R.string.whodidit_not_quite) else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = viewModel::onUndo) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = stringResource(R.string.whodidit_undo),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                WhoDidItInfoPanel(puzzle = session.puzzle, text = text, modifier = Modifier.weight(1f))
            }
        },
        board = {
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
        },
    )
}

private val WHO_DID_IT_MESSAGE_HEIGHT = 40.dp

private fun whoDidItDockHeight(sizeClass: PlaySizeClass): Dp = WHO_DID_IT_MESSAGE_HEIGHT + when (sizeClass) {
    PlaySizeClass.Compact -> PANEL_MIN_HEIGHT + 30.dp
    PlaySizeClass.TabletPortrait -> 320.dp
    else -> PANEL_MAX_HEIGHT
}

@Composable
private fun whoDidItHintMessage(hint: WhoDidItHint, text: WhoDidItText?): PlayHintMessage {
    val step = hint.step
    if (step == null || text == null) return PlayHintMessage(stringResource(R.string.whodidit_hint_none), "")
    val body = buildList {
        hint.lead?.let { add(text.hintLead(it)) }
        add(text.hintBody(step))
        text.hintAction(step)?.let { add(it) }
        if (hint.hasWrongMarks) add(stringResource(R.string.whodidit_hint_wrong_marks))
    }.joinToString("\n")
    return PlayHintMessage(text.hintTitle(step), body)
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
        SecondaryScrollableTabRow(selectedTabIndex = selected, edgePadding = 0.dp) {
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
                InfoTab.CLUES -> {
                    Text(text.caseQuestion(), style = MaterialTheme.typography.titleSmall)
                    puzzle.clues.indices.forEach { index ->
                        Text("${index + 1}. ${text.clue(index)}", style = MaterialTheme.typography.bodyMedium)
                    }
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
