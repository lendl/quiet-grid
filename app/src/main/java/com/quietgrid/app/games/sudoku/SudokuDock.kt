package com.quietgrid.app.games.sudoku

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.quietgrid.app.R
import com.quietgrid.app.ui.play.PlaySegmentedButtons
import com.quietgrid.app.ui.play.PlaySizeClass

private val PAD_KEY_SHAPE = RoundedCornerShape(10.dp)
private val SIDE_ROW_HEIGHT = 40.dp
private val UPRIGHT_TOP_PADDING = 4.dp
private val TOOL_MAX_WIDTH = 120.dp

internal class SudokuEditControls(
    val noteMode: Boolean,
    val autoCandidateMode: Boolean,
    val canUndo: Boolean,
    val onToggleNoteMode: () -> Unit,
    val onToggleAutoCandidateMode: () -> Unit,
    val onUndo: () -> Unit,
)

internal class SudokuDockModel(
    val digitCount: Int,
    val digitsEnabled: Boolean,
    val clearEnabled: Boolean,
    val isDigitActive: (Int) -> Boolean,
    val onDigit: (Int) -> Unit,
    val onClear: () -> Unit,
    val edit: SudokuEditControls? = null,
)

internal fun sudokuDockModel(
    session: SudokuSession,
    selectedCell: Pair<Int, Int>?,
    canUndo: Boolean,
    onDigit: (Int) -> Unit,
    onClear: () -> Unit,
    onToggleNoteMode: () -> Unit,
    onToggleAutoCandidateMode: () -> Unit,
    onUndo: () -> Unit,
): SudokuDockModel {
    val noteMode = session.inputMode == SudokuInputMode.NOTES
    val (row, col) = selectedCell ?: (-1 to -1)
    val selectedValue = if (selectedCell != null) session.board[row][col] else null
    val selectedNotes = session.notes.getOrElse(row) { emptyList() }.getOrElse(col) { emptySet() }
    return SudokuDockModel(
        digitCount = session.board.size,
        digitsEnabled = selectedCell != null && !(noteMode && selectedValue != null),
        clearEnabled = selectedCell != null,
        isDigitActive = { digit -> if (noteMode) digit in selectedNotes else selectedValue == digit },
        onDigit = onDigit,
        onClear = onClear,
        edit = SudokuEditControls(
            noteMode = noteMode,
            autoCandidateMode = session.autoCandidateMode,
            canUndo = canUndo,
            onToggleNoteMode = onToggleNoteMode,
            onToggleAutoCandidateMode = onToggleAutoCandidateMode,
            onUndo = onUndo,
        ),
    )
}

private class SudokuDockMetrics(
    val toolHeight: Dp,
    val toolIconSize: Dp,
    val toolLabelSize: TextUnit,
    val keyHeight: Dp,
    val keyTextSize: TextUnit,
    val gap: Dp,
)

private fun sudokuDockMetrics(sizeClass: PlaySizeClass): SudokuDockMetrics = when (sizeClass) {
    PlaySizeClass.Compact -> SudokuDockMetrics(48.dp, 20.dp, 11.sp, 46.dp, 20.sp, 6.dp)
    PlaySizeClass.Medium -> SudokuDockMetrics(56.dp, 22.dp, 11.sp, 52.dp, 22.sp, 10.dp)
    PlaySizeClass.PhoneLandscape -> SudokuDockMetrics(SIDE_ROW_HEIGHT, 20.dp, 12.sp, 64.dp, 24.sp, 6.dp)
    PlaySizeClass.TabletLandscape -> SudokuDockMetrics(84.dp, 30.dp, 16.sp, 104.dp, 40.sp, 20.dp)
    PlaySizeClass.TabletPortrait -> SudokuDockMetrics(84.dp, 30.dp, 16.sp, 80.dp, 36.sp, 16.dp)
}

internal fun sudokuDockHeight(sizeClass: PlaySizeClass): Dp {
    val metrics = sudokuDockMetrics(sizeClass)
    return UPRIGHT_TOP_PADDING + metrics.toolHeight + metrics.gap + metrics.keyHeight
}

@Composable
internal fun SudokuDock(sizeClass: PlaySizeClass, model: SudokuDockModel) {
    val metrics = sudokuDockMetrics(sizeClass)
    when (sizeClass) {
        PlaySizeClass.PhoneLandscape -> SudokuSideDock(model, metrics)
        PlaySizeClass.TabletLandscape -> SudokuPanelDock(model, metrics)
        else -> SudokuUprightDock(model, metrics)
    }
}

@Composable
private fun SudokuUprightDock(model: SudokuDockModel, metrics: SudokuDockMetrics) {
    Column(Modifier.fillMaxWidth().padding(top = UPRIGHT_TOP_PADDING)) {
        SudokuToolsRow(model, metrics)
        Spacer(Modifier.height(metrics.gap))
        Row(Modifier.fillMaxWidth().height(metrics.keyHeight), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            for (digit in 1..model.digitCount) {
                SudokuDigitKey(digit, model, metrics.keyTextSize, Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun SudokuPanelDock(model: SudokuDockModel, metrics: SudokuDockMetrics) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val columns = sudokuPadColumns(model.digitCount)
        val rows = (model.digitCount + columns - 1) / columns
        val gridSpace = maxHeight - metrics.toolHeight - metrics.gap * (rows + 1)
        val keyHeight = min(metrics.keyHeight, (gridSpace / rows).coerceAtLeast(0.dp))
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(metrics.gap, Alignment.CenterVertically)) {
            SudokuToolsRow(model, metrics)
            SudokuDigitGrid(model, columns, keyHeight, metrics.keyTextSize, gap = 10.dp)
        }
    }
}

@Composable
private fun SudokuSideDock(model: SudokuDockModel, metrics: SudokuDockMetrics) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val edit = model.edit
        val columns = sudokuPadColumns(model.digitCount)
        val rows = (model.digitCount + columns - 1) / columns
        val fixedRows = if (edit != null) 2 else 1
        val gridSpace = maxHeight - SIDE_ROW_HEIGHT * fixedRows - metrics.gap * (rows - 1 + fixedRows)
        val preferredKeyHeight = if (model.digitCount > 6) metrics.keyHeight else 72.dp
        val keyHeight = min(preferredKeyHeight, (gridSpace / rows).coerceAtLeast(0.dp))
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(metrics.gap, Alignment.CenterVertically)) {
            if (edit != null) {
                Row(Modifier.fillMaxWidth().height(SIDE_ROW_HEIGHT), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SudokuModeSwitch(edit, Modifier.weight(1f))
                    SudokuSquareKey(Icons.AutoMirrored.Outlined.Undo, R.string.sudoku_undo, edit.canUndo, edit.onUndo)
                }
            }
            SudokuDigitGrid(model, columns, keyHeight, metrics.keyTextSize, gap = 6.dp)
            Row(Modifier.fillMaxWidth().height(SIDE_ROW_HEIGHT), verticalAlignment = Alignment.CenterVertically) {
                SudokuSquareKey(Icons.Filled.Close, R.string.sudoku_clear_cell, model.clearEnabled, model.onClear)
                Spacer(Modifier.weight(1f))
                if (edit != null) {
                    Text(
                        stringResource(R.string.sudoku_auto_candidates),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Switch(
                        checked = edit.autoCandidateMode,
                        onCheckedChange = { edit.onToggleAutoCandidateMode() },
                        modifier = Modifier.scale(0.7f),
                    )
                }
            }
        }
    }
}

private fun sudokuPadColumns(digitCount: Int): Int = if (digitCount >= 6) 3 else 2

@Composable
private fun SudokuToolsRow(model: SudokuDockModel, metrics: SudokuDockMetrics) {
    val edit = model.edit
    Row(
        Modifier.fillMaxWidth().height(metrics.toolHeight),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        if (edit != null) {
            SudokuTool(Icons.AutoMirrored.Outlined.Undo, R.string.sudoku_undo, metrics, enabled = edit.canUndo, onClick = edit.onUndo)
        }
        SudokuTool(Icons.AutoMirrored.Outlined.Backspace, R.string.sudoku_clear_cell, metrics, enabled = model.clearEnabled, onClick = model.onClear)
        if (edit != null) {
            SudokuTool(
                Icons.Outlined.Edit,
                R.string.sudoku_candidate_mode_label,
                metrics,
                checked = edit.noteMode,
                enabled = !edit.autoCandidateMode,
                onClick = edit.onToggleNoteMode,
            )
            SudokuTool(
                Icons.Outlined.AutoFixHigh,
                R.string.sudoku_auto_candidates,
                metrics,
                checked = edit.autoCandidateMode,
                onClick = edit.onToggleAutoCandidateMode,
            )
        }
    }
}

@Composable
private fun RowScope.SudokuTool(
    icon: ImageVector,
    @StringRes labelRes: Int,
    metrics: SudokuDockMetrics,
    checked: Boolean? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val active = checked == true
    val contentColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
        active -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val modifier = Modifier.weight(1f, fill = false).widthIn(max = TOOL_MAX_WIDTH).fillMaxWidth().fillMaxHeight()
    val shape = RoundedCornerShape(14.dp)
    val color = if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val content: @Composable () -> Unit = {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(metrics.toolIconSize))
            Text(
                stringResource(labelRes),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = metrics.toolLabelSize, fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    if (checked == null) {
        Surface(onClick = onClick, enabled = enabled, modifier = modifier, shape = shape, color = color, contentColor = contentColor, content = content)
    } else {
        Surface(
            checked = checked,
            onCheckedChange = { onClick() },
            enabled = enabled,
            modifier = modifier,
            shape = shape,
            color = color,
            contentColor = contentColor,
            content = content,
        )
    }
}

@Composable
private fun SudokuModeSwitch(edit: SudokuEditControls, modifier: Modifier) {
    PlaySegmentedButtons(
        labels = listOf(stringResource(R.string.sudoku_normal_mode_label), stringResource(R.string.sudoku_candidate_mode_label)),
        selectedIndex = if (edit.noteMode) 1 else 0,
        onSelect = { edit.onToggleNoteMode() },
        modifier = modifier.fillMaxHeight(),
        isEnabled = { index -> index == 0 || !edit.autoCandidateMode },
    )
}

@Composable
private fun SudokuSquareKey(icon: ImageVector, @StringRes descriptionRes: Int, enabled: Boolean, onClick: () -> Unit) {
    SudokuPadButton(active = false, enabled = enabled, onClick = onClick, modifier = Modifier.size(44.dp, SIDE_ROW_HEIGHT)) {
        Icon(icon, contentDescription = stringResource(descriptionRes), modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun SudokuDigitGrid(model: SudokuDockModel, columns: Int, keyHeight: Dp, keyTextSize: TextUnit, gap: Dp) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(gap)) {
        (1..model.digitCount).chunked(columns).forEach { rowDigits ->
            Row(Modifier.fillMaxWidth().height(keyHeight), horizontalArrangement = Arrangement.spacedBy(gap)) {
                rowDigits.forEach { digit ->
                    SudokuDigitKey(digit, model, keyTextSize, Modifier.weight(1f).fillMaxHeight())
                }
                repeat(columns - rowDigits.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun SudokuDigitKey(digit: Int, model: SudokuDockModel, textSize: TextUnit, modifier: Modifier) {
    SudokuPadButton(
        active = model.isDigitActive(digit),
        enabled = model.digitsEnabled,
        onClick = { model.onDigit(digit) },
        modifier = modifier,
    ) {
        Text(
            digit.toString(),
            style = MaterialTheme.typography.titleMedium.copy(fontSize = textSize, lineHeight = textSize, fontWeight = FontWeight.Medium),
        )
    }
}

@Composable
internal fun SudokuPadButton(
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val contentColor = when {
        active -> MaterialTheme.colorScheme.onPrimary
        enabled -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = PAD_KEY_SHAPE,
        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = contentColor,
        border = if (active) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            content()
        }
    }
}
