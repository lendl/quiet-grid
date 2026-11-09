package com.quietgrid.app.games.wordswap

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quietgrid.app.R
import com.quietgrid.app.ui.play.PlaySizeClass
import com.quietgrid.engine.wordswap.WordSwapDirection

private val WORDSWAP_TOOL_MAX_WIDTH = 120.dp
private val LIST_FADE_HEIGHT = 14.dp

private class WordSwapDockMetrics(
    val bankHeight: Dp,
    val toolHeight: Dp,
    val iconSize: Dp,
    val labelSize: TextUnit,
    val wordSize: TextUnit,
)

private fun wordSwapDockMetrics(sizeClass: PlaySizeClass): WordSwapDockMetrics = when (sizeClass) {
    PlaySizeClass.Compact -> WordSwapDockMetrics(76.dp, 48.dp, 20.dp, 11.sp, 13.sp)
    PlaySizeClass.Medium -> WordSwapDockMetrics(100.dp, 56.dp, 22.dp, 11.sp, 15.sp)
    PlaySizeClass.PhoneLandscape -> WordSwapDockMetrics(150.dp, 56.dp, 20.dp, 12.sp, 14.sp)
    PlaySizeClass.TabletLandscape -> WordSwapDockMetrics(220.dp, 84.dp, 30.dp, 16.sp, 20.sp)
    PlaySizeClass.TabletPortrait -> WordSwapDockMetrics(150.dp, 84.dp, 30.dp, 16.sp, 20.sp)
}

internal fun wordSwapDockHeight(sizeClass: PlaySizeClass): Dp {
    val metrics = wordSwapDockMetrics(sizeClass)
    return metrics.bankHeight + metrics.toolHeight + 8.dp
}

@StringRes
internal fun wordSwapDirectionLabelRes(direction: WordSwapDirection): Int = when (direction) {
    WordSwapDirection.ACROSS -> R.string.wordswap_across
    WordSwapDirection.DOWN -> R.string.wordswap_down
    WordSwapDirection.DIAGONAL -> R.string.wordswap_diagonal
    WordSwapDirection.ANTI_DIAGONAL -> R.string.wordswap_anti_diagonal
}

@Composable
internal fun WordSwapDock(
    sizeClass: PlaySizeClass,
    bank: List<WordSwapBankWord>,
    canUndo: Boolean,
    canRestart: Boolean,
    onUndo: () -> Unit,
    onRestart: () -> Unit,
) {
    val metrics = wordSwapDockMetrics(sizeClass)
    var showAllWords by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().height(metrics.bankHeight).clipToBounds()) {
            WordSwapBank(bank, metrics.wordSize, Modifier.fillMaxWidth().padding(top = 4.dp))
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(LIST_FADE_HEIGHT)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, MaterialTheme.colorScheme.background))),
            )
        }
        Row(
            Modifier.fillMaxWidth().height(metrics.toolHeight),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            WordSwapTool(Icons.AutoMirrored.Outlined.ViewList, R.string.wordswap_all_words, metrics, true) { showAllWords = true }
            WordSwapTool(Icons.AutoMirrored.Outlined.Undo, R.string.wordswap_undo, metrics, canUndo, onUndo)
            WordSwapTool(Icons.Outlined.RestartAlt, R.string.wordswap_restart, metrics, canRestart, onRestart)
        }
    }
    if (showAllWords) {
        WordSwapAllWordsSheet(bank, onDismiss = { showAllWords = false })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordSwapBank(bank: List<WordSwapBankWord>, wordSize: TextUnit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        bank.groupBy { it.direction }.forEach { (direction, words) ->
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    stringResource(wordSwapDirectionLabelRes(direction)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
                words.forEach { WordSwapBankLabel(it, wordSize) }
            }
        }
    }
}

@Composable
private fun WordSwapBankLabel(word: WordSwapBankWord, size: TextUnit) {
    Text(
        word.word.uppercase(),
        fontSize = size,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        color = if (word.done) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
        textDecoration = if (word.done) TextDecoration.LineThrough else null,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WordSwapAllWordsSheet(bank: List<WordSwapBankWord>, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.wordswap_words_title), style = MaterialTheme.typography.titleSmall)
            WordSwapBank(bank, 17.sp, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun RowScope.WordSwapTool(
    icon: ImageVector,
    @StringRes labelRes: Int,
    metrics: WordSwapDockMetrics,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.weight(1f, fill = false).widthIn(max = WORDSWAP_TOOL_MAX_WIDTH).fillMaxWidth().fillMaxHeight(),
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
        contentColor = contentColor,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(metrics.iconSize))
            Text(
                stringResource(labelRes),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = metrics.labelSize, fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
